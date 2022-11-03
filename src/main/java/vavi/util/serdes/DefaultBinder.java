/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.logging.Level;

import vavi.beans.BeanUtil;
import vavi.beans.ClassUtil;
import vavi.util.Debug;
import vavi.util.StringUtil;
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
            DefaultEachContext eachContext = (DefaultEachContext) context;
            context.setValue(eachContext.dis.readBoolean());
            eachContext.size = 1;
        }
    };

    // Byte
    protected EachBinder byteEachBinder = new Binder.ByteEachBinder() {
        @Override public void bind(EachContext context, Object destBean, Field field) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            context.setValue(eachContext.dis.readByte());
            eachContext.size = 1;
        }
    };

    // Short
    protected EachBinder shortEachBinder = new Binder.ShortEachBinder() {
        @Override public void bind(EachContext context, Object destBean, Field field) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            context.setValue(eachContext.dis.readShort());
            eachContext.size = 2;
        }
    };

    // Integer, value=type ("unsigned byte"|"unsigned short"|empty)
    protected EachBinder integerEachBinder = new Binder.IntegerEachBinder() {
        @Override public void bind(EachContext context, Object destBean, Field field) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
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

    // Long, value=type ("unsigned int"|empty)
    protected EachBinder longEachBinder = new Binder.LongEachBinder() {
        @Override public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String type = Element.Util.getValue(field);
            DefaultEachContext eachContext = (DefaultEachContext) context;
            if (type.equalsIgnoreCase("unsigned int")) {
                context.setValue(eachContext.dis.readInt() & 0xffff_ffffL);
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
            DefaultEachContext eachContext = (DefaultEachContext) context;
            context.setValue(eachContext.dis.readFloat());
            eachContext.size = 4;
        }
    };

    // Double
    protected EachBinder doubleEachBinder = new Binder.DoubleEachBinder() {
        @Override public void bind(EachContext context, Object destBean, Field field) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            context.setValue(eachContext.dis.readDouble());
            eachContext.size = 8;
        }
    };

    // Character
    protected EachBinder characterEachBinder = new Binder.CharacterEachBinder() {
        @Override public void bind(EachContext context, Object destBean, Field field) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            context.setValue(eachContext.dis.readChar());
            eachContext.size = 2;
        }
    };

    // Array, value=script for size
    protected EachBinder arrayEachBinder = new Binder.ArrayEachBinder() {
        @Override public void bind(EachContext context, Object destBean, Field field) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            Object fieldValue = BeanUtil.getFieldValue(field, destBean);
            if (fieldValue != null) {
                eachContext.size = Array.getLength(fieldValue);
            }
            String sizeScript = Element.Util.getValue(field);
Debug.println(Level.FINER, sizeScript);
            if (!sizeScript.isEmpty()) {
                eachContext.size = Double.valueOf(eachContext.eval(sizeScript).toString()).intValue();
            }

            Class<?> fieldElementClass = field.getType().getComponentType();
            if (fieldElementClass.equals(Byte.TYPE)) {
                // byte array
                if (fieldValue != null) {
                    eachContext.dis.readFully((byte[]) fieldValue, 0, eachContext.size);
                    context.setValue(fieldValue);
                } else {
                    byte[] buf = new byte[eachContext.size];
                    eachContext.dis.readFully(buf, 0, eachContext.size);
                    context.setValue(buf);
                }
            } else if (fieldElementClass.equals(Short.TYPE)) {
                // short array
                if (fieldValue != null) {
                    for (int i = 0; i < eachContext.size; i++) {
                        ((short[]) fieldValue)[i] = eachContext.dis.readShort();
                    }
                    context.setValue(fieldValue);
                } else {
                    short[] buf = new short[eachContext.size];
                    for (int i = 0; i < eachContext.size; i++) {
                        buf[i] = eachContext.dis.readShort();
                    }
                    context.setValue(buf);
                }
            } else if (fieldElementClass.equals(Integer.TYPE)) {
                // int array
                if (fieldValue != null) {
                    for (int i = 0; i < eachContext.size; i++) {
                        ((int[]) fieldValue)[i] = eachContext.dis.readInt();
                    }
                    context.setValue(fieldValue);
                } else {
                    int[] buf = new int[eachContext.size];
                    for (int i = 0; i < eachContext.size; i++) {
                        buf[i] = eachContext.dis.readInt();
                    }
                    context.setValue(buf);
                }
            } else if (fieldElementClass.equals(Long.TYPE)) {
                // long array
                if (fieldValue != null) {
                    for (int i = 0; i < eachContext.size; i++) {
                        ((long[]) fieldValue)[i] = eachContext.dis.readLong();
                    }
                    context.setValue(fieldValue);
                } else {
                    long[] buf = new long[eachContext.size];
                    for (int i = 0; i < eachContext.size; i++) {
                        buf[i] = eachContext.dis.readLong();
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

    // String, value=script for size
    protected EachBinder stringEachBinder = new Binder.StringEachBinder() {
        @Override public void bind(EachContext context, Object destBean, Field field) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            String sizeScript = Element.Util.getValue(field);
Debug.println(Level.FINER, sizeScript);
            if (!sizeScript.isEmpty()) {
                eachContext.size = Double.valueOf(eachContext.eval(sizeScript).toString()).intValue();
            } else {
                throw new IllegalArgumentException("a String field need value for length.");
            }
            byte[] bytes = new byte[eachContext.size];
            eachContext.dis.readFully(bytes);
            String encoding = Element.Util.getEncoding(field);
            if (encoding.isEmpty()) {
                encoding = Serdes.Util.encoding(destBean);
            }
            if (!encoding.isEmpty()) {
Debug.println(Level.FINE, encoding);
                context.setValue(new String(bytes, Charset.forName(encoding)));
            } else {
Debug.println(Level.FINE, "no encoding: " +bytes.length + " bytes\n" + StringUtil.getDump(bytes));
                context.setValue(new String(bytes));
            }
        }
    };

    /** @throws IllegalArgumentException when type is not proper value */
    private Object read(DefaultEachContext eachContext, String type) throws IOException {
        if (type.equalsIgnoreCase("byte")) {
            eachContext.size = 1;
            return eachContext.dis.readByte();
        } else if (type.equalsIgnoreCase("unsigned byte")) {
            eachContext.size = 1;
            return eachContext.dis.readUnsignedByte();
        } else if (type.equalsIgnoreCase("short")) {
            eachContext.size = 2;
            return eachContext.dis.readShort();
        } else if (type.equalsIgnoreCase("unsigned short")) {
            eachContext.size = 2;
            return eachContext.dis.readUnsignedShort();
        } else if (type.equalsIgnoreCase("int")) {
            eachContext.size = 4;
            return eachContext.dis.readInt();
        } else if (type.equalsIgnoreCase("unsigned int")) {
            eachContext.size = 4;
            return eachContext.dis.readInt() & 0xffff_ffffL;
        } else if (type.equalsIgnoreCase("long")) {
            eachContext.size = 8;
            return eachContext.dis.readLong();
        } else {
            throw new IllegalArgumentException(type);
        }
    }

    /**
     * Gets one of enum member.
     * @return Arrays.stream(enumType.values()).filter(p).findFirst().get();
     * @throws java.util.NoSuchElementException when not found
     */
    private Enum<?> getEnum(Class<?> enumType, Predicate<Enum<?>> p) {
        try {
            // TODO "values()" is implementation specific?
            Method method = enumType.getDeclaredMethod("values");
Debug.println(Level.FINER, "getEnum: " + Arrays.toString((Enum<?>[]) method.invoke(null)));
            return Arrays.stream((Enum<?>[]) method.invoke(null)).filter(p).findFirst().get();
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Gets enum value by method "methodName".
     * @return enumType.enumObject.methodName() e.g. EnumX.MEMBER_1.getValue()
     * @throws IllegalStateException cannot get
     */
    private Object invokeValueMethod(Class<?> enumType, Object enumObject, String methodName) {
Debug.println(Level.FINER, "invokeValueMethod: " + enumType + ", " + enumObject + ", " + methodName);
        try {
            Method method = enumType.getDeclaredMethod(methodName);
            return method.invoke(enumObject);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    // Enum, value=type ("int"|"unsigned byte"|...)
    protected EachBinder enumEachBinder = new Binder.EnumEachBinder() {
        @Override
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            String type = Element.Util.getValue(field);
            // TODO fixed default value type "unsigned short"
            Object value = read(eachContext, !type.isEmpty() ? type : "unsigned short");
            Class<?> enumValueType = enumValueType(field.getType());
            if (enumValueType == Void.TYPE) {
                eachContext.setValue(getEnum(field.getType(), e -> e.ordinal() == (Integer) value));
            } else if (enumValueType == Integer.TYPE) {
                // TODO fixed getter method name "getValue"
                eachContext.setValue(getEnum(field.getType(), e -> invokeValueMethod(field.getType(), e, "getValue").equals(value)));
            } else if (enumValueType == Long.TYPE) {
                // TODO fixed getter method name "getValue"
                eachContext.setValue(getEnum(field.getType(), e -> invokeValueMethod(field.getType(), e, "getValue").equals(value)));
            } else if (enumValueType == String.class) {
                // TODO not implemented yet
                throw new UnsupportedOperationException("use @Bound: " + enumValueType + "] at " + field.getName() + " (" + context.getSequence() + ")");
            } else {
                throw new UnsupportedOperationException("use @Bound: " + enumValueType + "] at " + field.getName() + " (" + context.getSequence() + ")");
            }
        }
    };

    /**
     * Gets a type of user defined enum value.
     * TODO depends enum implementation?
     * @return null no suitable type found
     */
    Class<?> enumValueType(Class<?> enumClass) {
        // enum constructor has
        // parameters with (String:memberName, int:ordinal, XXX...:user specified types...)
        // means from 3rd parameters are user defined values.
if (Debug.isLoggable(Level.FINER)) {
 Arrays.stream(enumClass.getDeclaredConstructors()).forEach(c -> {
  System.err.println(c.getName() + "." + ClassUtil.signatureWithName(c));
 });
}
        try {
            // case simple enum, e.g.
            // enum E { A, B, C }
            Constructor<?> c = enumClass.getDeclaredConstructor(String.class, Integer.TYPE);
            return Void.TYPE; // use ordinal (means no user defined value)
        } catch (NoSuchMethodException e) {
Debug.println(Level.FINER, e);
        }
        try {
            // case enum with integer value, e.g.
            // enum E { A(1), B(2), C(4); final int v; public int getValue() {return v;} E(int v) {this.v = v;}
            Constructor<?> c = enumClass.getDeclaredConstructor(String.class, Integer.TYPE, Integer.TYPE);
            return Integer.TYPE; // integer value
        } catch (NoSuchMethodException e) {
Debug.println(Level.FINER, e);
        }
        try {
            // case enum with long value, e.g.
            // enum E { A(1L), B(2L), C(4L); final long v; public long getValue() {return v;} E(long v) {this.v = v;}
            Constructor<?> c = enumClass.getDeclaredConstructor(String.class, Integer.TYPE, Long.TYPE);
            return Long.TYPE; // integer value
        } catch (NoSuchMethodException e) {
Debug.println(Level.FINER, e);
        }
        try {
            // case enum with String value, e.g.
            // enum E { A("alpha"), B("bravo"), C(""); final String v; public String getValue() {return v;} E(String v) {this.v = v;}
            Constructor<?> c = enumClass.getDeclaredConstructor(String.class, Integer.TYPE, String.class);
            return String.class; // string value
        } catch (NoSuchMethodException e) {
Debug.println(Level.FINER, e);
        }
        return null;
    }

    /** */
    private final EachBinder[] eachBinders = {
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
        enumEachBinder,
    };

    @Override
    public EachBinder[] getEachBinders() {
        return eachBinders;
    }
}

/* */
