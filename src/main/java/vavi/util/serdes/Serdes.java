/*
 * Copyright (c) 2003 by Naohide Sano, All Rights Reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheResolverFactory;
import javax.cache.annotation.CacheResult;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.configuration.OptionalFeature;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.management.CacheStatisticsMXBean;
import javax.cache.spi.CachingProvider;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import org.aopalliance.intercept.MethodInvocation;
import org.jsr107.ri.annotations.CacheContextSource;
import org.jsr107.ri.annotations.DefaultCacheKeyGenerator;
import org.jsr107.ri.annotations.DefaultCacheResolverFactory;
import org.jsr107.ri.annotations.InternalCacheInvocationContext;
import org.jsr107.ri.annotations.InternalCacheKeyInvocationContext;
import org.jsr107.ri.annotations.StaticCacheInvocationContext;
import org.jsr107.ri.annotations.guice.CacheLookupUtil;
import org.jsr107.ri.annotations.guice.CacheResultInterceptor;


/**
 * Serdes represents a fields of POJO annotated with {@link Element} are automatically injected
 * values by the {@link Serdes.Util} utility class. not only "injection" but this system has
 * "conditioning injection", "field value validation" also.
 * <p>
 * {@link Serdes.Util#deserialize(Object, Object)} method searches
 * super classes annotated with {@link Serdes}
 * </p>
 * system property:
 * <li>{@code vavi.util.serdes.cache.statistics} ... enable cache statistics or not, default {@code false}</li>
 * <h4>*** WARNING ***</h4>
 * this property doesn't work @BeforeAll at unit test, set in pom.xml surefire pkugin section.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 031216 nsano initial version <br>
 */
