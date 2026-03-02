/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import tools.jackson.dataformat.xml.XmlMapper;
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
        xmlMapper = XmlMapper.builder()
            .disable(tools.jackson.dataformat.xml.XmlWriteFeature.WRITE_NULLS_AS_XSI_NIL)
            .build();
    }

    static class DummyIOSource implements BeanBinder.IOSource {
    }

    @Override
    public DummyIOSource getIOSource(Object... args) throws IOException {
        return null;
    }

    /**
     * @param dstBean NEVER FILLED VALUES AS RESULT, USE RETURN VALUE
     * @return new instance of type of dstBean
     */
    @Override
    public Object deserialize(Object io, Object dstBean) throws IOException {
        if (io instanceof String string) {
            return xmlMapper.readValue(string, dstBean.getClass());
        } else if (io instanceof InputStream is) {
            return xmlMapper.readValue(is, dstBean.getClass());
        } else {
            throw new IllegalArgumentException("unsupported class: " + io.getClass().getName());
        }
    }

    /**
     * @param dstBean NEVER FILLED VALUES WHEN io TYPE IS STRING, USE RETURN VALUE
     * @return new instance of type of dstBean
     */
    @Override
    public Object serialize(Object dstBean, Object io) throws IOException {
        if (io instanceof String) {
            return xmlMapper.writeValueAsString(dstBean);
        } else if (io instanceof OutputStream os) {
            xmlMapper.writeValue(os, dstBean);
            return io;
        } else {
            throw new IllegalArgumentException("unsupported class: " + io.getClass().getName());
        }
    }
}
