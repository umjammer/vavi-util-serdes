/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.util.prefs.Preferences;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * PreferencesBeanBinderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/02/26 umjammer initial version <br>
 */
class PreferencesBeanBinderTest {

    @Serdes(beanBinder = PreferencesBeanBinder.class)
    static class Test1 {
        @Element
        int i1;
        @Element
        String s2;
        @Element
        boolean b3 = true; // default value
        @Element
        int i4 = 98765; // default value
        @Element
        String s5 = "namachapanda";
        @Element("six") // use #value() as name
        int i6;
        @Element("seven") // use #value() as name
        String s7;
        @Element
        byte[] ba8;
    }

    @Test
    void test1() throws Exception {
        String name = "test/test1";
        Preferences prefs = Preferences.userRoot().node(name);
        prefs.put("s2", "umjammer");
        prefs.putInt("i1", 123456);
        prefs.putInt("six", 666);
        prefs.put("seven", "damian");
        prefs.putByteArray("ba8", "hello world".getBytes());
        prefs.flush();

        Test1 test = new Test1();
        Serdes.Util.deserialize(name, test);
        assertEquals("umjammer", test.s2);
        assertEquals(123456, test.i1);
        assertTrue(test.b3);
        assertEquals(98765, test.i4);
        assertEquals("namachapanda", test.s5);
        assertEquals(666, test.i6);
        assertEquals("damian", test.s7);
        assertArrayEquals("hello world".getBytes(), test.ba8);

        prefs.exportNode(System.out);

        prefs.removeNode();
    }
}
