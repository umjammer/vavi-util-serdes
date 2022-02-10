/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.injection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;

import vavi.test.box.Box;
import vavi.util.ByteUtil;
import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * InjectorTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/04/07 umjammer initial version <br>
 */
class InjectorTest {

    @Test
    void test() throws Exception {
        InputStream is = Injector.class.getResourceAsStream("/sample4.m4a");
        Box box = new Box();
        Injector.Util.inject(is, box);
    }

    @Injector
    class Test2 {
        @Element(sequence = 1, value = "10") // value for String means source bytes length
        String str;
    }

    @Test
    void test2() throws Exception {
        InputStream is = new ByteArrayInputStream("Hello my name is Naohide.".getBytes());
        Test2 test = new Test2();
        Injector.Util.inject(is, test);
        assertEquals("Hello my n", test.str);
    }

    @Injector(bigEndian = false) // global setting (LE)
    class Test3 {
        @Element(sequence = 1, bigEndian = "true") // each setting (BE)
        int i1;
        @Element(sequence = 2) // default setting (LE)
        int i2;
    }

    @Test
    void test3() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(ByteUtil.getBeBytes(0xfedcba98));
        baos.write(ByteUtil.getBeBytes(0xfedcba98));
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test3 test = new Test3();
        Injector.Util.inject(is, test);
        assertEquals(0xfedcba98, test.i1);
        assertEquals(0x98badcfe, test.i2);
    }

    @Injector
    static class Test4 {
        @Element(sequence = 1)
        int i1;
        @Injector
        static class Test4_Child { // nested class definition
            @Element(sequence = 1)
            int i1;
        }
        @Element(sequence = 2)
        Test4_Child c2 = new Test4_Child(); // nested class object
    }

    @Test
    void test4() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(ByteUtil.getBeBytes(0x12345678));
        baos.write(ByteUtil.getBeBytes(0x87654321));
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test4 test = new Test4();
        Injector.Util.inject(is, test);
        assertEquals(0x12345678, test.i1);
        assertEquals(0x87654321, test.c2.i1);
    }

    @Injector
    static class Test5 {
        @Element(sequence = 1)
        int i1;
        @Element(sequence = 2, condition = "condition") // method name
        int i2;
        @Element(sequence = 3, condition = "condition")
        int i3;
        @Element(sequence = 4, condition = "condition")
        int i4;
        boolean condition(int sequence) {
Debug.println(Level.FINE, "sequence: " + sequence + ", i1: " + i1);
            return i1 == sequence;
        }
    }

    @Test
    void test5() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(ByteUtil.getBeBytes(2));
        baos.write(ByteUtil.getBeBytes(100));
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test5 test = new Test5();
        Injector.Util.inject(is, test);
        assertEquals(2, test.i1);
        assertEquals(100, test.i2);
        assertEquals(0, test.i3);
        assertEquals(0, test.i4);

        baos = new ByteArrayOutputStream();
        baos.write(ByteUtil.getBeBytes(3));
        baos.write(ByteUtil.getBeBytes(200));
        is = new ByteArrayInputStream(baos.toByteArray());

        test = new Test5();
        Injector.Util.inject(is, test);
        assertEquals(3, test.i1);
        assertEquals(0, test.i2);
        assertEquals(200, test.i3);
        assertEquals(0, test.i4);

        baos = new ByteArrayOutputStream();
        baos.write(ByteUtil.getBeBytes(4));
        baos.write(ByteUtil.getBeBytes(300));
        is = new ByteArrayInputStream(baos.toByteArray());

        test = new Test5();
        Injector.Util.inject(is, test);
        assertEquals(4, test.i1);
        assertEquals(0, test.i2);
        assertEquals(0, test.i3);
        assertEquals(300, test.i4);
    }

    @Injector
    static class Test6 {
        @Element(sequence = 1, validation = "6")
        int i1;
        @Element(sequence = 2, value = "4", validation = "\"sano\".getBytes()")
        byte[] i2;
        @Element(sequence = 3, validation = "3")
        Integer i3;
    }

    @Test
    void test6() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(ByteUtil.getBeBytes(6));
        baos.write("sano".getBytes());
        baos.write(ByteUtil.getBeBytes(3));
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test6 test = new Test6();
        Injector.Util.inject(is, test);
        assertEquals(6, test.i1);
        assertArrayEquals("sano".getBytes(), test.i2);
        assertEquals(3, test.i3);
    }

    @Injector
    static class Test7 {
        @Element(sequence = 1, validation = "6")
        int i1;
    }

    @Test
    void test7() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(ByteUtil.getBeBytes(7)); // validation error
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test7 test = new Test7();
        assertThrows(IllegalArgumentException.class, () -> {
            Injector.Util.inject(is, test);
        });
    }

    @Injector
    static class Test8S {
        @Element(sequence = 1, value= "5", validation = "\"Super\"")
        String s1;
    }

    static class Test8 extends Test8S {
        @Element(sequence = 2, value = "3", validation = "\"Sub\"")
        String s2;
    }

    @Test
    void test8() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write("Super".getBytes());
        baos.write("Sub".getBytes());
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test8 test = new Test8();
        Injector.Util.inject(is, test);
        assertEquals("Super", test.s1);
        assertEquals("Sub", test.s2);
    }
}

/* */
