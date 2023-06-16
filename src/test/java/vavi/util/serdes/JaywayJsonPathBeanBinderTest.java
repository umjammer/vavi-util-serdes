/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


/**
 * JaywayJsonPathBeanBinderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/12/18 umjammer initial version <br>
 */
class JaywayJsonPathBeanBinderTest {

    @Serdes(beanBinder = JaywayJsonPathBeanBinder.class)
    static class Store {
        @Element(sequence = 1, value = "$.store.book[*]")
        List<Book> books;
        @Serdes(beanBinder = JaywayJsonPathBeanBinder.class)
        static class Book {
            @Element(sequence = 1, value = "$.category")
            String category;
            @Element(sequence = 2, value = "$.author")
            String author;
            @Element(sequence = 3, value = "$.title")
            String title;
            @Element(sequence = 4, value = "$.price")
            int price;
        }
    }

    @Test
    @Disabled
    void test() throws Exception {
        Store store = new Store();
        Store r = Serdes.Util.deserialize(JaywayJsonPathBinder.class.getResourceAsStream("/test.json"), store);
System.err.println(r);
    }
}

/* */
