/*
 * Copyright (c) 2003 by Naohide Sano, All Rights Reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.injection;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import vavi.beans.BeanUtil;
import vavi.io.LittleEndianDataInput;
import vavi.io.LittleEndianDataInputStream;
import vavi.io.LittleEndianSeekableDataInputStream;
import vavi.io.SeekableDataInputStream;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * Injector represents a fields of POJO annotated with {@link Element} are automatically injected
 * values by the {@link Injector.Util} utility class. not only "injection" but this system has
 * "conditioning injection", "field value validation" also.
 * <p>
 * {@link Injector.Util} class's `inject` method search super classes annotated with {@link Injector}
 * </p>
 * TODO how about Factory Injector? specify column
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 031216 vavi initial version <br>
 */
@java.lang.annotation.Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Injector {

    /** default is system encoding */
    String encoding() default "";

    /** default is true */
    boolean bigEndian() default true;

    /**
     * utility for injection.
     */
    class Util {

        private Util() {
        }

        /** a function for script */
        public static int len(Object arg) {
            if (arg.getClass().isArray()) {
                return Array.getLength(arg);
            } else {
                throw new IllegalArgumentException("arg is not an array");
            }
        }

        /** for script */
        private static Map<Object, Integer> sizeMap = new HashMap<>();

        /** a function for script */
        public static int sizeof(Object arg) {
            return sizeMap.get(arg);
        }

        /** search super classes recursively */
        private static List<Field> getElementFields(Object destBean) {
            List<Field> elementFields = new ArrayList<>();

            Class<?> clazz = destBean.getClass();
            while (clazz != null) {
                for (Field field : clazz.getDeclaredFields()) {
                    Element elementAnnotation = field.getAnnotation(Element.class);
                    if (elementAnnotation == null) {
                        continue;
                    }

                    elementFields.add(field);
                }
                clazz = clazz.getSuperclass();
            }

            Collections.sort(elementFields, (o1, o2) -> {
                int s1 = Element.Util.getSequence(o1);
                int s2 = Element.Util.getSequence(o2);
                return s1 - s2;
            });

            return elementFields;
        }

        /**
         * search super classes recursively
         * @throws IllegalArgumentException bean is not annotated with {@link Injector}
         */
        private static Injector getAnnotation(Object destBean) {
            Class<?> clazz = destBean.getClass();
            while (clazz != null) {
                Injector injectorAnnotation = clazz.getAnnotation(Injector.class);
                if (injectorAnnotation != null) {
                    return injectorAnnotation;
                }
                clazz = clazz.getSuperclass();
            }
            throw new IllegalArgumentException("bean is not annotated with " + Injector.class.getName());
        }

        /** */
        public static boolean isBigEndian(Object destBean) {
            Injector injectorAnnotation = getAnnotation(destBean);

            return injectorAnnotation.bigEndian();
        }

        private static class InputSource {
            DataInput bedis;
            LittleEndianDataInput ledis;
            DataInput defaultDis;
            void setDefault(boolean isBigedian) {
                this.defaultDis = isBigedian ? bedis : ledis;
            }
            DataInput get(boolean isBigedian) {
                return isBigedian ? bedis : ledis;
            }
            int available;
        }

        /**
         * Injects data into a POJO destBean from stream.
         */
        public static void inject(InputStream is, Object destBean) throws IOException {
            InputSource in = new InputSource();
            in.bedis = new DataInputStream(is);
            in.ledis = new LittleEndianDataInputStream(is);
            in.available = is.available();
            inject(in, destBean);
        }

        /**
         * Injects data into a POJO destBean from a channel.
         *
         * @throws IllegalArgumentException thrown by validation failure
         * @throws IllegalStateException might be thrown by wrong annotation settings
         */
        public static void inject(SeekableByteChannel sbc, Object destBean) throws IOException {
            InputSource in = new InputSource();
            in.bedis = new SeekableDataInputStream(sbc);
            in.ledis = new LittleEndianSeekableDataInputStream(sbc);
            in.available = (int) (sbc.size() - sbc.position());
            inject(in, destBean);
        }

        private static void inject(InputSource in, Object destBean) throws IOException {
            getAnnotation(destBean);

            // 1. list up fields
            List<Field> elementFields = getElementFields(destBean);

            // 2. injection
try {
            in.setDefault(Injector.Util.isBigEndian(destBean));

            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("beanshell");
            Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
            bindings.put("$0", in.available); // "$0" means whole data length
            String prepare = "import static vavi.util.injection.Injector.Util.*;";
            engine.eval(prepare);

            for (Field field : elementFields) {
                int sequence = Element.Util.getSequence(field);
                DataInput dis;
                if (Element.Util.isBigEndian(field) != null) {
                    dis = in.get(Element.Util.isBigEndian(field).booleanValue());
                } else {
                    dis = in.defaultDis;
                }

                // condition
                String condition = Element.Util.getCondition(field);
                if (!condition.isEmpty()) {
                    try {
                        Method method = destBean.getClass().getDeclaredMethod(condition, Integer.TYPE);
                        boolean r = (boolean) method.invoke(destBean, sequence);
                        if (!r) {
Debug.println(Level.FINE, "condition check is false");
                            continue;
                        }
                    } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        throw new IllegalStateException(e);
                    }
                }

                // inject
                Object value = null;
                int size = 0;
                if (Bound.Util.isBound(field)) {
//                    Binder<?> binder = Bound.Util.getBinder(field);
//                    defaultBinder.bind(destBean, field, field.getType(), binder.bind(dis), null);
                    throw new UnsupportedOperationException("Binder not supported yet");
                } else {
                    Class<?> fieldClass = field.getType();
                    if (fieldClass.equals(Boolean.class) || fieldClass.equals(Boolean.TYPE)) {
                        throw new UnsupportedOperationException("boolean");
                    } else if (fieldClass.equals(Integer.class) || fieldClass.equals(Integer.TYPE)) {
                        // Integer
                        String type = Element.Util.getValue(field);
                        if (type.equalsIgnoreCase("unsigned byte")) {
                            value = dis.readUnsignedByte();
                            size = 1;
                        } else if (type.equalsIgnoreCase("unsigned short")) {
                            value = dis.readUnsignedShort();
                            size = 2;
                        } else {
                            value = dis.readInt();
                            size = 4;
                        }
                    } else if (fieldClass.equals(Short.class) || fieldClass.equals(Short.TYPE)) {
                        // Short
                        value = dis.readShort();
                        size = 2;
                    } else if (fieldClass.equals(Byte.class) || fieldClass.equals(Byte.TYPE)) {
                        // Byte
                        value = dis.readByte();
                        size = 1;
                    } else if (fieldClass.equals(Long.class) || fieldClass.equals(Long.TYPE)) {
                        // Long
                        String type = Element.Util.getValue(field);
                        if (type.equalsIgnoreCase("unsigned int")) {
                            value = dis.readInt() & 0xffffffffl;
                            size = 4;
                        } else {
                            value = dis.readLong();
                            size = 8;
                        }
                    } else if (fieldClass.equals(Float.class) || fieldClass.equals(Float.TYPE)) {
                        throw new UnsupportedOperationException("float");
                    } else if (fieldClass.equals(Double.class) || fieldClass.equals(Double.TYPE)) {
                        throw new UnsupportedOperationException("double");
                    } else if (fieldClass.equals(Character.class) || fieldClass.equals(Character.TYPE)) {
                        throw new UnsupportedOperationException("char");
                    } else if (fieldClass.isArray()) {
                        // Array
                        Object fieldValue = BeanUtil.getFieldValue(field, destBean);
                        if (fieldValue != null) {
                            size = Array.getLength(fieldValue);
                        }
                        String sizeScript = Element.Util.getValue(field);
//System.err.println(sizeScript);
                        if (!sizeScript.isEmpty()) {
                            size = Double.valueOf(engine.eval(sizeScript).toString()).intValue();
                        }

                        fieldClass = fieldClass.getComponentType();
                        if (fieldClass.equals(Byte.TYPE)) {
                            // byte array
                            if (fieldValue != null) {
                                dis.readFully(byte[].class.cast(fieldValue), 0, size);
                                value = fieldValue;
                            } else {
                                byte[] buf = new byte[size];
                                dis.readFully(buf, 0, size);
                                value = buf;
                            }
                        } else {
                            // object array
                            Injector annotation = fieldClass.getAnnotation(Injector.class);
                            if (annotation == null) {
                                throw new UnsupportedOperationException("use @Bound: " + fieldClass.getTypeName() + "]");
                            }
                            try {
                                if (fieldValue != null) {
                                    fieldValue = Array.newInstance(fieldClass, size);
                                }
                                for (int i = 0; i < size; i++) {
                                    Object fieldBean = fieldClass.newInstance();
                                    inject(in, fieldBean);
                                    Array.set(fieldValue, i, fieldBean);
                                }
                                value = fieldValue;
                            } catch (InstantiationException | IllegalAccessException e) {
                                throw new IllegalStateException(e);
                            }
                        }
                    } else if (fieldClass.equals(String.class)) {
                        // String
                        String sizeScript = Element.Util.getValue(field);
//System.err.println(sizeScript);
                        if (!sizeScript.isEmpty()) {
                            size = Double.valueOf(engine.eval(sizeScript).toString()).intValue();
                        } else {
                            throw new IllegalArgumentException("a String field need value for length.");
                        }
                        byte[] bytes = new byte[size];
                        dis.readFully(bytes);
                        value = new String(bytes);
                   } else {
                       // nested user defined class object annotated @Injector
                       Injector annotation = fieldClass.getAnnotation(Injector.class);
                       if (annotation == null) {
                           throw new UnsupportedOperationException("use @Bound: " + fieldClass.getTypeName() + "]");
                       }
                       try {
                           Object fieldValue = BeanUtil.getFieldValue(field, destBean);
                           if (fieldValue == null) {
                               fieldValue = fieldClass.newInstance();
                           }
                           inject(in, fieldValue);
                           value = fieldValue;
                       } catch (InstantiationException | IllegalAccessException e) {
                           throw new IllegalStateException(e);
                       }
                    }

                    BeanUtil.setFieldValue(field, destBean, value);
                }
Debug.println(Level.FINE, field.getName() + ": " + field.getType() + (size != 0 ? "{" + size + "}" : "" ) + " = " + (byte[].class.isInstance(value) ? "\n" + StringUtil.getDump(byte[].class.cast(value), 64): value));
                // field values are stored as "$1", "$2" ...
                bindings.put("$" + sequence, value);
                // field sizes
                sizeMap.put(value, size);

                // 2.2. validation
                String validation = Element.Util.getValidation(field);
                if (!validation.isEmpty()) {
                    // TODO why bean shell accepts >= 0x80 byte value w/o (byte) cast?
                    String validationScript;
                    if (field.getType().isArray()) {
                        validationScript = "java.util.Arrays.equals($" + sequence + ", " + validation + ")";
                    } else {
                        validationScript = "$" + sequence + ".equals(" + validation + ")";
                    }
                    if (!Boolean.valueOf(engine.eval(validationScript).toString())) {
                        throw new IllegalArgumentException("validation for sequence " + sequence + " failed.\n" + validationScript);
                    }
                }
            }
} catch (ScriptException e) {
    throw new IllegalStateException(e);
}
        }
    }
}

/* */
