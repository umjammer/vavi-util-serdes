/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.logging.Level;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.xml.sax.InputSource;
import vavi.util.Debug;
import vavi.xml.util.PrettyPrinter;


/**
 * SaxonXPathBeanBinder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-11-07 nsano initial version <br>
 */
public class SaxonXPathBeanBinder extends SimpleBeanBinder<SaxonXPathBeanBinder.NullIOSource> {

    String source;

    static class NullIOSource implements BeanBinder.IOSource {
    }

    @Override
    public NullIOSource getIOSource(Object... args) throws IOException {
        NullIOSource io = new NullIOSource();
        if (args[0] instanceof InputStream) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream is = (InputStream) args[0];
            byte[] b = new byte[8192];
            while (true) {
                int r = is.read(b);
                if (r < 0) break;
                baos.write(b, 0, r);
            }
            source = baos.toString();
Debug.println(Level.FINE, "source: " + source);
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
        return new SaxonBinder(source);
    }
}
