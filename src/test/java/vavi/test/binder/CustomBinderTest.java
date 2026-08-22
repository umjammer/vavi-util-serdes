/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.test.binder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;

import vavi.beans.BeanUtil;
import vavi.util.serdes.Binder;
import vavi.util.serdes.Bound;
import vavi.util.serdes.DefaultBeanBinder.DefaultContext;
import vavi.util.serdes.DefaultBeanBinder.DefaultEachContext;
import vavi.util.serdes.Element;
import vavi.util.serdes.Serdes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * A {@link Binder} which is written outside of the {@code vavi.util.serdes} package,
 * it makes sure the utilities a custom binder needs are public.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-08-23 nsano initial version <br>
 */
class CustomBinderTest {

    /** a byte length prefixed string */
    public static class PascalStringBinder implements Binder {

        @Override
        public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            int length = eachContext.dis.readUnsignedByte();
            byte[] bytes = new byte[length];
            eachContext.dis.readFully(bytes);
            eachContext.size = 1 + length;

            context.setValue(new String(bytes, encoding(field, dstBean.getClass())));
            BeanUtil.setFieldValue(field, dstBean, context.getValue());
        }

        @Override
        public void bind(Object srcBean, Field field, EachContext context) throws IOException {
            DefaultEachContext eachContext = (DefaultEachContext) context;
            String value = (String) BeanUtil.getFieldValue(field, srcBean);
            context.setValue(value);
            byte[] bytes = value.getBytes(encoding(field, srcBean.getClass()));

            eachContext.dos.writeByte(bytes.length);
            eachContext.dos.write(bytes);
            eachContext.size = 1 + bytes.length;
        }

        /** {@link Element#encoding()} then {@link Serdes#encoding()} */
        private static Charset encoding(Field field, Class<?> beanClass) {
            String encoding = element.getEncoding(field);
            if (encoding.isEmpty()) {
                encoding = serdes.encoding(beanClass); // Serdes.Util#encoding must be public
            }
            return encoding.isEmpty() ? Charset.defaultCharset() : Charset.forName(encoding);
        }
    }

    @Serdes(bigEndian = false, encoding = "utf-8")
    public static class Test1 {
        @Element(sequence = 1)
        int number;
        @Element(sequence = 2)
        @Bound(binder = PascalStringBinder.class)
        String name;
    }

    /** 1, "なまえ" */
    static final byte[] data = {
            0x01, 0x00, 0x00, 0x00,
            0x09, (byte) 0xe3, (byte) 0x81, (byte) 0xaa, (byte) 0xe3, (byte) 0x81, (byte) 0xbe, (byte) 0xe3, (byte) 0x81, (byte) 0x88
    };

    @Test
    @DisplayName("a binder outside of the package can use the annotation utilities")
    void test() throws Exception {
        InputStream is = new ByteArrayInputStream(data);
        Test1 bean = Serdes.Util.deserialize(is, new Test1());

        assertEquals(1, bean.number);
        assertEquals("なまえ", bean.name);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Serdes.Util.serialize(bean, baos);
        assertArrayEquals(data, baos.toByteArray());
    }

    @Test
    @DisplayName("the annotation accessors are public")
    void test2() {
        assertEquals("utf-8", Binder.serdes.encoding(Test1.class));
        assertFalse(Binder.serdes.isBigEndian(Test1.class));
        assertEquals(2, Binder.serdes.getElementFields(Test1.class).size());
        assertEquals("utf-8", Binder.serdes.getAnnotation(Test1.class).encoding());
        assertEquals("", Binder.element.getEncoding(Binder.serdes.getElementFields(Test1.class).get(1)));
        assertNotNull(Binder.serdes.getBeanBinder(Test1.class));
    }

    @Test
    @DisplayName("a custom bean binder can evaluate a script")
    void test3() throws Exception {
        assertTrue(Modifier.isPublic(DefaultContext.class.getDeclaredMethod("eval", String.class).getModifiers()));
    }
}
