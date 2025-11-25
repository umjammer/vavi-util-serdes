/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import vavi.io.LittleEndianDataOutputStream;
import vavi.test.box.Box;
import vavi.util.ByteUtil;
import vavi.util.Debug;
import vavi.util.StringUtil;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * SerdesTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/04/07 umjammer initial version <br>
 */
class SerdesTest {

    @Test
    @DisplayName("Box injection")
    void test() throws Exception {
        InputStream is = SerdesTest.class.getResourceAsStream("/sample4.m4a");
        Box box = new Box();
        Serdes.Util.deserialize(is, box);
    }

    @Serdes
    static class Test2 {
        @Element(sequence = 1, value = "10") // value for String means source bytes length
        String str;
    }

    @Test
    @DisplayName("constraint by value")
    void test2() throws Exception {
        InputStream is = new ByteArrayInputStream("Hello my name is Naohide.".getBytes());
        Test2 test = new Test2();
        Serdes.Util.deserialize(is, test);
        assertEquals("Hello my n", test.str);
    }

    @Serdes(bigEndian = false) // global setting (LE)
    static class Test3 {
        @Element(sequence = 1, bigEndian = "true") // each setting (BE)
        int i1;
        @Element(sequence = 2) // default setting (LE)
        int i2;
    }

    @Test
    @DisplayName("big/little endian mixture")
    void test3() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(ByteUtil.getBeBytes(0xfedcba98));
        baos.write(ByteUtil.getBeBytes(0xfedcba98));
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test3 test = new Test3();
        Serdes.Util.deserialize(is, test);
        assertEquals(0xfedcba98, test.i1);
        assertEquals(0x98badcfe, test.i2);
    }

    @Serdes
    static class Test4 {
        @Element(sequence = 1)
        int i1;
        @Serdes
        static class Test4_Child { // nested class definition
            @Element(sequence = 1) // sequence is independent of the container class
            int i1;
        }
        @Element(sequence = 2)
        Test4_Child c2 = new Test4_Child(); // nested class object
    }

    @Test
    @DisplayName("nested class")
    void test4() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(ByteUtil.getBeBytes(0x12345678));
        baos.write(ByteUtil.getBeBytes(0x87654321));
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test4 test = new Test4();
        Serdes.Util.deserialize(is, test);
        assertEquals(0x12345678, test.i1);
        assertEquals(0x87654321, test.c2.i1);
    }

    @Serdes
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
    @DisplayName("condition method call")
    void test5() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(ByteUtil.getBeBytes(2));
        baos.write(ByteUtil.getBeBytes(100));
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test5 test = new Test5();
        Serdes.Util.deserialize(is, test);
        assertEquals(2, test.i1);
        assertEquals(100, test.i2);
        assertEquals(0, test.i3);
        assertEquals(0, test.i4);

        baos = new ByteArrayOutputStream();
        baos.write(ByteUtil.getBeBytes(3));
        baos.write(ByteUtil.getBeBytes(200));
        is = new ByteArrayInputStream(baos.toByteArray());

        test = new Test5();
        Serdes.Util.deserialize(is, test);
        assertEquals(3, test.i1);
        assertEquals(0, test.i2);
        assertEquals(200, test.i3);
        assertEquals(0, test.i4);

        baos = new ByteArrayOutputStream();
        baos.write(ByteUtil.getBeBytes(4));
        baos.write(ByteUtil.getBeBytes(300));
        is = new ByteArrayInputStream(baos.toByteArray());

        test = new Test5();
        Serdes.Util.deserialize(is, test);
        assertEquals(4, test.i1);
        assertEquals(0, test.i2);
        assertEquals(0, test.i3);
        assertEquals(300, test.i4);
    }

    @Serdes
    static class Test6 {
        @Element(sequence = 1, validation = "6")
        int i1;
        @Element(sequence = 2, value = "4", validation = "\"sano\".getBytes()")
        byte[] i2;
        @Element(sequence = 3, validation = "3")
        Integer i3;
    }

    @Test
    @DisplayName("validation")
    void test6() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(ByteUtil.getBeBytes(6));
        baos.write("sano".getBytes());
        baos.write(ByteUtil.getBeBytes(3));
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test6 test = new Test6();
        Serdes.Util.deserialize(is, test);
        assertEquals(6, test.i1);
        assertArrayEquals("sano".getBytes(), test.i2);
        assertEquals(3, test.i3);
    }

    @Serdes
    static class Test7 {
        @Element(sequence = 1, validation = "6")
        int i1;
    }

    @Test
    @DisplayName("validation error")
    void test7() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(ByteUtil.getBeBytes(7)); // validation error
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test7 test = new Test7();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
            Serdes.Util.deserialize(is, test)
        );
        assertTrue(e.getMessage().contains("validation"), e.getMessage());
    }

    @Serdes
    static class Test8S {
        @Element(sequence = 1, value= "5", validation = "\"Super\"") // sequence is must be unique among this class and super classes and subclasses
        String s1;
    }

    /** uniqueness for all sequences are guaranteed by {@link BaseBeanBinder#validateSequences} */
    static class Test8 extends Test8S {
        @Element(sequence = 2, value = "3", validation = "\"Sub\"") // sequence is must be unique among this class and super classes and subclasses
        String s2;
    }

    @Test
    @DisplayName("super class element")
    void test8() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write("Super".getBytes());
        baos.write("Sub".getBytes());
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test8 test = new Test8();
        Serdes.Util.deserialize(is, test);
        assertEquals("Super", test.s1);
        assertEquals("Sub", test.s2);
    }

    @Serdes
    static class Test9 {
        @Element(sequence = 1, value = "9")
        int[] ia1;
        @Element(sequence = 2)
        int[] ia2 = new int[5];
    }

    @Test
    @DisplayName("array")
    void test9() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < 18; i++) {
            baos.write(ByteUtil.getBeBytes(i));
        }
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test9 test = new Test9();
        Serdes.Util.deserialize(is, test);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 5, 6 ,7, 8 }, test.ia1);
        assertArrayEquals(new int[] { 9, 10, 11, 12, 13 }, test.ia2);
    }

    @Serdes
    static class Test10 {
        @Element(sequence = 0)
        int i1;
    }

    @Test
    @DisplayName("non natural number sequence")
    void test10() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(ByteUtil.getBeBytes(10));
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test10 test = new Test10();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
            Serdes.Util.deserialize(is, test)
        );
        assertTrue(e.getMessage().contains("sequence should be > 0"), e.getMessage());
    }

    @Serdes
    static class Test11 {
        @Element(sequence = 1)
        int i1;
        @Element(sequence = 1)
        int i2;
    }

    /** uniqueness for all sequences are guaranteed by {@link BaseBeanBinder#validateSequences} */
    @Test
    @DisplayName("duplicate sequence")
    void test11() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(ByteUtil.getBeBytes(11));
        baos.write(ByteUtil.getBeBytes(11));
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test11 test = new Test11();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
            Serdes.Util.deserialize(is, test)
        );
        assertTrue(e.getMessage().contains("duplicate sequence"), e.getMessage());
    }

    @Serdes
    static class Test12 {
        @Element
        int i1;
    }

    @Test
    @DisplayName("w/o sequence for default beanBinder")
    void test12() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(ByteUtil.getBeBytes(10));
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test12 test = new Test12();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
            Serdes.Util.deserialize(is, test)
        );
        assertTrue(e.getMessage().contains("sequence should be > 0"), e.getMessage());
    }

    @Serdes
    static class Test13 {
        @Element(sequence = 1)
        short[] sa = new short[2];
        @Element(sequence = 2)
        long[] la = new long[3];
    }

    @Test
    @DisplayName("short, long array")
    void test13() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeShort(0x1234);
        dos.writeShort(0x5678);
        dos.writeLong(0x1000_0000_0000_0000L);
        dos.writeLong(0x1000_0000_0000_0001L);
        dos.writeLong(0x1000_0000_0000_0002L);
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test13 test = new Test13();
        Serdes.Util.deserialize(is, test);
        assertEquals(0x5678, test.sa[1]);
        assertEquals(0x1000_0000_0000_0002L, test.la[2]);
    }

    @Serdes
    static class Test14 {
        @Element(sequence = 1)
        boolean z;
        @Element(sequence = 2)
        char c;
        @Element(sequence = 3)
        float f;
        @Element(sequence = 4)
        double d;
    }

    @Test
    @DisplayName("boolean, char, float, double")
    void test14() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeBoolean(true);
        dos.writeChar('佐');
        dos.writeFloat((float) Math.E);
        dos.writeDouble(Math.PI);
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test14 test = new Test14();
        Serdes.Util.deserialize(is, test);
        assertTrue(test.z);
        assertEquals('佐', test.c);
        assertEquals((float) Math.E, test.f);
        assertEquals(Math.PI, test.d);
    }

    @Serdes
    static class Test15 {
        public enum A {
            A1, A2
        }
        public enum B {
            B1(10), B2(20);
            final int v;
            int getValue() { return v; } // TODO "getValue" is magic
            B(int v) { this.v = v; }
        }
        @Element(sequence = 1)
        A a;
        @Element(sequence = 2)
        B b;
        @Element(sequence = 3, value = "int")
        B b2;
    }

    /**
     * enum special specification:
     * <li>default deserialized value assumed to short size (2) byte array. you can specify the size at {@code value}</li>
     * <li>default deserialized value is compared with {@code ordinal()}.
     *     the case if there is a method named {@code getValue()}, it is used instead of {@code ordinal()}.</li>
     */
    @Test
    @DisplayName("enum")
    void test15() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeShort(1); // TODO short is default
        dos.writeShort(20);
        dos.writeInt(10);
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test15 test = new Test15();
        Serdes.Util.deserialize(is, test);
        assertEquals(Test15.A.A2, test.a);
        assertEquals(Test15.B.B2, test.b);
        assertEquals(Test15.B.B1, test.b2);
    }

    @Serdes
    static class Test16 {
        @Element(sequence = 1)
        int size;
        @Element(sequence = 2, value = "$1")
        byte[] ba;
    }

    @Test
    @DisplayName("undefined array")
    void test16() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(8);
        dos.write("umjammer".getBytes());
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test16 test = new Test16();
        Serdes.Util.deserialize(is, test);
        assertArrayEquals("umjammer".getBytes(), test.ba);
    }

    @Serdes(encoding = "MS932") // encoding affect all member fields of strings
    static class Test17 {
        @Element(sequence = 1, value = "4") // string length is "bytes length after encoded"
        String s;
    }

    @Test
    @DisplayName("encoding at serdes")
    void test17() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.write("直秀".getBytes(Charset.forName("MS932")));
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test17 test = new Test17();
        Serdes.Util.deserialize(is, test);
        assertEquals("直秀", test.s);
    }

    @Serdes(encoding = "MS932")
    static class Test18 {
        @Element(sequence = 1, value = "4", encoding = "EUCJIS") // encoding affects only here and ignored at encoding at serdes
        String s;
    }

    @Test
    @DisplayName("'encoding' at @Element priority is higher than it at @Serdes")
    void test18() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.write("直秀".getBytes(Charset.forName("EUCJIS")));
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test18 test = new Test18();
        Serdes.Util.deserialize(is, test);
        assertEquals("直秀", test.s);
    }

    @Serdes
    static class Test19 {
        @Element(sequence = 1, value = "8")
        List<Byte> bl;
        @Element(sequence = 2, value = "3")
        List<Long> ll = new ArrayList<>(3);
    }

    @Test
    @DisplayName("list")
    void test19() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write("umjammer".getBytes());
        baos.write(ByteUtil.getBeBytes(13L));
        baos.write(ByteUtil.getBeBytes(14L));
        baos.write(ByteUtil.getBeBytes(15L));
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test19 test = new Test19();
        Serdes.Util.deserialize(is, test);
        assertEquals(8, test.bl.size());
