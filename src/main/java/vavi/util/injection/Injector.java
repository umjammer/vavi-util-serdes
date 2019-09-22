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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import vavi.beans.BeanUtil;
import vavi.io.LittleEndianDataInputStream;
import vavi.util.StringUtil;


/**
 * Injector.
 *
 * TODO Factory Injector とか？ カラムを指定する
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

        /** */
        private static Map<Object, Integer> sizeMap = new HashMap<>();

        /** a function for script */
        public static int sizeof(Object arg) {
            return sizeMap.get(arg);
        }

        /** */
        private static List<Field> getElementFields(Object destBean) {
            // 1. list up fields
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

        /** */
        public static boolean isBigEndian(Object destBean) {
            Injector injectorAnnotation = destBean.getClass().getAnnotation(Injector.class);
            if (injectorAnnotation == null) {
                throw new IllegalArgumentException("bean is not annotated with " + Injector.class.getName());
            }

            return injectorAnnotation.bigEndian();
        }

        /**
         * ストリームから POJO destBean に値をを設定します。
         */
        public static void inject(InputStream is, Object destBean) throws IOException {

            Injector injectorAnnotation = destBean.getClass().getAnnotation(Injector.class);
            if (injectorAnnotation == null) {
                throw new IllegalArgumentException("bean is not annotated with " + Injector.class.getName());
            }

            List<Field> elementFields = getElementFields(destBean);

            // 2. injection
try {
            DataInput dis;
            if (Injector.Util.isBigEndian(destBean)) {
                dis = new DataInputStream(is);
            } else {
                dis = new LittleEndianDataInputStream(is);
            }

            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("beanshell");
            Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
            bindings.put("$0", is.available()); // "$0" means whole data length
            String prepare = "import static vavi.util.injection.Injector.Util.*;";
            engine.eval(prepare);

            for (Field field : elementFields) {
                int sequence = Element.Util.getSequence(field);

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
                        value = dis.readInt();
                        size = 4;
                    } else if (fieldClass.equals(Short.class) || fieldClass.equals(Short.TYPE)) {
                        value = dis.readShort();
                        size = 2;
                    } else if (fieldClass.equals(Byte.class) || fieldClass.equals(Byte.TYPE)) {
                        value = dis.readByte();
                        size = 1;
                    } else if (fieldClass.equals(Long.class) || fieldClass.equals(Long.TYPE)) {
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
                            if (fieldValue != null) {
                                dis.readFully(byte[].class.cast(fieldValue), 0, size);
                                value = fieldValue;
                            } else {
                                byte[] buf = new byte[size];
                                dis.readFully(buf, 0, size);
                                value = buf;
                            }
                        } else {
                            throw new UnsupportedOperationException(fieldClass.getTypeName() + "]");
                        }
                    } else {
                        throw new UnsupportedOperationException("use @Bound");
                    }
                }
System.err.println(field.getName() + ": " + field.getType() + (size != 0 ? "{" + size + "}" : "" ) + " = " + (byte[].class.isInstance(value) ? "\n" + StringUtil.getDump(byte[].class.cast(value)): value));
                // field values are stored as "$1", "$2" ...
                bindings.put("$" + sequence, value);
                // field sizes
                sizeMap.put(value, size);

                // 2.2. validation
                String validation = Element.Util.getValidation(field);
                if (!validation.isEmpty()) {
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
