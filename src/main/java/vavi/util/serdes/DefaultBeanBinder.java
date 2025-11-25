/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
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

import vavi.beans.BeanUtil;
import vavi.io.LittleEndianDataInput;
import vavi.io.LittleEndianDataInputStream;
import vavi.io.LittleEndianDataOutput;
import vavi.io.LittleEndianDataOutputStream;
import vavi.io.LittleEndianSeekableDataInputStream;
import vavi.io.SeekableDataInputStream;
import vavi.util.StringUtil;
import vavi.util.serdes.Binder.EachContext;
import vavi.util.serdes.DefaultBeanBinder.DefaultIOSource;


/**
 * BeanBinders for binary.
 * <p>
 * {@link Element#sequence()} ... 1 origin (duplication is not allowed and causes exception)
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/02/26 umjammer initial version <br>
 */
public class DefaultBeanBinder extends BaseBeanBinder<DefaultIOSource> {

    private static final Logger logger = System.getLogger(DefaultBeanBinder.class.getName());

    public interface DefaultIOSource extends IOSource {}

    /**
     * @param args 0: accepts {@link InputStream}, {@link SeekableByteChannel}, {@link OutputStream}
     *             1: boolean true: big endian
     */
    @Override
    public DefaultIOSource getIOSource(Object... args) throws IOException {
        if (args[0] instanceof InputStream is) {
            DefaultInputSource in = new DefaultInputSource();
            in.bedis = new DataInputStream(is);
            in.ledis = new LittleEndianDataInputStream(is);
            in.available = is.available();
            in.defaultDis = (boolean) args[1] ? in.bedis : in.ledis;
            return in;
        } else if (args[0] instanceof SeekableByteChannel sbc) {
            DefaultInputSource in = new DefaultInputSource();
            in.bedis = new SeekableDataInputStream(sbc);
            in.ledis = new LittleEndianSeekableDataInputStream(sbc);
            in.available = (int) (sbc.size() - sbc.position());
            in.defaultDis = (boolean) args[1] ? in.bedis : in.ledis;
            return in;
        } else if (args[0] instanceof OutputStream os) {
            DefaultOutputSource out = new DefaultOutputSource();
            out.bedos = new DataOutputStream(os);
            out.ledos = new LittleEndianDataOutputStream(os);
            out.defaultDos = (boolean) args[1] ? out.bedos : out.ledos;
            return out;
        }

        throw new IllegalArgumentException("unsupported class args[0]: " + args[0].getClass().getName());
    }

    /** for deserializing */
    protected static class DefaultInputSource implements DefaultIOSource {
        DataInput bedis;
        LittleEndianDataInput ledis;
        /** {@link Element#bigEndian()} considerable DataInput */
        DataInput defaultDis;
        /** {@link Element#bigEndian()} considerable DataInput */
        DataInput get(boolean isBigendian) {
            return isBigendian ? bedis : ledis;
        }
        int available;
    }

    /** for serializing */
    protected static class DefaultOutputSource implements DefaultIOSource {
        DataOutput bedos;
        LittleEndianDataOutput ledos;
        /** {@link Element#bigEndian()} considerable DataOutput */
        DataOutput defaultDos;
        /** {@link Element#bigEndian()} considerable DataOutput */
        DataOutput get(boolean isBigendian) {
            return isBigendian ? bedos : ledos;
        }
    }

    /**
     * context for a bean.
     * <p>
     * not thread safe, must be public for script engine
     * <p>
     * scripting predefined variables
     * <pre>
     *  * {@code $_} value of the bean
     *  * {@code $#} value of the field. # is like 1, 2, 3 ..., 1 origin, means the {@link Element#sequence()}
     *  * {@code $0} is whole data length TODO $0 is not object length but stream length
     * </pre>
     */
    public static class DefaultContext implements BeanBinder.Context {
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("groovy");
        final Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);

        final DefaultIOSource io;
        final List<Field> fields;
        final Object bean;
        final DefaultBeanBinder beanBinder;

        /** for deserializing */
        DefaultContext(DefaultInputSource in, List<Field> fields, Object bean, Object parent, DefaultBeanBinder beanBinder) {
            this.io = in;
            this.fields = fields;
            this.bean = bean;
            this.beanBinder = beanBinder;

            validator.validateSequences(bean.getClass());

logger.log(Level.TRACE, "engine: " + engine.getFactory().getEngineName());
logger.log(Level.TRACE, "parent: " + parent + ", bean: " + bean);
            bindings.put("$__", parent);
            bindings.put("$_", bean);
            bindings.put("$0", ((DefaultInputSource) this.io).available); // "$0" means whole data length TODO available is not object length but stream length
        }