Debug.println(test.bl);
        assertEquals((byte) 'j', test.bl.get(2));
        assertEquals(14L, test.ll.get(1));
    }

    @Serdes
    static class Test20_Child {
        @Element(sequence = 1)
        int child;
        @Override public String toString() { return "Child: " + child; }
    }

    @Serdes
    static class Test20 {
        @Element(sequence = 1)
        int size;
        @Element(sequence = 2, value = "$1") // $1 means field size
        List<Test20_Child> chldren = new ArrayList<>();
    }

    @Test
    @DisplayName("list")
    void test20() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(ByteUtil.getBeBytes(3));
        baos.write(ByteUtil.getBeBytes(100));
        baos.write(ByteUtil.getBeBytes(200));
        baos.write(ByteUtil.getBeBytes(300));
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test20 test = new Test20();
        Serdes.Util.deserialize(is, test);
        assertEquals(3, test.chldren.size());
Debug.println(test.chldren);
        assertEquals(100, test.chldren.get(0).child);
        assertEquals(300, test.chldren.get(2).child);
    }

    @Serdes
    static class Test21 {
        @Element(sequence = 1, value = "byte")
        int value;
    }

    @Test
    @DisplayName("'value' usage for int type")
    void test21() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(-21);
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test21 test = new Test21();
        Serdes.Util.deserialize(is, test);
        assertEquals(-21, test.value);
    }

    @Serdes
    static class Test22 {
        @Element(sequence = 1, validation = "new byte[] {'s', 'a', 'n', 'o'}")
        byte[] b = new byte[4]; // needs instance array
    }

    @Test
    @DisplayName("byte array validation")
    void test22() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write("sano".getBytes());
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test22 test = new Test22();
        Serdes.Util.deserialize(is, test);
        assertArrayEquals(baos.toByteArray(), test.b);
    }

    // TODO $0 is not while size of beans, but stream length at this moment (just before serialising)
    @Serdes
    static class Test23 {
        @Element(sequence = 1, validation = "$0") // $0 means size of Test23
        int size;
        @Element(sequence = 2)
        byte[] b = new byte[3];
    }

    @Test
    @DisplayName("sizeof")
    void test23() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(ByteUtil.getBeBytes(4 + 3));
        baos.write("abc".getBytes());
        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        Test23 test = new Test23();
        Serdes.Util.deserialize(is, test);
        assertEquals(4 + 3, test.size);
        assertArrayEquals("abc".getBytes(), test.b);
    }

    @Serdes
    public static class Test24 {
        @Element(sequence = 1)
        private int id = 99;

        @Element(sequence = 2, value = "9") // value is byte length for String
        private String name = "test-bean";
    }

    @Test
    @DisplayName("serialize")
    void test24() throws Exception {
        Test24 bean = new Test24();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Serdes.Util.serialize(bean, baos);

        ByteArrayOutputStream expectedBaos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(expectedBaos);
        dos.writeInt(99);
        dos.write("test-bean".getBytes());
        assertArrayEquals(expectedBaos.toByteArray(), baos.toByteArray());
    }

    @Serdes
    public static class Test25 {
        public enum A {
            A1, A2
        }
        @Element(sequence = 1, value = "int")
        A enumA = A.A2;
    }

    @Test
    @DisplayName("serialize: enum")
    void test25() throws Exception {
        Test25 bean = new Test25();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Serdes.Util.serialize(bean, baos);

        ByteArrayOutputStream expectedBaos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(expectedBaos);
        dos.writeInt(Test25.A.A2.ordinal());
        assertArrayEquals(expectedBaos.toByteArray(), baos.toByteArray());
    }

    @Serdes(bigEndian = false)
    public static class Test26 {
        @Element(sequence = 1, value = "16") // value is byte length for String
        @Bound(binder = AsciizBinder.class) // set special binder
        String a;
        @Element(sequence = 2)
        short b;
    }

    @Test
    @DisplayName("asciiz binder")
    void test26() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LittleEndianDataOutputStream ledos = new LittleEndianDataOutputStream(baos);
