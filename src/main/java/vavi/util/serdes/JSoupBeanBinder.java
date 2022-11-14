/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


/**
 * JSoupBeanBinder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-11-07 nsano initial version <br>
 */
public class JSoupBeanBinder extends SimpleBeanBinder<JSoupBeanBinder.NullIOSource> {

    Document document;

    static class NullIOSource implements BeanBinder.IOSource {
    }

    @Override
    public NullIOSource getIOSource(Object... args) throws IOException {
        NullIOSource io = new NullIOSource();
        if (args[0] instanceof InputStream) {
            InputStream is = (InputStream) args[0];
            document = Jsoup.parse(is, StandardCharsets.UTF_8.name(), "");
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
        return new JSoupBinder(document);
    }
}
