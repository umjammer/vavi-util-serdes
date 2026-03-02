/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import vavi.util.Debug;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * JacksonXMLBeanBinderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/03/15 umjammer initial version <br>
 */
class JacksonXMLBeanBinderTest {

    @Serdes(beanBinder = JacksonXMLBeanBinder.class)
    static class Test1 {
        public int i1; // w/o annotation, should be public
        public String s2;
        public boolean b3 = true;
        public int i4 = 98765;
        public String s5 = "namachapanda";
        public int i6;
        @JacksonXmlProperty
        String s7; // w/ @JacksonXmlProperty annotation, private ok
        public byte[] ba8;
    }

    @Test
    @DisplayName("serdes api serialize")
    void test() throws Exception {
        Test1 test = new Test1();
        String r = Serdes.Util.serialize(test, "");
Debug.println(r);
        assertEquals("<Test1><b3>true</b3><ba8/><i1>0</i1><i4>98765</i4><i6>0</i6><s2/><s5>namachapanda</s5><s7/></Test1>", r);
    }

    @Serdes(beanBinder = JacksonXMLBeanBinder.class)
    @JacksonXmlRootElement(localName = "container")
    public static class Container {

        public static class RootFiles {

            public static class RootFile {

                @JacksonXmlProperty(isAttribute = true, localName = "full-path")
                String fullPath;
            }

            @JacksonXmlProperty(localName = "rootfile")
            @JacksonXmlElementWrapper(useWrapping = false) // for list
            List<RootFile> rootfile;
        }

        @JacksonXmlProperty(localName = "rootfiles")
        RootFiles rootfiles;
    }

    @Test
    @DisplayName("serdes api deserialize")
    void test2() throws Exception {
        Container container = new Container();
        container = Serdes.Util.deserialize(JacksonXMLBeanBinderTest.class.getResourceAsStream("/container.xml"), container);
        assertEquals("item/standard.opf", container.rootfiles.rootfile.get(0).fullPath);
    }

    static final String xml = """
            <?xml version="1.0"?>
            <container xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
              <rootfiles>
                <rootfile full-path="item/standard.opf" media-type="application/oebps-package+xml"/>
              </rootfiles>
            </container>
            """;

    @Test
    @DisplayName("raw api by string")
    void test3() throws Exception {

        XmlMapper xmlMapper = new XmlMapper();
        Container container = xmlMapper.readValue(xml, Container.class);

        assertEquals("item/standard.opf", container.rootfiles.rootfile.get(0).fullPath);
    }

    @Test
    @DisplayName("raw api by stream")
    void test4() throws Exception {

        XmlMapper xmlMapper = new XmlMapper();
        Container container = xmlMapper.readValue(Files.newInputStream(Path.of(JacksonXMLBeanBinderTest.class.getResource("/container.xml").toURI())), Container.class);

        assertEquals("item/standard.opf", container.rootfiles.rootfile.get(0).fullPath);
    }
}
