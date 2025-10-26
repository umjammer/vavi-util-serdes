/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


/**
 * JSoupCssSelectorBeanBinderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/11/07 umjammer initial version <br>
 */
class JSoupCssSelectorBeanBinderTest {

    @Serdes(beanBinder = JSoupCssSelectorBeanBinder.class)
    static class Test1 {
        @Element(value = "rootfile[fill-path]")
        String fillPath;
    }

    @Test
    @Disabled("jsoup url is for http(s) only")
    void test() throws Exception {
        Test1 test = new Test1();
        Serdes.Util.deserialize(JSoupCssSelectorBeanBinderTest.class.getResourceAsStream("/container.xml"), test);
System.err.println(test.fillPath);
    }
}
