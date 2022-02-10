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
 * <p>
 * {@link Injector.Util} class's `inject` method search super classes' methods annotated with {@link Element}
 * </p>
 * TODO validation message
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 031216 vavi initial version <br>
 */
@java.lang.annotation.Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Element {

    /**
     * 1 origin
     * TODO sub classes has same number
     */
    int sequence();

    /**
     * <pre>
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
     * </pre>
     */
    String value() default "";

    /**
     * default is follow parent @Injector
     * <p>
     * we need the 3rd value that means default (follow global setting),
     * but boolean takes only 2value (true/false). so we make this type as Boolean,
     * and null is used as the 3rd value.
     */
    String bigEndian() default "";

    /**
     * method name, signature is "method_name(I)B"
     */
    String condition() default "";

    /**
     * beanshell script that used in equals method
     * <pre>
     * for normal fields
     *
     *  $2.equals(eval("validation script"));
     *
     * for array fields
     *
     *  Arrays.equals($3, eval("validation script"));
     *
     * </pre>
     * TODO scripting more freely? (user writes equals, then eval is like assertTrue)
     * @see #value()
     */
    String validation() default "";

    /**
     * TODO annotation for method
     */
    final class Util {

        private Util() {
        }

        /** see {@link Element#sequence()} */
        public static int getSequence(Field field) {
            Element element = field.getAnnotation(Element.class);
            int sequence = element.sequence();
            if (sequence < 1) {
                throw new IllegalArgumentException("sequence should be > 0: " + sequence);
            }
            return sequence;
        }

        /** see {@link Element#value()} */
        public static String getValue(Field field) {
            Element element = field.getAnnotation(Element.class);
            return element.value();
        }

        /** see {@link Element#bigEndian()} */
        public static Boolean isBigEndian(Field field) {
            Element element = field.getAnnotation(Element.class);
            return element.bigEndian().isEmpty() ? null : Boolean.valueOf(element.bigEndian());
        }

        /** see {@link Element#validation()} */
        public static String getValidation(Field field) {
            Element element = field.getAnnotation(Element.class);
            return element.validation();
        }

        /** see {@link Element#condition()} */
        public static String getCondition(Field field) {
            Element element = field.getAnnotation(Element.class);
            return element.condition();
        }
    }
}

/* */
