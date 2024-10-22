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

import static java.lang.System.getLogger;


/**
 * SaxonXPathBeanBinder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-11-07 nsano initial version <br>
 */
public class SaxonXPathBeanBinder extends SimpleBeanBinder<SaxonXPathBeanBinder.NullIOSource> {

    private static final Logger logger = getLogger(SaxonXPathBeanBinder.class.getName());

    String source;

    static class NullIOSource implements BeanBinder.IOSource {
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

    @Override
    public Object serialize(Object destBean, Object io) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Binder getDefaultBinder() {
        return new SaxonXPathBinder(source);
    }
}