        /** for serializing */
        DefaultContext(DefaultOutputSource out, List<Field> fields, Object bean, Object parent, DefaultBeanBinder beanBinder) {
            this.io = out;
            this.fields = fields;
            this.bean = bean;
            this.beanBinder = beanBinder;

            validator.validateSequences(bean.getClass());

            bindings.put("$__", parent);
            bindings.put("$_", bean);
        }

        Object eval(String script) {
            try {
                String prepare = "import static " + getClass().getName() + ".*;";
logger.log(Level.TRACE, "prepare: " + prepare);
                return engine.eval(prepare + script);
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

    /** context for each field */
    public static class DefaultEachContext implements EachContext {
        public final int sequence;
        public final DefaultBeanBinder.DefaultContext context;
        public final Field field;

        protected Object value;
        public int size;

        @Override
        public Object getValue() {
            return value;
        }
        @Override
        public void setValue(Object value) {
            this.value = value;
        }

        /** {@link Element#bigEndian()} considerable DataInput */
        public DataInput dis;
        /** {@link Element#bigEndian()} considerable DataOutput */
        public DataOutput dos;

        public DefaultEachContext(int sequence, Boolean isBigEndian, Field field, Context context) {
            this.sequence = sequence;
            this.field = field;
            this.context = (DefaultContext) context;

            if (this.context.io instanceof DefaultBeanBinder.DefaultInputSource) {
                if (isBigEndian != null) {
                    dis = ((DefaultInputSource) this.context.io).get(isBigEndian);
                } else {
                    dis = ((DefaultInputSource) this.context.io).defaultDis;
                }
            } else if (this.context.io instanceof DefaultBeanBinder.DefaultOutputSource) {
                if (isBigEndian != null) {
                    dos = ((DefaultOutputSource) this.context.io).get(isBigEndian);
                } else {
                    dos = ((DefaultOutputSource) this.context.io).defaultDos;
                }
            } else {
                throw new IllegalStateException(this.context.io.getClass().getName());
            }
        }

        @Override
        public void deserialize(Object dstBean, Object parent) throws IOException {
            context.beanBinder.deserialize0((DefaultInputSource) context.io, dstBean, parent);
        }

        @Override
        public void serialize(Object srcBean, Object parent) throws IOException {
            context.beanBinder.serialize0(srcBean, (DefaultOutputSource) context.io, parent);
        }

        /** @throws IllegalArgumentException eval failed */
        public Object eval(String script) {
            return context.eval(script);
        }

        @Override
        public int getSequence() {
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
        @Override
        public void validate(String validation) {
            // TODO why bean shell accepts >= 0x80 byte value w/o (byte) cast?
            String validationScript;
            if (field.getType().isArray()) {
                validationScript = "java.util.Arrays.equals($" + sequence + ", " + validation + ")";
            } else {
                validationScript = "$" + sequence + ".equals(" + validation + ")";
            }
            if (!Boolean.parseBoolean(eval(validationScript).toString())) {
                throw new IllegalArgumentException("validation for sequence " + sequence + " failed.\n" + validationScript);
            }
        }

        /**
         * @param condition method name, signature is "method_name(I)B", argument is {@link Element#sequence()}
         */
        @Override
        public boolean condition(String condition) {
            try {
                Method method = BeanUtil.getMethodByNameOf(context.bean.getClass(), condition, Integer.TYPE);
                return (boolean) method.invoke(context.bean, sequence);
            } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new IllegalArgumentException(e);
            }
        }

        /**
         * set bsh value named "$#" as the java field {@link #value}
         * and update {@link DefaultContext#sizeMap}
         */
        @Override
        public void settleValues() {
            // field values are stored as "$1", "$2" ...
            this.context.bindings.put("$" + sequence, value);
            // field sizes
            DefaultContext.sizeMap.put(value, size);
        }

        @Override
        public String toString() {
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
    protected Context getContext(IOSource io, List<Field> fields, Object bean, Object parent) {
        if (io instanceof DefaultBeanBinder.DefaultInputSource iio) {
            return new DefaultBeanBinder.DefaultContext(iio, fields, bean, parent, this);
        } else if (io instanceof DefaultBeanBinder.DefaultOutputSource oio) {
            return new DefaultContext(oio, fields, bean, parent, this);
        } else {
            throw new IllegalStateException(io.getClass().getName());
        }
    }

    @Override
    protected EachContext getEachContext(int sequence, Boolean isBigEndian, Field field, Context context) {
        return new DefaultBeanBinder.DefaultEachContext(sequence, isBigEndian, field, context);
    }
}
