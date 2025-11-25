/*
 * Copyright (c) 2003 by Naohide Sano, All Rights Reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheResult;


/**
 * Element.
 * <p>
 * {@link Serdes.Util#deserialize(Object, Object)} method
 * searches super classes' methods annotated with {@link Element}
 * </p>
 * TODO validation message
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 031216 vavi initial version <br>
 */
@java.lang.annotation.Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Element {

    /**
     * depends {@link BeanBinder}.
     * when the binder is {@link DefaultBeanBinder}, origin is 1.
     * @see DefaultBeanBinder
     */
    int sequence() default 0;

    /**
     * depends {@link BeanBinder}.
     * @see DefaultBinder
     */
    String value() default "";

    /**
     * default is follow parent @Serdes
     * <p>
     * we need the 3rd value that means default (follow global setting),
     * but boolean takes only 2value (true/false). so we make this type as Boolean,
     * and null is used as the 3rd value.
     */
    String bigEndian() default "";

    /**
     * depends {@link BeanBinder}.
     * @see DefaultBeanBinder.DefaultEachContext#condition(String)
     */
    String condition() default "";

    /**
     * depends {@link BeanBinder}.
     * @see DefaultBeanBinder.DefaultEachContext#validate(String)
     * @see #value()
     */
    String validation() default "";

    /** encoding */
    String encoding() default "";

    /**
     * TODO annotation for method
     */
    class Util {

        /** see {@link Element#sequence()} */
        @CacheResult(cacheName = "element_sequence")
        int getSequence(Field field) {
            Element element = field.getAnnotation(Element.class);
            int sequence = element.sequence();
            return sequence;
        }

        /** see {@link Element#value()} */
        @CacheResult(cacheName = "element_value")
        String getValue(Field field) {
            Element element = field.getAnnotation(Element.class);
            return element.value();
        }

        /** see {@link Element#bigEndian()} */
        @CacheResult(cacheName = "element_bigEndian") // TODO null cannot cache
        Boolean isBigEndian(Field field) {
            Element element = field.getAnnotation(Element.class);
            return element.bigEndian().isEmpty() ? null : Boolean.valueOf(element.bigEndian());
        }

        /** see {@link Element#validation()} */
        @CacheResult(cacheName = "element_validation")
        String getValidation(Field field) {
            Element element = field.getAnnotation(Element.class);
            return element.validation();
        }

        /** see {@link Element#condition()} */
        @CacheResult(cacheName = "element_condition")
        String getCondition(Field field) {
            Element element = field.getAnnotation(Element.class);
            return element.condition();
        }

        /** see {@link Element#encoding()} */
        @CacheResult(cacheName = "element_encoding")
        String getEncoding(Field field) {
            Element element = field.getAnnotation(Element.class);
            return element.encoding();
        }
    }
}
