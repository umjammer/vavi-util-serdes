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
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Serdes represents a fields of POJO annotated with {@link Element} are automatically injected
 * values by the {@link Serdes.Util} utility class. not only "injection" but this system has
 * "conditioning injection", "field value validation" also.
 * <p>
 * {@link Serdes.Util#deserialize(SeekableByteChannel, Object)} method searches
 * super classes annotated with {@link Serdes}
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 031216 vavi initial version <br>
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
    final class Util {

        private Util() {
        }

        /** search super classes recursively */
        static List<Field> getElementFields(Object destBean) {
            List<Field> elementFields = new ArrayList<>();

            Class<?> clazz = destBean.getClass();
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

            Collections.sort(elementFields, (o1, o2) -> {
                int s1 = Element.Util.getSequence(o1);
                int s2 = Element.Util.getSequence(o2);
                return s1 - s2;
            });

            return elementFields;
        }

        /**
         * search super classes recursively
         * @throws IllegalArgumentException bean is not annotated with {@link Serdes}
         */
        static Serdes getAnnotation(Object destBean) {
            Class<?> clazz = destBean.getClass();
            while (clazz != null) {
                Serdes serdesAnnotation = clazz.getAnnotation(Serdes.class);
                if (serdesAnnotation != null) {
                    return serdesAnnotation;
                }
                clazz = clazz.getSuperclass();
            }
            throw new IllegalArgumentException("bean is not annotated with " + Serdes.class.getName());
        }

        /** */
        static boolean isBigEndian(Object destBean) {
            Serdes serdesAnnotation = getAnnotation(destBean);
            if (serdesAnnotation == null) {
                throw new IllegalArgumentException("bean is not annotated with " + Serdes.class.getName());
            }
            return serdesAnnotation.bigEndian();
        }

        /**
         *
         * @throws NullPointerException when field is not annotated by {@link Serdes}
         */
        static BeanBinder<? extends BeanBinder.IOSource> getBeanBinder(Object destBean) {
            Serdes serdesAnnotation = getAnnotation(destBean);
            if (serdesAnnotation == null) {
                throw new IllegalArgumentException("bean is not annotated with " + Serdes.class.getName());
            }
            try {
                return (BeanBinder<? extends BeanBinder.IOSource>) serdesAnnotation.beanBinder().getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        /**
         * Deserializes data into a POJO destBean from in.
         *
         * @throws IllegalArgumentException thrown by validation failure
         * @throws IllegalStateException might be thrown by wrong annotation settings
         */
        @SuppressWarnings("unchecked")
        public static <T> T deserialize(Object in, T destBean) throws IOException {
            BeanBinder<? extends BeanBinder.IOSource> binders = getBeanBinder(destBean);
            return (T) binders.deserialize(in, destBean);
        }

        /**
         * Serializes data from a POJO srcBean to out.
         */
        @SuppressWarnings("unchecked")
        public static <T> T serialize(Object srcBean, T out) throws IOException {
            BeanBinder<? extends BeanBinder.IOSource> binders = getBeanBinder(srcBean);
            return (T) binders.serialize(srcBean, out);
        }
    }
}

/* */