@java.lang.annotation.Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Serdes {

    /** default is system encoding */
    String encoding() default "";

    /** default is true */
    boolean bigEndian() default true;

    /** default is {@link DefaultBeanBinder} */
    Class<? extends BeanBinder<? extends BeanBinder.IOSource>> beanBinder() default DefaultBeanBinder.class;

    /**
     * @see "https://aistudio.google.com/prompts/1Ws7pbMREtUaYDRCz1Jv1t2CVxdUxEdD8"
     */
    final class Cacher {

        private static final Logger logger = System.getLogger(Cacher.class.getName());

        // Static fields are set after initialization, so access to them must be delayed

        /** */
        private static final Injector injector;

        /** */
        private static final CacheManager cacheManager;

        /** */
        public static Injector injector() {
            return injector;
        }

        // avoiding key conflicts for reporting statistics
        private static final String[] CACHE_NAMES = {
                "serdes_elementFields",
                "serdes_annotation",
                "serdes_beanBinder",
                "serdes_bigEndian",
                "serdes_encoding",
                "element_sequence",
                "element_value",
                "element_bigEndian",
                "element_validation",
                "element_condition",
                "element_encoding",
                "beanBinder_validateSequences",
        };

        // Since it is managed manually, @Inject is not required, but it must conform to the interface.
        public static class MyCacheContextSource implements CacheContextSource<MethodInvocation> {

            private final CacheLookupUtil cacheLookupUtil;

            public MyCacheContextSource(CacheLookupUtil cacheLookupUtil) {
                this.cacheLookupUtil = cacheLookupUtil;
            }

            @Override
            public InternalCacheKeyInvocationContext<? extends Annotation> getCacheKeyInvocationContext(MethodInvocation invocation) {
                return cacheLookupUtil.getCacheKeyInvocationContext(invocation);
            }

            @Override
            public InternalCacheInvocationContext<? extends Annotation> getCacheInvocationContext(MethodInvocation invocation) {
                return cacheLookupUtil.getCacheInvocationContext(invocation);
            }

            @Override
            public StaticCacheInvocationContext<? extends Annotation> getMethodDetails(Method method, Class<?> targetClass) {
                return cacheLookupUtil.getMethodDetails(method, targetClass);
            }
        }

        static {
            try {
                CachingProvider cachingProvider = Caching.getCachingProvider();
logger.log(Level.TRACE, "STORE_BY_REFERENCE: " + cachingProvider.isSupported(OptionalFeature.STORE_BY_REFERENCE));
                cacheManager = cachingProvider.getCacheManager(null, Serdes.class.getClassLoader());

                boolean statisticsEnabled = Boolean.parseBoolean(System.getProperty("vavi.util.serdes.cache.statistics", "false"));
                // cache settings: fields etc. cannot be saved unless storeByValue=false (pass by reference)
                CompleteConfiguration<Object, Object> config = new MutableConfiguration<>()
                        .setTypes(Object.class, Object.class)
                        .setStoreByValue(false)
                        .setStatisticsEnabled(statisticsEnabled)
                        .setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(new Duration(TimeUnit.HOURS, 1)));

                // avoiding key conflicts for reporting statistics
                for (String name : CACHE_NAMES) {
                    cacheManager.createCache(name, config);
                }

                injector = Guice.createInjector(
                        new AbstractModule() {
                            @Override
                            protected void configure() {
                                // 1. Basic component
                                DefaultCacheKeyGenerator keyGenerator = new DefaultCacheKeyGenerator();
                                DefaultCacheResolverFactory resolverFactory = new DefaultCacheResolverFactory(cacheManager);

                                // 2. Injector Proxy (avoid circular references)
                                // CacheLookupUtil requires an Injector, but the injector isn't complete at this point.
                                // Therefore, when calling the method, pass a proxy that will reference Cacher.injector.
                                Injector lazyInjectorProxy = (Injector) Proxy.newProxyInstance(
                                        Injector.class.getClassLoader(),
                                        new Class<?>[]{Injector.class},
                                        (proxy, method, args) -> method.invoke(Cacher.injector, args)
                                );

                                // 3. Manual assembly
                                CacheLookupUtil cacheLookupUtil = new CacheLookupUtil(lazyInjectorProxy, keyGenerator, resolverFactory);
                                MyCacheContextSource myContextSource = new MyCacheContextSource(cacheLookupUtil);
                                CacheResultInterceptor interceptor = new CacheResultInterceptor();

                                // 4. Forced injection by reflection
                                // RequestInjection fails, so force the field to have a value.
                                try {
                                    Field field = CacheResultInterceptor.class.getDeclaredField("cacheContextSource");
                                    field.setAccessible(true);
                                    field.set(interceptor, myContextSource);
                                } catch (Exception e) {
                                    throw new IllegalStateException("Failed to inject cacheContextSource manually", e);
                                }

                                // 5. Guice registration
                                bind(CacheManager.class).toInstance(cacheManager);
                                bind(CacheResolverFactory.class).toInstance(resolverFactory);
                                bind(CacheKeyGenerator.class).toInstance(keyGenerator);
                                bind(CacheLookupUtil.class).toInstance(cacheLookupUtil);

                                // Bind both the raw type and the generic type (just in case)
                                bind(CacheContextSource.class).toInstance(myContextSource);
                                bind(new TypeLiteral<CacheContextSource<MethodInvocation>>(){}).toInstance(myContextSource);

                                // 6. Interceptor registration
                                // The fields have already been injected, so requestInjection is not necessary,
                                // but we call it in case there are other dependencies.
                                requestInjection(interceptor);
                                bindInterceptor(Matchers.any(),
                                        Matchers.annotatedWith(CacheResult.class),
                                        interceptor);
                            }
                        });
            } catch (Throwable t) {
                logger.log(Level.ERROR, t.getMessage(), t);
                throw new ExceptionInInitializerError(t);
            }
        }

        public static void printCacheStatistics() {
            Arrays.stream(CACHE_NAMES).forEach(Cacher::printCacheStatistics);
        }

        private static void printCacheStatistics(String cacheName) {
            try {
                MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

                ObjectName objectName = new ObjectName("javax.cache:type=CacheStatistics,CacheManager="
                        + sanitize(cacheManager.getURI().toString())
                        + ",Cache=" + cacheName);

                if (!mBeanServer.isRegistered(objectName)) {
                    throw new IllegalArgumentException("No statistics found for cache: " + cacheName);
                }

                CacheStatisticsMXBean stats = JMX.newMBeanProxy(mBeanServer, objectName, CacheStatisticsMXBean.class);

                System.out.println("=== Cache Stats: " + cacheName + " ===");
                System.out.println("Cache Hits       : " + stats.getCacheHits());
                System.out.println("Cache Misses     : " + stats.getCacheMisses());
                System.out.println("Cache Gets       : " + stats.getCacheGets());
                System.out.println("Cache Puts       : " + stats.getCachePuts());
                System.out.println("Cache Evictions  : " + stats.getCacheEvictions());
                System.out.println("Hit Percentage   : " + stats.getCacheHitPercentage() + "%");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        private static String sanitize(String uri) {
            return uri.replace(":", ".");
        }
    }

    /**
     * utility for serialize/deserialize.
     */
    class Util {

        private static final Element.Util element = Cacher.injector().getInstance(Element.Util.class);

        /** search super classes recursively */
        @CacheResult(cacheName = "serdes_elementFields")
        List<Field> getElementFields(Class<?> clazz) {
            List<Field> elementFields = new ArrayList<>();

            while (clazz != null) {
                for (Field field : clazz.getDeclaredFields()) {
                    Element elementAnnotation = field.getAnnotation(Element.class);
                    if (elementAnnotation == null) {
                        continue;
                    }

                    elementFields.add(field);
                }
                clazz = clazz.getSuperclass();
            }

            elementFields.sort((o1, o2) -> {
                int s1 = element.getSequence(o1);
                int s2 = element.getSequence(o2);
                return s1 - s2;
            });

            return elementFields;
        }

        /**
         * search super classes recursively
         * @throws IllegalArgumentException bean is not annotated with {@link Serdes}
         */
        @CacheResult(cacheName = "serdes_annotation")
        Serdes getAnnotation(Class<?> clazz) {
            while (clazz != null) {
                Serdes serdesAnnotation = clazz.getAnnotation(Serdes.class);
                if (serdesAnnotation != null) {
                    return serdesAnnotation;
                }
                clazz = clazz.getSuperclass();
            }
            throw new IllegalArgumentException("bean is not annotated with " + Serdes.class.getName());
        }

        /**
         * @throws IllegalArgumentException bean is not annotated with {@link Serdes}
         */
        @CacheResult(cacheName = "serdes_bigEndian")
        boolean isBigEndian(Class<?> clazz) {
            Serdes serdesAnnotation = getAnnotation(clazz);
            return serdesAnnotation.bigEndian();
        }

        /**
         * @throws IllegalArgumentException bean is not annotated with {@link Serdes}
         */
        @CacheResult(cacheName = "serdes_encoding")
        String encoding(Class<?> clazz) {
            Serdes serdesAnnotation = getAnnotation(clazz);
            return serdesAnnotation.encoding();
        }

        /**
         *
         * @throws NullPointerException when field is not annotated by {@link Serdes}
         */
        @CacheResult(cacheName = "serdes_beanBinder")
        BeanBinder<? extends BeanBinder.IOSource> getBeanBinder(Class<?> clazz) {
            Serdes serdesAnnotation = getAnnotation(clazz);
            try {
                return serdesAnnotation.beanBinder().getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        /**
         * Deserializes data into a POJO dstBean from in.
         *
         * @throws IllegalArgumentException thrown by validation failure
         * @throws IllegalStateException might be thrown by wrong annotation settings
         */
        @SuppressWarnings("unchecked")
        public static <T> T deserialize(Object in, T dstBean) throws IOException {
            Util util = Cacher.injector.getInstance(Util.class);
            BeanBinder<? extends BeanBinder.IOSource> binders = util.getBeanBinder(dstBean.getClass());
            return (T) binders.deserialize(in, dstBean);
        }

        /**
         * Serializes data from a POJO srcBean to out.
         */
        @SuppressWarnings("unchecked")
        public static <T> T serialize(Object srcBean, T out) throws IOException {
            Util util = Cacher.injector.getInstance(Util.class);
            BeanBinder<? extends BeanBinder.IOSource> binders = util.getBeanBinder(srcBean.getClass());
            return (T) binders.serialize(srcBean, out);
        }
    }
}
