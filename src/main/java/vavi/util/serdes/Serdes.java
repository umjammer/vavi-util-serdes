/*
 * Copyright (c) 2003 by Naohide Sano, All Rights Reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import javax.cache.annotation.CacheResult;


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
     * utility for serialize/deserialize.
     */
    class Util {

        private static final Element.Util element = CachingDIContainer.injector().getInstance(Element.Util.class);

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
            Serdes.Util serdes = CachingDIContainer.injector().getInstance(Serdes.Util.class);
            BeanBinder<? extends BeanBinder.IOSource> binders = serdes.getBeanBinder(dstBean.getClass());
            return (T) binders.deserialize(in, dstBean);
        }

        /**
         * Serializes data from a POJO srcBean to out.
         */
        @SuppressWarnings("unchecked")
        public static <T> T serialize(Object srcBean, T out) throws IOException {
            Serdes.Util serdes = CachingDIContainer.injector().getInstance(Serdes.Util.class);
            BeanBinder<? extends BeanBinder.IOSource> binders = serdes.getBeanBinder(srcBean.getClass());
            return (T) binders.serialize(srcBean, out);
        }
    }
}
