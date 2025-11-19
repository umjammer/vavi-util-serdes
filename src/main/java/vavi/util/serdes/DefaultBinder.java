/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import vavi.beans.BeanUtil;
import vavi.beans.ClassUtil;
import vavi.util.StringUtil;
import vavi.util.serdes.DefaultBeanBinder.DefaultEachContext;

import static java.lang.System.getLogger;


/**
 * DefaultBinder.
 * <ul>
 * <li> {@link Element#value()} value set to this annotation field will be evaluated by script engine.
 * <pre>
 * engine
 *
 *  * beanshell
 *
 * pre-bound variables
 *
 *  * {@code $_} value of the bean
 *  * {@code $#} value of the field. # is like 1, 2, 3 ..., 1 origin, means the {@link Element#sequence()}
 *  * {@code $0} is whole data length TODO $0 is not object length but stream length
 *
 * function
 *
 *  * {@code len(arg)} returns length of the array arg
 *  * {@code sizeof(arg)} returns size of the arg
 *
 * value
 *
 *  * when field is a java primitive
 *
 *  ** default size of java primitives (Integer.BYTES, Short.BYTES, ...)
 *  ** {@code unsigned int} unsigned 32 bit
 *
 * * when field is an array
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
@SuppressWarnings("JavadocReference")
public class DefaultBinder implements Binder {

    private static final Logger logger = getLogger(DefaultBinder.class.getName());

    // Boolean
    protected final EachBinder booleanEachBinder = new Binder.BooleanEachBinder() {
        @Override public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            context.setValue(eachContext.dis.readBoolean());
            eachContext.size = 1;
        }

        @Override public void bind(Object srcBean, Field field, EachContext context) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            eachContext.dos.writeBoolean((boolean) context.getValue());
        }
    };

    // Byte
    protected final EachBinder byteEachBinder = new Binder.ByteEachBinder() {
        @Override public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            context.setValue(eachContext.dis.readByte());
            eachContext.size = 1;
        }

        @Override public void bind(Object srcBean, Field field, EachContext context) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            eachContext.dos.writeByte((byte) context.getValue());
            eachContext.size = 1;
        }
    };

    // Short
    protected final EachBinder shortEachBinder = new Binder.ShortEachBinder() {
        @Override public void bind(EachContext context, Object dsstBean, Field field) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            context.setValue(eachContext.dis.readShort());
            eachContext.size = 2;
        }

        @Override public void bind(Object srcBean, Field field, EachContext context) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            eachContext.dos.writeShort((short) context.getValue());
            eachContext.size = 2;
        }
    };

    // Integer, value=type ("unsigned byte"|"unsigned short"|empty)
    protected final EachBinder integerEachBinder = new Binder.IntegerEachBinder() {
        // Integer deserializing
        @Override public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            String type = Element.Util.getValue(field);
            if (type.equalsIgnoreCase("byte")) {
                context.setValue(eachContext.dis.readByte());
                eachContext.size = 1;
            } else if (type.equalsIgnoreCase("unsigned byte")) {
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

        // Integer serializing
        @Override public void bind(Object srcBean, Field field, EachContext context) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            String type = Element.Util.getValue(field);
            if (type.equalsIgnoreCase("byte")) {
                eachContext.dos.writeByte((int) context.getValue());
                eachContext.size = 1;
            } else if (type.equalsIgnoreCase("unsigned byte")) {
                eachContext.dos.writeByte((int) context.getValue());
                eachContext.size = 1;
            } else if (type.equalsIgnoreCase("unsigned short")) {
                eachContext.dos.writeShort((int) context.getValue());
                eachContext.size = 2;
            } else {
                eachContext.dos.writeInt((int) context.getValue());
                eachContext.size = 4;
            }
        }
    };

    // Long, value=type ("unsigned int"|empty)
    protected final EachBinder longEachBinder = new Binder.LongEachBinder() {
        // Long deserializing
        @Override public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            String type = Element.Util.getValue(field);
            if (type.equalsIgnoreCase("unsigned int")) {
                context.setValue(eachContext.dis.readInt() & 0xffff_ffffL);
                eachContext.size = 4;
            } else {
                context.setValue(eachContext.dis.readLong());
                eachContext.size = 8;
            }
        }

        // Long serializing
        @Override public void bind(Object srcBean, Field field, EachContext context) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            String type = Element.Util.getValue(field);
            if (type.equalsIgnoreCase("unsigned int")) {
                eachContext.dos.writeInt((int) context.getValue());
                eachContext.size = 4;
            } else {
                eachContext.dos.writeLong((long) context.getValue());
                eachContext.size = 8;
            }
        }
    };

    // Float
    protected final EachBinder floatEachBinder = new Binder.FloatEachBinder() {
        @Override public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            context.setValue(eachContext.dis.readFloat());
            eachContext.size = 4;
        }

        @Override public void bind(Object srcBean, Field field, EachContext context) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            eachContext.dos.writeFloat((float) context.getValue());
            eachContext.size = 4;
        }
    };

    // Double
    protected final EachBinder doubleEachBinder = new Binder.DoubleEachBinder() {
        @Override public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            context.setValue(eachContext.dis.readDouble());
            eachContext.size = 8;
        }

        @Override public void bind(Object srcBean, Field field, EachContext context) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            eachContext.dos.writeDouble((double) context.getValue());
            eachContext.size = 8;
        }
    };

    // Character
    protected final EachBinder characterEachBinder = new Binder.CharacterEachBinder() {
        @Override public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            context.setValue(eachContext.dis.readChar());
            eachContext.size = 2;
        }

        @Override public void bind(Object srcBean, Field field, EachContext context) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            eachContext.dos.writeChar((char) context.getValue());
            eachContext.size = 2;
        }
    };

    // Array, value=script for size
    protected final EachBinder arrayEachBinder = new Binder.ArrayEachBinder() {
        // array deserializing
        @Override public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            Object fieldValue = BeanUtil.getFieldValue(field, dstBean);
            if (fieldValue != null) {
                eachContext.size = Array.getLength(fieldValue);
            }
            String sizeScript = Element.Util.getValue(field);
logger.log(Level.TRACE, "sizeScript: " + sizeScript);
            if (!sizeScript.isEmpty()) {
                eachContext.size = Double.valueOf(eachContext.eval(sizeScript).toString()).intValue(); // TODO size means array size or total byte?
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
                    if (fieldValue == null) {
                        fieldValue = Array.newInstance(fieldElementClass, eachContext.size);
                    }
                    for (int i = 0; i < eachContext.size; i++) {
                        Object fieldBean = fieldElementClass.getDeclaredConstructor().newInstance();
                        eachContext.deserialize(fieldBean);
                        Array.set(fieldValue, i, fieldBean);
                    }
                    context.setValue(fieldValue);
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                         InvocationTargetException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        // array serializing
        @Override public void bind(Object srcBean, Field field, EachContext context) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            Object fieldValue = context.getValue();
            if (fieldValue != null) {
                eachContext.size = Array.getLength(fieldValue);
            }
            String sizeScript = Element.Util.getValue(field);
logger.log(Level.TRACE, "sizeScript: " + sizeScript);
            if (!sizeScript.isEmpty()) {
                eachContext.size = Double.valueOf(eachContext.eval(sizeScript).toString()).intValue();
            }
            if (eachContext.size == 0) throw new IllegalStateException("size must be set for: " + field.getName());

            Class<?> fieldElementClass = field.getType().getComponentType();
            if (fieldElementClass.equals(Byte.TYPE)) {
                // byte array
                if (fieldValue != null) {
                    for (int i = 0; i < eachContext.size; i++) {
                        eachContext.dos.writeByte(((byte[]) fieldValue)[i]);
                    }
                } else {
                    byte[] buf = new byte[eachContext.size];
                    eachContext.dos.write(buf, 0, eachContext.size);
                }
            } else if (fieldElementClass.equals(Short.TYPE)) {
                // short array
                if (fieldValue != null) {
                    for (int i = 0; i < eachContext.size; i++) {
                        eachContext.dos.writeShort(((short[]) fieldValue)[i]);
                    }
                } else {
                    for (int i = 0; i < eachContext.size; i++) {
                        eachContext.dos.writeShort(0);
                    }
                }
            } else if (fieldElementClass.equals(Integer.TYPE)) {
                // int array
                if (fieldValue != null) {
                    for (int i = 0; i < eachContext.size; i++) {
                        eachContext.dos.writeInt(((int[]) fieldValue)[i]);
                    }
                } else {
                    for (int i = 0; i < eachContext.size; i++) {
                        eachContext.dos.writeInt(0);
                    }
                }
            } else if (fieldElementClass.equals(Long.TYPE)) {
                // long array
                if (fieldValue != null) {
                    for (int i = 0; i < eachContext.size; i++) {
                        eachContext.dos.writeLong(((long[]) fieldValue)[i]);
                    }
                } else {
                    for (int i = 0; i < eachContext.size; i++) {
                        eachContext.dos.writeLong(0);
                    }
                }
            } else {
                // object array
                Serdes annotation = fieldElementClass.getAnnotation(Serdes.class);
                if (annotation == null) {
                    throw new UnsupportedOperationException("use @Bound: " + fieldElementClass.getTypeName() + "] at " + field.getName() + " (" + context.getSequence() + ")");
                }
                if (fieldValue == null) {
                    fieldValue = Array.newInstance(fieldElementClass, eachContext.size);
                }
                for (int i = 0; i < eachContext.size; i++) {
                    Object fieldBean = Array.get(fieldValue, i);
                    eachContext.serialize(fieldBean);
                }
            }
        }
    };

    // List, value=script for size
    protected final EachBinder listEachBinder = new Binder.ListEachBinder() {
        // list deserializing
        @SuppressWarnings("unchecked")
        @Override public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            Object fieldValue = BeanUtil.getFieldValue(field, dstBean);
            String sizeScript = Element.Util.getValue(field);
logger.log(Level.TRACE, "sizeScript: " + sizeScript);
            if (!sizeScript.isEmpty()) {
                eachContext.size = Double.valueOf(eachContext.eval(sizeScript).toString()).intValue();
            }

            Class<?> genericTypeClass = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            if (genericTypeClass.equals(Byte.class)) {
                // byte list
                if (fieldValue == null) {
                    fieldValue = new ArrayList<>(eachContext.size);
                }
                byte[] buf = new byte[eachContext.size];
                eachContext.dis.readFully(buf, 0, eachContext.size);
                for (byte b : buf) {
                    ((List<Byte>) fieldValue).add(b);
                }
                context.setValue(fieldValue);
            } else if (genericTypeClass.equals(Short.class)) {
                // short list
                if (fieldValue == null) {
                    fieldValue = new ArrayList<>(eachContext.size);
                }
                for (int i = 0; i < eachContext.size; i++) {
                    ((List<Short>) fieldValue).add(eachContext.dis.readShort());
                }
                context.setValue(fieldValue);
            } else if (genericTypeClass.equals(Integer.class)) {
                // int list
                if (fieldValue == null) {
                    fieldValue = new ArrayList<>(eachContext.size);
                }
                for (int i = 0; i < eachContext.size; i++) {
                    ((List<Integer>) fieldValue).add(eachContext.dis.readInt());
                }
                context.setValue(fieldValue);
            } else if (genericTypeClass.equals(Long.class)) {
                // long list
                if (fieldValue == null) {
                    fieldValue = new ArrayList<>(eachContext.size);
                }
                for (int i = 0; i < eachContext.size; i++) {
                    ((List<Long>) fieldValue).add(eachContext.dis.readLong());
                }
                context.setValue(fieldValue);
            } else {
                // object list
                Serdes annotation = genericTypeClass.getAnnotation(Serdes.class);
                if (annotation == null) {
                    throw new UnsupportedOperationException("use @Bound: " + genericTypeClass.getTypeName() + "] at " + field.getName() + " (" + context.getSequence() + ")");
                }
                try {
                    if (fieldValue == null) {
                        fieldValue = new ArrayList<>(eachContext.size);
                    }
                    for (int i = 0; i < eachContext.size; i++) {
                        Object fieldBean = genericTypeClass.getDeclaredConstructor().newInstance();
                        eachContext.deserialize(fieldBean);
                        ((List<Object>) fieldValue).add(fieldBean);
                    }
                    context.setValue(fieldValue);
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                         InvocationTargetException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        // list serializing
        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override public void bind(Object srcBean, Field field, EachContext context) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            Object fieldValue = context.getValue();
            if (fieldValue != null) {
                eachContext.size = ((List) fieldValue).size();
            }
            String sizeScript = Element.Util.getValue(field);
logger.log(Level.TRACE, "sizeScript: " + sizeScript);
            if (!sizeScript.isEmpty()) {
                eachContext.size = Double.valueOf(eachContext.eval(sizeScript).toString()).intValue();
            }
            if (eachContext.size == 0) throw new IllegalStateException("size must be set for: " + field.getName());

            Class<?> fieldElementClass = field.getType().getComponentType();
            if (fieldElementClass.equals(Byte.TYPE)) {
                // byte list
                if (fieldValue != null) {
                    for (byte b : (List<Byte>) fieldValue) {
                        eachContext.dos.writeByte(b);
                    }
                } else {
                    byte[] buf = new byte[eachContext.size];
                    eachContext.dos.write(buf, 0, eachContext.size);
                }
            } else if (fieldElementClass.equals(Short.TYPE)) {
                // short list
                if (fieldValue != null) {
                    for (short s : (List<Short>) fieldValue) {
                        eachContext.dos.writeShort(s);
                    }
                } else {
                    for (int i = 0; i < eachContext.size; i++) {
                        eachContext.dos.writeShort(0);
                    }
                }
            } else if (fieldElementClass.equals(Integer.TYPE)) {
                // int list
                if (fieldValue != null) {
                    for (int i : (List<Integer>) fieldValue) {
                        eachContext.dos.writeInt(i);
                    }
                } else {
                    for (int i = 0; i < eachContext.size; i++) {
                        eachContext.dos.writeInt(0);
                    }
                }
            } else if (fieldElementClass.equals(Long.TYPE)) {
                // long list
                if (fieldValue != null) {
                    for (long l : (List<Long>) fieldValue) {
                        eachContext.dos.writeLong(l);
                    }
                } else {
                    for (int i = 0; i < eachContext.size; i++) {
                        eachContext.dos.writeLong(0);
                    }
                }
            } else {
                // object list
                Serdes annotation = fieldElementClass.getAnnotation(Serdes.class);
                if (annotation == null) {
                    throw new UnsupportedOperationException("use @Bound: " + fieldElementClass.getTypeName() + "] at " + field.getName() + " (" + context.getSequence() + ")");
                }
                if (fieldValue == null) {
                    fieldValue = Array.newInstance(fieldElementClass, eachContext.size);
                }
                for (int i = 0; i < eachContext.size; i++) {
                    Object fieldBean = Array.get(fieldValue, i);
                    eachContext.serialize(fieldBean);
                }
            }
        }
    };

    // String, value=script for size
    protected final EachBinder stringEachBinder = new Binder.StringEachBinder() {
        // String deserializing
        @Override public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            String sizeScript = Element.Util.getValue(field);
logger.log(Level.TRACE, sizeScript);
            if (!sizeScript.isEmpty()) {
                eachContext.size = Double.valueOf(eachContext.eval(sizeScript).toString()).intValue();
            } else {
                throw new IllegalArgumentException("a String field need value for length.");
            }
            byte[] bytes = new byte[eachContext.size];
            eachContext.dis.readFully(bytes);
            String encoding = Element.Util.getEncoding(field);
            if (encoding.isEmpty()) {
                encoding = Serdes.Util.encoding(dstBean);
            }
            if (!encoding.isEmpty()) {
logger.log(Level.DEBUG, encoding);
                context.setValue(new String(bytes, Charset.forName(encoding)));
            } else {
logger.log(Level.DEBUG, () -> "no encoding: " + bytes.length + " bytes\n" + StringUtil.getDump(bytes));
                context.setValue(new String(bytes));
            }
        }

        // String serializing
        @Override public void bind(Object srcBean, Field field, EachContext context) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            eachContext.size = ((String) context.getValue()).length();
            String sizeScript = Element.Util.getValue(field);
logger.log(Level.TRACE, sizeScript);
            if (!sizeScript.isEmpty()) {
                eachContext.size = Double.valueOf(eachContext.eval(sizeScript).toString()).intValue();
            }
            if (eachContext.size == 0) throw new IllegalStateException("size must be set for: " + field.getName());
            String encoding = Element.Util.getEncoding(field);
            if (encoding.isEmpty()) {
                encoding = Serdes.Util.encoding(srcBean);
            }
            if (!encoding.isEmpty()) {
logger.log(Level.DEBUG, encoding);
                eachContext.dos.write(((String) context.getValue()).getBytes(Charset.forName(encoding)));
            } else {
logger.log(Level.DEBUG, () -> "no encoding: " + field.getName());
                eachContext.dos.write(((String) context.getValue()).getBytes());
            }
        }
    };

    /** @throws IllegalArgumentException when type is not proper value */
    private static Object read(DefaultEachContext eachContext, String type) throws IOException {
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

    /** @throws IllegalArgumentException when type is not proper value */
    private static void write(DefaultEachContext eachContext, String type, Object value) throws IOException {
logger.log(Level.TRACE, "field: " + eachContext.field.getName() + ", type: " + type + ", value: " + value);
        if (type.equalsIgnoreCase("byte")) {
            eachContext.size = 1;
            eachContext.dos.writeByte((byte) value);
        } else if (type.equalsIgnoreCase("unsigned byte")) {
            eachContext.size = 1;
            eachContext.dos.writeByte((byte) value);
        } else if (type.equalsIgnoreCase("short")) {
            eachContext.size = 2;
            eachContext.dos.writeShort((short) value);
        } else if (type.equalsIgnoreCase("unsigned short")) {
            eachContext.size = 2;
            eachContext.dos.writeShort((short) value);
        } else if (type.equalsIgnoreCase("int")) {
            eachContext.size = 4;
            eachContext.dos.writeInt((int) value);
        } else if (type.equalsIgnoreCase("unsigned int")) {
            eachContext.size = 4;
            eachContext.dos.writeInt((int) value);
        } else if (type.equalsIgnoreCase("long")) {
            eachContext.size = 8;
            eachContext.dos.writeLong((long)value);
        } else {
            throw new IllegalArgumentException(type);
        }
    }

    /**
     * Gets one of enum member.
     * @return Arrays.stream(enumType.values()).filter(p).findFirst().get();
     * @throws java.util.NoSuchElementException when not found
     */
    private static Enum<?> getEnum(Class<?> enumType, Predicate<Enum<?>> p) {
        try {
            // TODO "values()" is implementation specific?
            Method method = enumType.getDeclaredMethod("values");
logger.log(Level.TRACE, "getEnum: " + Arrays.toString((Enum<?>[]) method.invoke(null)));
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
    private static Object invokeValueMethod(Class<?> enumType, Object enumObject, String methodName) {
logger.log(Level.TRACE, "invokeValueMethod: " + enumType + ", " + enumObject + ", " + methodName);
        try {
            Method method = enumType.getDeclaredMethod(methodName);
            return method.invoke(enumObject);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    // Enum, value=type ("int"|"unsigned byte"|...)
    protected final EachBinder enumEachBinder = new Binder.EnumEachBinder() {
        // Enum deserializing
        @Override public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            String type = Element.Util.getValue(field);
            // TODO fixed default value type "unsigned short"
            Object value = read(eachContext, !type.isEmpty() ? type : "unsigned short");
logger.log(Level.TRACE, "value: " + value);
            Class<?> enumValueType = enumValueType(field.getType());
            if (enumValueType == Void.TYPE) {
                eachContext.setValue(getEnum(field.getType(), e -> e.ordinal() == (Integer) value));
                eachContext.size = 2;
            } else if (enumValueType == Integer.TYPE) {
                // TODO fixed getter method name "getValue"
                eachContext.setValue(getEnum(field.getType(), e -> invokeValueMethod(field.getType(), e, "getValue").equals(value)));
                eachContext.size = 4;
            } else if (enumValueType == Long.TYPE) {
                // TODO fixed getter method name "getValue"
                eachContext.setValue(getEnum(field.getType(), e -> invokeValueMethod(field.getType(), e, "getValue").equals(value)));
                eachContext.size = 8;
            } else if (enumValueType == String.class) {
                // TODO not implemented yet
                throw new UnsupportedOperationException("use @Bound: " + enumValueType + "] at " + field.getName() + " (" + context.getSequence() + ")");
            } else {
                throw new UnsupportedOperationException("use @Bound: " + enumValueType + "] at " + field.getName() + " (" + context.getSequence() + ")");
            }
        }

        // Enum serializing
        @Override public void bind(Object srcBean, Field field, EachContext context) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            String type = Element.Util.getValue(field);
            Class<?> enumValueType = enumValueType(field.getType());
logger.log(Level.TRACE, "type: " + type + ", enumValueType: " + enumValueType);
            if (enumValueType == Void.TYPE) {
                if (!type.isEmpty()) {
                    write(eachContext, type, ((Enum<?>) eachContext.getValue()).ordinal());
                } else {
                    // TODO fixed default value type "unsigned short"
                    eachContext.dos.writeShort(((Enum<?>) eachContext.getValue()).ordinal());
                    eachContext.size = 2;
                }
            } else if (enumValueType == Integer.TYPE) {
                if (!type.isEmpty()) {
                    write(eachContext, type, (Integer) invokeValueMethod(field.getType(), (Enum<?>) eachContext.getValue(), "getValue"));
                } else {
                    // TODO fixed getter method name "getValue"
                    eachContext.dos.writeInt((Integer) invokeValueMethod(field.getType(), (Enum<?>) eachContext.getValue(), "getValue"));
                    eachContext.size = 4;
                }
            } else if (enumValueType == Long.TYPE) {
                if (!type.isEmpty()) {
                    write(eachContext, type, (Long) invokeValueMethod(field.getType(), (Enum<?>) eachContext.getValue(), "getValue"));
                } else {
                    // TODO fixed getter method name "getValue"
                    eachContext.dos.writeLong((Long) invokeValueMethod(field.getType(), (Enum<?>) eachContext.getValue(), "getValue"));
                    eachContext.size = 8;
                }
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
    static Class<?> enumValueType(Class<?> enumClass) {
        // enum constructor has
        // parameters with (String:memberName, int:ordinal, XXX...:user specified types...)
        // means from 3rd parameters are user defined values.
if (logger.isLoggable(Level.TRACE)) {
 Arrays.stream(enumClass.getDeclaredConstructors()).forEach(c ->
  logger.log(Level.TRACE, c.getName() + "." + ClassUtil.signatureWithName(c))
 );
}
        try {
            // case simple enum, e.g.
            // enum E { A, B, C }
            Constructor<?> c = enumClass.getDeclaredConstructor(String.class, Integer.TYPE);
            return Void.TYPE; // use ordinal (means no user defined value)
        } catch (NoSuchMethodException e) {
logger.log(Level.TRACE, e);
        }
        try {
            // case enum with integer value, e.g.
            // enum E { A(1), B(2), C(4); final int v; public int getValue() {return v;} E(int v) {this.v = v;}
            Constructor<?> c = enumClass.getDeclaredConstructor(String.class, Integer.TYPE, Integer.TYPE);
            return Integer.TYPE; // integer value
        } catch (NoSuchMethodException e) {
logger.log(Level.TRACE, e);
        }
        try {
            // case enum with long value, e.g.
            // enum E { A(1L), B(2L), C(4L); final long v; public long getValue() {return v;} E(long v) {this.v = v;}
            Constructor<?> c = enumClass.getDeclaredConstructor(String.class, Integer.TYPE, Long.TYPE);
            return Long.TYPE; // integer value
        } catch (NoSuchMethodException e) {
logger.log(Level.TRACE, e);
        }
        try {
            // case enum with String value, e.g.
            // enum E { A("alpha"), B("bravo"), C(""); final String v; public String getValue() {return v;} E(String v) {this.v = v;}
            Constructor<?> c = enumClass.getDeclaredConstructor(String.class, Integer.TYPE, String.class);
            return String.class; // string value
        } catch (NoSuchMethodException e) {
logger.log(Level.TRACE, e);
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
        listEachBinder,
    };

    @Override
    public EachBinder[] getEachBinders() {
        return eachBinders;
    }
}
