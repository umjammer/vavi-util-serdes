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
 * Element.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 031216 vavi initial version <br>
 */
@java.lang.annotation.Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Element {

    /** 1 origin */
    int sequence();

    /**
     * engine
     *
     *  * beanshell
     *
     * prebound
     *
     *  * $# # is 1, 2, 3 ...
     *  * $0 is whole data length
     *
     * function
     *
     *  * len(arg) returns length of the array arg
     *  * sizeof(arg) returns size of the arg
     *
     * value
     *
     *  * java primitives
     *
     *  ** default size of java primitives
     *  ** "unsigned int" unsigned 32 bit
     *
     * * array
     *
     *  ** value length of the array
     */
    String value() default "";

    /**
     * 
     */
    String validation() default "";

    /**
     * TODO アノテーションがメソッド指定の場合
     */
    class Util {

        private Util() {
        }

        /** */
        public static int getSequence(Field field) {
            Element element = field.getAnnotation(Element.class);
            int sequence = element.sequence();
            if (sequence < 1) {
                throw new IllegalArgumentException("sequence should be > 0: " + sequence);
            }
            return sequence;
        }

        /** */
        public static String getValue(Field field) {
            Element element = field.getAnnotation(Element.class);
            return element.value();
        }

        /** */
        public static String getValidation(Field field) {
            Element element = field.getAnnotation(Element.class);
            return element.validation();
        }
    }
}

/* */
