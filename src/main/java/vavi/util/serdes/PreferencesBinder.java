/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.prefs.Preferences;

import vavi.beans.BeanUtil;


/**
 * PreferencesBinder.
 * <ul>
 * <li> {@link Element#value()} ... name (when not specified, use fieald name)
 * <li> fieldValue ... default value
 * </ul>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/02/26 umjammer initial version <br>
 */
public class PreferencesBinder implements Binder {

    Preferences prefs;

    PreferencesBinder(Preferences prefs) {
        this.prefs = prefs;
    }

    // Boolean
    protected EachBinder booleanEachBinder = new Binder.BooleanEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String name = Element.Util.getValue(field);
            if (name.isEmpty()) {
                name = field.getName();
            }
            boolean fieldValue = (boolean) BeanUtil.getFieldValue(field, destBean);
            context.setValue(prefs.getBoolean(name, fieldValue));
        }
    };

    // Integer
    protected EachBinder integerEachBinder = new Binder.IntegerEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String name = Element.Util.getValue(field);
            if (name.isEmpty()) {
                name = field.getName();
            }
            int fieldValue = (int) BeanUtil.getFieldValue(field, destBean);
            context.setValue(prefs.getInt(name, fieldValue));
        }
    };

    // Short
    protected EachBinder shortEachBinder = new Binder.ShortEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String name = Element.Util.getValue(field);
            if (name.isEmpty()) {
                name = field.getName();
            }
            short fieldValue = (short) BeanUtil.getFieldValue(field, destBean);
            context.setValue(prefs.getInt(name, fieldValue & 0xffff));
        }
    };

    // Byte
    protected EachBinder byteEachBinder = new Binder.ByteEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String name = Element.Util.getValue(field);
            if (name.isEmpty()) {
                name = field.getName();
            }
            byte fieldValue = (byte) BeanUtil.getFieldValue(field, destBean);
            context.setValue(prefs.getInt(name, fieldValue & 0xff));
        }
    };

    // Long
    protected EachBinder longEachBinder = new Binder.LongEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String name = Element.Util.getValue(field);
            if (name.isEmpty()) {
                name = field.getName();
            }
            long fieldValue = (long) BeanUtil.getFieldValue(field, destBean);
            context.setValue(prefs.getLong(name, fieldValue));
        }
    };

    // Float
    protected EachBinder floatEachBinder = new Binder.FloatEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String name = Element.Util.getValue(field);
            if (name.isEmpty()) {
                name = field.getName();
            }
            float fieldValue = (float) BeanUtil.getFieldValue(field, destBean);
            context.setValue(prefs.getFloat(name, fieldValue));
        }
    };

    // Double
    protected EachBinder doubleEachBinder = new Binder.DoubleEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String name = Element.Util.getValue(field);
            if (name.isEmpty()) {
                name = field.getName();
            }
            double fieldValue = (double) BeanUtil.getFieldValue(field, destBean);
            context.setValue(prefs.getDouble(name, fieldValue));
        }
    };

    // Character
    protected EachBinder characterEachBinder = new Binder.CharacterEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String name = Element.Util.getValue(field);
            if (name.isEmpty()) {
                name = field.getName();
            }
            char fieldValue = (char) BeanUtil.getFieldValue(field, destBean);
            context.setValue(prefs.getInt(name, fieldValue));
        }
    };

    // Array
    protected EachBinder arrayEachBinder = new Binder.ArrayEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            Class<?> fieldElementClass = field.getType().getComponentType();
            if (fieldElementClass.equals(Byte.TYPE)) {
                // byte array
                String name = Element.Util.getValue(field);
                if (name.isEmpty()) {
                    name = field.getName();
                }
                byte[] fieldValue = (byte[]) BeanUtil.getFieldValue(field, destBean);
                context.setValue(prefs.getByteArray(name, fieldValue));
            } else {
                throw new IllegalArgumentException("unsupported array: " + fieldElementClass.getClass().getName());
            }
        }
    };

    // String
    protected EachBinder stringEachBinder = new Binder.StringEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String name = Element.Util.getValue(field);
            if (name.isEmpty()) {
                name = field.getName();
            }
            String fieldValue = (String) BeanUtil.getFieldValue(field, destBean);
//Debug.println("string: " + name + ", " + prefs.get(name, fieldValue) + ", " + fieldValue + ", " + StringUtil.paramString(field));
            context.setValue(prefs.get(name, fieldValue));
        }
    };

    /** */
    private EachBinder[] eachBinders = {
        booleanEachBinder,
        integerEachBinder,
        shortEachBinder,
        byteEachBinder,
        longEachBinder,
        floatEachBinder,
        doubleEachBinder,
        characterEachBinder,
        arrayEachBinder,
        stringEachBinder,
    };


    @Override
    public EachBinder[] getEachBinders() {
        return eachBinders;
    }
}

/* */
