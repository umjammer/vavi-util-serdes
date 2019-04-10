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

    /** 0 origin */
    int sequence();

    /**
     * engine
     * 
     *  * beanshell
     *
     * prebound
     *
     *  * $# # is 0, 1, 2 ...
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
     * TODO アノテーションがメソッド指定の場合
     */
    class Util {

        private Util() {
        }

        /** */
        public static int getSequence(Field field) {
            Element element = field.getAnnotation(Element.class);
            return element.sequence();
        }

        /** */
        public static String getValue(Field field) {
            Element element = field.getAnnotation(Element.class);
            return element.value();
        }
    }
}

/* */
