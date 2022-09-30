/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;


/**
 * JacksonXMLBeanBinderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/03/15 umjammer initial version <br>
 */
class JacksonXMLBeanBinderTest {

    @Serdes(beanBinder = JacksonXMLBeanBinder.class)
    class Test1 {
        public int i1; // w/o annotation, should be public
        public String s2;
        public boolean b3 = true;
        public int i4 = 98765;
        public String s5 = "namachapanda";
        public int i6;
        @JacksonXmlProperty
        String s7; // w/ annotation, private ok
        public byte[] ba8;
    }

    @Test
    void test() throws Exception {
        Test1 test = new Test1();
        String r = (String) Serdes.Util.serialize(test, "");
System.err.println(r);
    }
}

/* */
