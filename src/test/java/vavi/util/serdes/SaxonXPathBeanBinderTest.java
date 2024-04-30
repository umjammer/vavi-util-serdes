/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * SaxonXPathBeanBinderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/11/07 umjammer initial version <br>
 */
class SaxonXPathBeanBinderTest {

    @Serdes(beanBinder = SaxonXPathBeanBinder.class)
    static class Test1 {
        @Element(value = "//*[local-name() = 'rootfile']/@full-path")
        String fillPath;
    }

    @Test
    void test() throws Exception {
//System.err.println("---- source ----");
//new PrettyPrinter(System.err).print(new InputSource(SaxonXPathBeanBinderTest.class.getResourceAsStream("/container.xml")));
//System.err.println("---- xpath ----");
//XPathDebugger.getEntryList(new InputSource(SaxonXPathBeanBinderTest.class.getResourceAsStream("/container.xml"))).forEach(System.err::println);

        Test1 test = new Test1();
        Serdes.Util.deserialize(SaxonXPathBeanBinderTest.class.getResourceAsStream("/container.xml"), test);
System.err.println(test.fillPath);
        assertEquals("item/standard.opf", test.fillPath);
    }
}
