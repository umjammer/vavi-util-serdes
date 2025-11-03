/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import vavi.util.serdes.JacksonXMLBeanBinder.DummyIOSource;


/**
 * JacksonXMLBeanBinder.
 * <p>
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/02/26 umjammer initial version <br>
 */
public class JacksonXMLBeanBinder implements BeanBinder<DummyIOSource> {

    private final XmlMapper xmlMapper;

    public JacksonXMLBeanBinder() {
        xmlMapper = new XmlMapper();
        xmlMapper.configure(FromXmlParser.Feature.EMPTY_ELEMENT_AS_NULL, true);
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // @JsonIgnoreProperties(ignoreUnknown = true)

//        xmlMapper.getFactory().getXMLInputFactory().setProperty("javax.xml.stream.isNamespaceAware", false); // no need
    }

    static class DummyIOSource implements BeanBinder.IOSource {
    }

    @Override
    public DummyIOSource getIOSource(Object... args) throws IOException {
        return null;
    }

    /**
     * @param destBean NEVER FILLED VALUES AS RESULT, USE RETURN VALUE
     * @return new instance of type of destBean
     */
    @Override
    public Object deserialize(Object io, Object destBean) throws IOException {
        if (io instanceof String string) {
            return xmlMapper.readValue(string, destBean.getClass());
        } else if (io instanceof InputStream is) {
            return xmlMapper.readValue(is, destBean.getClass());
        } else {
            throw new IllegalArgumentException("unsupported class: " + io.getClass().getName());
        }
    }

    /**
     * @param destBean NEVER FILLED VALUES WHEN io TYPE IS STRING, USE RETURN VALUE
     * @return new instance of type of destBean
     */
    @Override
    public Object serialize(Object destBean, Object io) throws IOException {
        if (io instanceof String) {
            return xmlMapper.writeValueAsString(destBean);
        } else if (io instanceof OutputStream os) {
            xmlMapper.writeValue(os, destBean);
            return io;
        } else {
            throw new IllegalArgumentException("unsupported class: " + io.getClass().getName());
        }
    }
}
