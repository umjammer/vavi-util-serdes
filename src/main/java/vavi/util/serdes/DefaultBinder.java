/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

import vavi.beans.BeanUtil;
import vavi.util.serdes.DefaultBeanBinder.DefaultEachContext;


/**
 * DefaultBinder.
 * <ul>
 * <li> {@link Element#value()}
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
 * </ul>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/02/22 umjammer initial version <br>
 * @see DefaultBeanBinder.DefaultContext
 * @see DefaultBeanBinder.DefaultContext#engine
 * @see DefaultBeanBinder.DefaultContext#sizeof(Object)
 * @see DefaultBeanBinder.DefaultContext#len(Object)
 * @see DefaultBeanBinder.DefaultContext#sizeMap
 */
public class DefaultBinder implements Binder {

    // Boolean
    protected EachBinder booleanEachBinder = new Binder.BooleanEachBinder() {
        @Override public void bind(EachContext context, Object destBean, Field field) throws IOException {
            throw new UnsupportedOperationException("boolean");
        }
    };

    // Integer
    protected EachBinder integerEachBinder = new Binder.IntegerEachBinder() {
        @Override public void bind(EachContext context, Object destBean, Field field) throws IOException {
            DefaultEachContext eachContext = DefaultEachContext.class.cast(context);
            String type = Element.Util.getValue(field);
            if (type.equalsIgnoreCase("unsigned byte")) {
                context.setValue(eachContext.dis.readUnsignedByte());
                eachContext.size = 1;
            } else if (type.equalsIgnoreCase("unsigned short")) {
                context.setValue(eachContext.dis.readUnsignedShort());
                eachContext.size = 2;
            } else {
                context.setValue(eachContext.dis.readInt());
                eachContext.size = 4;
            }
        }
    };

    // Short
    protected EachBinder shortEachBinder = new Binder.ShortEachBinder() {
        @Override public void bind(EachContext context, Object destBean, Field field) throws IOException {
            DefaultEachContext eachContext = DefaultEachContext.class.cast(context);
            context.setValue(eachContext.dis.readShort());
            eachContext.size = 2;
        }
    };

    // Byte
    protected EachBinder byteEachBinder = new Binder.ByteEachBinder() {
        @Override public void bind(EachContext context, Object destBean, Field field) throws IOException {
            DefaultEachContext eachContext = DefaultEachContext.class.cast(context);
            context.setValue(eachContext.dis.readByte());
            eachContext.size = 1;
        }
    };

    // Long
    protected EachBinder longEachBinder = new Binder.LongEachBinder() {
        @Override public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String type = Element.Util.getValue(field);
            DefaultEachContext eachContext = DefaultEachContext.class.cast(context);
            if (type.equalsIgnoreCase("unsigned int")) {
                context.setValue(eachContext.dis.readInt() & 0xffffffffl);
                eachContext.size = 4;
            } else {
                context.setValue(eachContext.dis.readLong());
                eachContext.size = 8;
            }
        }
    };

    // Float
    protected EachBinder floatEachBinder = new Binder.FloatEachBinder() {
        @Override public void bind(EachContext context, Object destBean, Field field) throws IOException {
            throw new UnsupportedOperationException("float");
        }
    };

    // Double
    protected EachBinder doubleEachBinder = new Binder.DoubleEachBinder() {
        @Override public void bind(EachContext context, Object destBean, Field field) throws IOException {
            throw new UnsupportedOperationException("double");
        }
    };

    // Character
    protected EachBinder characterEachBinder = new Binder.CharacterEachBinder() {
        @Override public void bind(EachContext context, Object destBean, Field field) throws IOException {
            throw new UnsupportedOperationException("char");
        }
    };

    // Array
    protected EachBinder arrayEachBinder = new Binder.ArrayEachBinder() {
        @Override public void bind(EachContext context, Object destBean, Field field) throws IOException {
            // Array
            DefaultEachContext eachContext = DefaultEachContext.class.cast(context);
            Object fieldValue = BeanUtil.getFieldValue(field, destBean);
            if (fieldValue != null) {
                eachContext.size = Array.getLength(fieldValue);
            }
            String sizeScript = Element.Util.getValue(field);
//Debug.println(sizeScript);
            if (!sizeScript.isEmpty()) {
                eachContext.size = Double.valueOf(eachContext.eval(sizeScript).toString()).intValue();
            }

            Class<?> fieldElementClass = field.getType().getComponentType();
            if (fieldElementClass.equals(Byte.TYPE)) {
                // byte array
                if (fieldValue != null) {
                    eachContext.dis.readFully(byte[].class.cast(fieldValue), 0, eachContext.size);
                    context.setValue(fieldValue);
                } else {
                    byte[] buf = new byte[eachContext.size];
                    eachContext.dis.readFully(buf, 0, eachContext.size);
                    context.setValue(buf);
                }
            } else if (fieldElementClass.equals(Integer.TYPE)) {
                // int array
                if (fieldValue != null) {
                    for (int i = 0; i < eachContext.size; i++) {
                        int[].class.cast(fieldValue)[i] = eachContext.dis.readInt();
                    }
                    context.setValue(fieldValue);
                } else {
                    int[] buf = new int[eachContext.size];
                    for (int i = 0; i < eachContext.size; i++) {
                        buf[i] = eachContext.dis.readInt();
                    }
                    context.setValue(buf);
                }
            } else {
                // object array
                Serdes annotation = fieldElementClass.getAnnotation(Serdes.class);
                if (annotation == null) {
                    throw new UnsupportedOperationException("use @Bound: " + fieldElementClass.getTypeName() + "] at " + field.getName() + " (" + context.getSequence() + ")");
                }
                try {
                    if (fieldValue != null) {
                        fieldValue = Array.newInstance(fieldElementClass, eachContext.size);
                    }
                    for (int i = 0; i < eachContext.size; i++) {
                        Object fieldBean = fieldElementClass.newInstance();
                        eachContext.deserialize(fieldBean);
                        Array.set(fieldValue, i, fieldBean);
                    }
                    eachContext.value = fieldValue;
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    };

    // String
    protected EachBinder stringEachBinder = new Binder.StringEachBinder() {
        @Override public void bind(EachContext context, Object destBean, Field field) throws IOException {
            DefaultEachContext eachContext = DefaultEachContext.class.cast(context);
            String sizeScript = Element.Util.getValue(field);
//Debug.println(sizeScript);
            if (!sizeScript.isEmpty()) {
                eachContext.size = Double.valueOf(eachContext.eval(sizeScript).toString()).intValue();
            } else {
                throw new IllegalArgumentException("a String field need value for length.");
            }
            byte[] bytes = new byte[eachContext.size];
            eachContext.dis.readFully(bytes);
            context.setValue(new String(bytes));
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
