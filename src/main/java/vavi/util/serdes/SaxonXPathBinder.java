/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.sf.saxon.om.NodeInfo;
import org.xml.sax.InputSource;
import vavi.beans.BeanUtil;
import vavi.util.Debug;


/**
 * SaxonXPathBinder.
 * <ul>
 * <li> {@link Element#value()} ... XPath
 * <li> fieldValue ... default value
 * </ul>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/11/07 umjammer initial version <br>
 */
public class SaxonXPathBinder implements Binder {

    private static final String JAXP_KEY_XPF = XPathFactory.DEFAULT_PROPERTY_NAME + ":" + XPathFactory.DEFAULT_OBJECT_MODEL_URI;
    private static final String JAXP_VALUE_XPF_SAXON = "net.sf.saxon.xpath.XPathFactoryImpl";

    /** */
    private XPath xPath;

    {
        String backup = System.setProperty(JAXP_KEY_XPF, JAXP_VALUE_XPF_SAXON);
        System.setProperty(JAXP_KEY_XPF, JAXP_VALUE_XPF_SAXON);
        XPathFactory factory = XPathFactory.newInstance();
        assert factory.getClass().getName().equals(JAXP_VALUE_XPF_SAXON) : "not saxon factory: " + factory.getClass().getName();
Debug.println(Level.FINE, "XPathFactory: " + factory.getClass().getName());
        xPath = factory.newXPath();
        if (backup != null)
            System.setProperty(JAXP_KEY_XPF, backup);
    }

    private String source;

    SaxonXPathBinder(String source) {
        this.source = source;
    }

    // Boolean
    protected EachBinder booleanEachBinder = new BooleanEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String xpath = Element.Util.getValue(field);
            try {
                String text = (String) xPath.evaluate(xpath, new InputSource(new StringReader(source)), XPathConstants.STRING);
                context.setValue(Boolean.parseBoolean(text));
            } catch (XPathExpressionException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    // Integer
    protected EachBinder integerEachBinder = new IntegerEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String xpath = Element.Util.getValue(field);
            try {
                String text = (String) xPath.evaluate(xpath, new InputSource(new StringReader(source)), XPathConstants.STRING);
                context.setValue(Integer.parseInt(text));
            } catch (XPathExpressionException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    // Short
    protected EachBinder shortEachBinder = new ShortEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String xpath = Element.Util.getValue(field);
            try {
                String text = (String) xPath.evaluate(xpath, new InputSource(new StringReader(source)), XPathConstants.STRING);
                context.setValue(Short.parseShort(text));
            } catch (XPathExpressionException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    // Byte
    protected EachBinder byteEachBinder = new ByteEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String xpath = Element.Util.getValue(field);
            try {
                String text = (String) xPath.evaluate(xpath, new InputSource(new StringReader(source)), XPathConstants.STRING);
                context.setValue(Byte.parseByte(text));
            } catch (XPathExpressionException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    // Long
    protected EachBinder longEachBinder = new LongEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String xpath = Element.Util.getValue(field);
            try {
                String text = (String) xPath.evaluate(xpath, new InputSource(new StringReader(source)), XPathConstants.STRING);
                context.setValue(Long.parseLong(text));
            } catch (XPathExpressionException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    // Float
    protected EachBinder floatEachBinder = new FloatEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String xpath = Element.Util.getValue(field);
            try {
                String text = (String) xPath.evaluate(xpath, new InputSource(new StringReader(source)), XPathConstants.STRING);
                context.setValue(Float.parseFloat(text));
            } catch (XPathExpressionException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    // Double
    protected EachBinder doubleEachBinder = new DoubleEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String xpath = Element.Util.getValue(field);
            try {
                String text = (String) xPath.evaluate(xpath, new InputSource(new StringReader(source)), XPathConstants.STRING);
                context.setValue(Double.parseDouble(text));
            } catch (XPathExpressionException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    // Character
    protected EachBinder characterEachBinder = new CharacterEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String xpath = Element.Util.getValue(field);
            try {
                String text = (String) xPath.evaluate(xpath, new InputSource(new StringReader(source)), XPathConstants.STRING);
                context.setValue((char) Integer.parseInt(text));
            } catch (XPathExpressionException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    // Array
    protected EachBinder arrayEachBinder = new ArrayEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            Class<?> fieldElementClass = field.getType().getComponentType();
            DefaultBeanBinder.DefaultEachContext eachContext = (DefaultBeanBinder.DefaultEachContext) context;
            Object fieldValue = BeanUtil.getFieldValue(field, destBean);
            if (fieldValue != null) {
                eachContext.size = Array.getLength(fieldValue);
            }
            String sizeScript = Element.Util.getValue(field);
Debug.println(Level.FINER, sizeScript);
            if (!sizeScript.isEmpty()) {
                eachContext.size = Double.valueOf(eachContext.eval(sizeScript).toString()).intValue();
            }

            // object array
            // TODO exclude primitives and base classes
            Serdes annotation = fieldElementClass.getAnnotation(Serdes.class);
            if (annotation == null) {
                throw new UnsupportedOperationException("use @Bound: " + fieldElementClass.getTypeName() + "] at " + field.getName() + " (" + context.getSequence() + ")");
            }

            try {
                String xpath = Element.Util.getValue(field);
                Object nodeSet = xPath.evaluate(xpath, new InputSource(new StringReader(source)), XPathConstants.NODESET);
                @SuppressWarnings("unchecked")
                List<NodeInfo> nodeList = (List<NodeInfo>) nodeSet;
if (nodeList.size() == 0) {
 Debug.println(Level.WARNING, "no node list: " + xpath);
}
                List<Object> result = new ArrayList<>();
                for (int i = 0; i < eachContext.size; i++) {
                    Object fieldBean = fieldElementClass.newInstance();
                    eachContext.deserialize(fieldBean);
                    result.add(fieldBean);
                }
                context.setValue(result.toArray());
            } catch (InstantiationException | IllegalAccessException | XPathExpressionException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    // String
    protected EachBinder stringEachBinder = new StringEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String xpath = Element.Util.getValue(field);
            try {
                String text = (String) xPath.evaluate(xpath, new InputSource(new StringReader(source)), XPathConstants.STRING);
Debug.println(Level.FINER, "xpath: " + xpath + ", string: " + text);
                context.setValue(text);
            } catch (XPathExpressionException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    /** */
    private EachBinder[] eachBinders = {
        booleanEachBinder,
        integerEachBinder,
        shortEachBinder,
        byteEachBinder,
        longEachBinder,
        floatEachBinder,
        doubleEachBinder,
        characterEachBinder,
        arrayEachBinder,
        stringEachBinder,
    };


    @Override
    public EachBinder[] getEachBinders() {
        return eachBinders;
    }
}

/* */
