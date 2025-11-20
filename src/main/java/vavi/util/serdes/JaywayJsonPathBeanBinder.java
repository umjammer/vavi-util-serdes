/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.Field;
import java.util.List;

import vavi.util.serdes.Binder.EachContext;

import static java.lang.System.getLogger;


/**
 * JaywayJsonPathBeanBinder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-12-18 nsano initial version <br>
 */
public class JaywayJsonPathBeanBinder extends DefaultBeanBinder {

    private static final Logger logger = getLogger(JaywayJsonPathBeanBinder.class.getName());

    String source;

    static class NullIOSource extends DefaultInputSource {
    }

    @Override
    public NullIOSource getIOSource(Object... args) throws IOException {
        NullIOSource io = new NullIOSource();
        if (args[0] instanceof InputStream is) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            while (true) {
                int r = is.read(b);
                if (r < 0) break;
                baos.write(b, 0, r);
            }
            source = baos.toString();
logger.log(Level.DEBUG, "source: " + source);
        } else {
            throw new IllegalArgumentException("unsupported class: " + args[0].getClass().getName());
        }
        return io;
    }

    /** disable eval (beanshell engine) method */
    protected static class MyDefaultEachContext extends DefaultEachContext {

        public MyDefaultEachContext(int sequence, Boolean isBigEndian, Field field, Context context) {
            super(sequence, isBigEndian, field, context);
        }

        @Override
        public Object eval(String script) {
            return script;
        }
    }

    @Override
    public Object serialize(Object srcBean, Object io) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Binder getDefaultBinder() {
        return new JaywayJsonPathBinder(source);
    }

    @Override
    protected EachContext getEachContext(int sequence, Boolean isBigEndian, Field field, Context context) {
        return new MyDefaultEachContext(sequence, isBigEndian, field, context);
    }

    @Override
    protected Context getContext(IOSource io, List<Field> fields, Object bean, Object parent) {
        if (io instanceof DefaultInputSource iio) {
            return new DefaultContext(iio, fields, bean, parent, this);
        } else {
            throw new IllegalStateException(io.getClass().getName());
        }
    }
}
