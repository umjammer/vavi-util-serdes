/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.SeekableByteChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import vavi.io.LittleEndianDataInput;
import vavi.io.LittleEndianDataInputStream;
import vavi.io.LittleEndianSeekableDataInputStream;
import vavi.io.SeekableDataInputStream;
import vavi.util.StringUtil;
import vavi.util.serdes.Binder.EachContext;
import vavi.util.serdes.DefaultBeanBinder.DefaultIOSource;


/**
 * BeanBinders for binary.
 * <p>
 * {@link Element#sequence()} ... 1 origin (duplication is not allowed)
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/02/26 umjammer initial version <br>
 */
public class DefaultBeanBinder extends BaseBeanBinder<DefaultIOSource> {

    /**
     * @param args 0: accepts {@link InputStream}, {@link SeekableByteChannel}
     *             1: boolean true: big endian
     */
    @Override
    public DefaultIOSource getIOSource(Object... args) throws IOException {
        DefaultIOSource in = new DefaultIOSource();
        if (args[0] instanceof InputStream) {
            InputStream is = (InputStream) args[0];
            in.bedis = new DataInputStream(is);
            in.ledis = new LittleEndianDataInputStream(is);
            in.available = is.available();
        } else if (args[0] instanceof SeekableByteChannel) {
            SeekableByteChannel sbc = (SeekableByteChannel) args[0];
            in.bedis = new SeekableDataInputStream(sbc);
            in.ledis = new LittleEndianSeekableDataInputStream(sbc);
            in.available = (int) (sbc.size() - sbc.position());
        } else {
            throw new IllegalArgumentException("unsupported class args[0]: " + args[0].getClass().getName());
        }
        in.defaultDis = (boolean) args[1] ? in.bedis : in.ledis;
        return in;
    }

    /** */
    protected static class DefaultIOSource implements BeanBinder.IOSource {
        DataInput bedis;
        LittleEndianDataInput ledis;
        DataInput defaultDis;
        DataInput get(boolean isBigedian) {
            return isBigedian ? bedis : ledis;
        }
        int available;
    }

    /** not thread safe, should be public for script engine */
    public static class DefaultContext implements BeanBinder.Context {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("beanshell");
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);

        DefaultIOSource in;
        List<Field> fields;
        Object bean;
        DefaultBeanBinder beanBinder;

        DefaultContext(IOSource in, List<Field> fields, Object bean, DefaultBeanBinder beanBinder) {
            this.in = (DefaultIOSource) in;
            this.fields = fields;
            this.bean = bean;
            this.beanBinder = beanBinder;

            validateSequences(fields);

            try {
                bindings.put("$0", this.in.available); // "$0" means whole data length
                String prepare = "import static " + getClass().getName() + ".*;";
                engine.eval(prepare);
            } catch (ScriptException e) {
                throw new IllegalStateException(e);
            }
        }

        /** for script */
        private static final Map<Object, Integer> sizeMap = new HashMap<>();

        /** a function for script */
        public static int sizeof(Object arg) {
            return sizeMap.get(arg);
        }

        /** a function for script */
        public static int len(Object arg) {
            if (arg.getClass().isArray()) {
                return Array.getLength(arg);
            } else {
                throw new IllegalArgumentException("arg is not an array");
            }
        }
    }

    /** */
    protected static class DefaultEachContext implements Binder.EachContext {
        public int sequence;
        DefaultContext context;
        Field field;

        public Object value;
        public int size;

        @Override
        public Object getValue() {
            return value;
        }
        @Override
        public void setValue(Object value) {
            this.value = value;
        }

        DataInput dis;

        public DefaultEachContext(int sequence, Boolean isBigEndian, Field field, Context context) {
            this.sequence = sequence;
            this.field = field;
            this.context = (DefaultContext) context;

            if (isBigEndian != null) {
                dis = this.context.in.get(isBigEndian);
            } else {
                dis = this.context.in.defaultDis;
            }
        }
        @Override public void deserialize(Object destBean) throws IOException {
            context.beanBinder.deserialize0(context.in, destBean);
        }
        /** @throws IllegalArgumentException eval failed */
        public Object eval(String script) {
            try {
                return context.engine.eval(script);
            } catch (ScriptException e) {
                throw new IllegalArgumentException(script, e);
            }
        }
        @Override public int getSequence() {
            return sequence;
        }
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
         */
        @Override public void validate(String validation) {
            // TODO why bean shell accepts >= 0x80 byte value w/o (byte) cast?
            String validationScript;
            if (field.getType().isArray()) {
                validationScript = "java.util.Arrays.equals($" + sequence + ", " + validation + ")";
            } else {
                validationScript = "$" + sequence + ".equals(" + validation + ")";
            }
            try {
                if (!Boolean.parseBoolean(context.engine.eval(validationScript).toString())) {
                    throw new IllegalArgumentException("validation for sequence " + sequence + " failed.\n" + validationScript);
                }
            } catch (ScriptException e) {
                throw new IllegalArgumentException(validation, e);
            }
        }
        /**
         * @param condition method name, signature is "method_name(I)B", argument is {@link Element#sequence()}
         */
        @Override public boolean condition(String condition) {
            try {
                Method method = context.bean.getClass().getDeclaredMethod(condition, Integer.TYPE);
                return (boolean) method.invoke(context.bean, sequence);
            } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new IllegalArgumentException(e);
            }
        }
        @Override public void settleValues() {
            // field values are stored as "$1", "$2" ...
            this.context.bindings.put("$" + sequence, value);
            // field sizes
            DefaultContext.sizeMap.put(value, size);
        }
        @Override public String toString() {
            return (size != 0 ? "{" + size + "}" : "" ) + " = " +
                    (value instanceof byte[] ? "\n" + StringUtil.getDump((byte[]) value, 64): value);
        }
    }

    /** {@link Binder} is state-less */
    private static final DefaultBinder defaultBinder = new DefaultBinder();

    @Override
    protected Binder getDefaultBinder() {
        return defaultBinder;
    }

    @Override
    protected Context getContext(IOSource in, List<Field> fields, Object bean) {
        return new DefaultContext(in, fields, bean, this);
    }

    @Override
    protected EachContext getEachContext(int sequence, Boolean isBigEndian, Field field, Context context) {
        return new DefaultEachContext(sequence, isBigEndian, field, context);
    }
}

/* */
