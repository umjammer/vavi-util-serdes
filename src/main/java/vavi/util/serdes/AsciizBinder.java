/*
 * Copyright (c) 2003 by Naohide Sano, All Rights Reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.Field;
import java.nio.charset.Charset;

import vavi.beans.BeanUtil;
import vavi.util.StringUtil;
import vavi.util.serdes.DefaultBeanBinder.DefaultEachContext;


/**
 * AsciizBinder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 031216 vavi initial version <br>
 */
public class AsciizBinder implements Binder {

    private static final Logger logger = System.getLogger(AsciizBinder.class.getName());

    /**
     * only for String field.
     * {@link Element#value()} ... array size must be set
     */
    public void bind(EachContext context, Object dstBean, Field field) throws IOException {
        DefaultEachContext eachContext = (DefaultEachContext) context;
        String sizeScript = element.getValue(field);
logger.log(Level.TRACE, "sizeScript: " + sizeScript);
        if (!sizeScript.isEmpty()) {
            eachContext.size = Double.valueOf(eachContext.eval(sizeScript).toString()).intValue(); // size means array size
        }
        if (eachContext.size == 0) throw new IllegalStateException("size must be set for: " + field.getName());

        Class<?> fieldClass = field.getType();
        if (fieldClass.equals(String.class)) {
            byte[] bytes = asciiz(eachContext);
            String encoding = element.getEncoding(field);
            if (encoding.isEmpty()) {
                encoding = serdes.encoding(dstBean.getClass());
            }
            if (!encoding.isEmpty()) {
logger.log(Level.DEBUG, encoding);
                context.setValue(new String(bytes, Charset.forName(encoding)));
            } else {
logger.log(Level.DEBUG, () -> "no encoding: " + bytes.length + " bytes\n" + StringUtil.getDump(bytes));
                context.setValue(new String(bytes));
            }

            BeanUtil.setFieldValue(field, dstBean, context.getValue());
        } else {
            throw new UnsupportedOperationException("this binder supports String only");
        }
    }

    /** get 0x00 ended byte array, array size must be <= context#size */
    private static byte[] asciiz(DefaultEachContext context) throws IOException {
        int l = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (l < context.size) {
            int r = context.dis.readByte();
            if (r <= 0) break;
            baos.write(r);
            l++;
        }
        context.dis.skipBytes(context.size - 1 - l);
        return baos.toByteArray();
    }
}
