/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Optional;

import vavi.beans.BeanUtil;


/**
 * Binder. (for a field of a bean)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/02/22 umjammer initial version <br>
 */
public interface Binder {

    interface EachBinder {
        boolean matches(Class<?> fieldClass);
        /** for deserializing */
        void bind(EachContext context, Object destBean, Field field) throws IOException;
        /** for serializing */
        default void bind(Object srcBean, Field field, EachContext context) throws IOException {
            // TODO remove and impl in sub class
        };
    }

    // Boolean
    abstract class BooleanEachBinder implements EachBinder {
        @Override public boolean matches(Class<?> fieldClass) {
            return fieldClass.equals(Boolean.class) || fieldClass.equals(Boolean.TYPE);
        }
    }

    // Integer
    abstract class IntegerEachBinder implements EachBinder {
        @Override public boolean matches(Class<?> fieldClass) {
            return fieldClass.equals(Integer.class) || fieldClass.equals(Integer.TYPE);
        }
    }

    // Short
    abstract class ShortEachBinder implements EachBinder {
        @Override public boolean matches(Class<?> fieldClass) {
            return fieldClass.equals(Short.class) || fieldClass.equals(Short.TYPE);
        }
    }

    // Byte
    abstract class ByteEachBinder implements EachBinder {
        @Override public boolean matches(Class<?> fieldClass) {
            return fieldClass.equals(Byte.class) || fieldClass.equals(Byte.TYPE);
        }
    }

    // Long
    abstract class LongEachBinder implements EachBinder {
        @Override public boolean matches(Class<?> fieldClass) {
            return fieldClass.equals(Long.class) || fieldClass.equals(Long.TYPE);
        }
    }

    // Float
    abstract class FloatEachBinder implements EachBinder {
        @Override public boolean matches(Class<?> fieldClass) {
            return fieldClass.equals(Float.class) || fieldClass.equals(Float.TYPE);
        }
    }

    // Double
    abstract class DoubleEachBinder implements EachBinder {
        @Override public boolean matches(Class<?> fieldClass) {
            return fieldClass.equals(Double.class) || fieldClass.equals(Double.TYPE);
        }
    }

    // Character
    abstract class CharacterEachBinder implements EachBinder {
        @Override public boolean matches(Class<?> fieldClass) {
            return fieldClass.equals(Character.class) || fieldClass.equals(Character.TYPE);
        }
    }

    // Array
    abstract class ArrayEachBinder implements EachBinder {
        @Override public boolean matches(Class<?> fieldClass) {
            return fieldClass.isArray();
        }
    }

    // List
    abstract class ListEachBinder implements EachBinder {
        @Override public boolean matches(Class<?> fieldClass) {
            return fieldClass.isAssignableFrom(java.util.List.class);
        }
    }

    // String
    abstract class StringEachBinder implements EachBinder {
        @Override public boolean matches(Class<?> fieldClass) {
            return fieldClass.equals(String.class);
        }
    }

    // Enum
    abstract class EnumEachBinder implements EachBinder {
        @Override public boolean matches(Class<?> fieldClass) {
            return fieldClass.isEnum();
        }
    }

    /** nested user defined class object annotated {@link Serdes} */
    EachBinder defaultEachBinder = new EachBinder() {
        @Override public boolean matches(Class<?> fieldClass) {
            return fieldClass.getAnnotation(Serdes.class) != null;
        }
        @Override public void bind(EachContext context, Object destBean, Field field) throws IOException {
            try {
                Object fieldValue = BeanUtil.getFieldValue(field, destBean);
                if (fieldValue == null) {
                    fieldValue = field.getType().getDeclaredConstructor().newInstance();
                }
                context.deserialize(fieldValue);
                context.setValue(fieldValue);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new IllegalStateException(e);
            }
        }
        @Override public void bind(Object srcBean, Field field, EachContext context) throws IOException {
            try {
                Object fieldValue = BeanUtil.getFieldValue(field, srcBean);
                if (fieldValue == null) {
                    fieldValue = field.getType().getDeclaredConstructor().newInstance();
                }
                context.serialize(fieldValue);
                context.setValue(fieldValue);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    /**
     * @throws IllegalArgumentException when eval failed
     * @throws UnsupportedOperationException float, double, char
     */
    default void bind(EachContext context, Object destBean, Field field) throws IOException {
        Class<?> fieldClass = field.getType();
        Optional<EachBinder> eb = Arrays.stream(getEachBinders()).filter(b -> b.matches(fieldClass)).findFirst();
        if (eb.isPresent()) {
            eb.get().bind(context, destBean, field);
        } else {
            if (defaultEachBinder.matches(fieldClass)) {
                defaultEachBinder.bind(context, destBean, field);
            } else {
                throw new UnsupportedOperationException("use @Bound: " + fieldClass.getTypeName() + "] at " + field.getName() + " (" + context.getSequence() + ")");
            }
        }

        BeanUtil.setFieldValue(field, destBean, context.getValue());
    }

    /**
     * @throws IllegalArgumentException when eval failed
     * @throws UnsupportedOperationException float, double, char
     */
    default void bind(Object srcBean, Field field, EachContext context) throws IOException {
        context.setValue(BeanUtil.getFieldValue(field, srcBean));

        Class<?> fieldClass = field.getType();
        Optional<EachBinder> eb = Arrays.stream(getEachBinders()).filter(b -> b.matches(fieldClass)).findFirst();
        if (eb.isPresent()) {
            eb.get().bind(srcBean, field, context);
        } else {
            if (defaultEachBinder.matches(fieldClass)) {
                defaultEachBinder.bind(srcBean, field, context);
            } else {
                throw new UnsupportedOperationException("use @Bound: " + fieldClass.getTypeName() + "] at " + field.getName() + " (" + context.getSequence() + ")");
            }
        }
    }

    /** */
    interface EachContext {

        int getSequence();

        Object getValue();

        void setValue(Object value);

        /**
         * @throws IllegalArgumentException when validation failed
         */
        void validate(String validation);

        boolean condition(String condition);

        /**
         * recursion
         */
        void deserialize(Object fieldValue) throws IOException;

        void serialize(Object fieldValue) throws IOException;

        /**
         * finalization (sets values to {@link vavi.util.serdes.BeanBinder.Context} etc.)
         */
        void settleValues();
    }

    /** */
    EachBinder[] getEachBinders();
}
