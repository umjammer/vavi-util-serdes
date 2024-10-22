/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.lang.reflect.Method;
import java.util.Arrays;

import vavi.beans.ClassUtil;
import vavi.util.Debug;


/**
 * EnumTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-10-10 nsano initial version <br>
 */
public class EnumTest {

    enum TestEnum1 {
        ELEMENT_1A,
        ELEMENT_1B
    }

    enum TestEnum2 {
        ELEMENT_2A(0x1L),
        ELEMENT_2B(0x2L);
        final long v;

        TestEnum2(long v) {
            this.v = v;
        }
        TestEnum2(String[] x) {
            this(0);
        }
    }

    /** */
    public static void main(String[] args) throws Exception {
        Class<? extends Enum<?>> e = TestEnum2.class;
Debug.println("isEnum: " + e.isEnum());
Arrays.stream(e.getDeclaredConstructors()).forEach(c ->
 System.err.println(c.getName() +"." + ClassUtil.signatureWithName(c))
);
        Method m = EnumTest.class.getMethod("main", String[].class);
Debug.println(ClassUtil.signatureWithName(m));
    }
}