Debug.println("bytes: " + StringUtil.getDump("Naohide Sano\0xyz".getBytes()));
        ledos.write("Naohide Sano\0xyz".getBytes());
        ledos.writeShort(1234);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        Test26 bean = new Test26();
        Serdes.Util.deserialize(bais, bean);
        assertEquals("Naohide Sano", bean.a);
        assertEquals(1234, bean.b);
    }

    @Serdes
    public static class Test27 {
        @Element(sequence = 1)
        public int a; // must be public for script engine
        @Element(sequence = 2)
        Test27Sub b;

        public int doubler(int x) { // must be public for script engine
            return x * 2;
        }

        @Serdes
        public static class Test27Sub {
            @Element(sequence = 1, value = "$__.doubler($__.a)") // '$__' is a parent object
            byte[] c;
        }
    }

    @Test
    @DisplayName("nested class referencing")
    void test27() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(ByteUtil.getBeBytes(3));
        baos.write("xyzabc".getBytes());

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        Test27 bean = new Test27();
        Serdes.Util.deserialize(bais, bean);
        assertEquals(3, bean.a);
        assertArrayEquals("xyzabc".getBytes(), bean.b.c);
    }

    @AfterAll
    static void teardown() throws Exception {
        if (Boolean.parseBoolean(System.getProperty("vavi.util.serdes.cache.statistics", "false")))
            Serdes.Cacher.printCacheStatistics();
    }
}
