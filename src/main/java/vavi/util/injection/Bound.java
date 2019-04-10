/*
 * Copyright (c) 2003 by Naohide Sano, All Rights Reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;


/**
 * Bound.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 031216 vavi initial version <br>
 */
@java.lang.annotation.Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Bound {

    /** */
    Class<? extends Binder<?>> binder();

    /**
     * TODO アノテーションがメソッド指定の場合
     */
    class Util {

        private Util() {
        }

        /** */
        public static boolean isBound(Field field) {
            return field.getAnnotation(Bound.class) != null;
        }

        /**
         *
         * @param field @{@link Bound} annotated field.
         * @throws NullPointerException when field is not annotated by {@link Bound}
         */
        public static <T> Binder<T> getBinder(Field field) {
            try {
                Bound bound = field.getAnnotation(Bound.class);
                @SuppressWarnings("unchecked")
                Binder<T> binder = (Binder<T>) bound.binder().getDeclaredConstructor().newInstance();
                return binder;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}

/* */
