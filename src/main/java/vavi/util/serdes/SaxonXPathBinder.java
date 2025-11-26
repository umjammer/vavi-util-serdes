/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.io.IOException;
import java.io.StringReader;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.sf.saxon.om.NodeInfo;
import org.xml.sax.InputSource;
import vavi.beans.BeanUtil;

import static java.lang.System.getLogger;


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

    private static final Logger logger = getLogger(SaxonXPathBinder.class.getName());

    private static final String JAXP_KEY_XPF = XPathFactory.DEFAULT_PROPERTY_NAME + ":" + XPathFactory.DEFAULT_OBJECT_MODEL_URI;
    private static final String JAXP_VALUE_XPF_SAXON = "net.sf.saxon.xpath.XPathFactoryImpl";

    /** */
    private final XPath xPath;

    {
        String backup = System.setProperty(JAXP_KEY_XPF, JAXP_VALUE_XPF_SAXON);
        System.setProperty(JAXP_KEY_XPF, JAXP_VALUE_XPF_SAXON);
        XPathFactory factory = XPathFactory.newInstance();
        assert factory.getClass().getName().equals(JAXP_VALUE_XPF_SAXON) : "not saxon factory: " + factory.getClass().getName();
logger.log(Level.DEBUG, "XPathFactory: " + factory.getClass().getName());
        xPath = factory.newXPath();
        if (backup != null)
            System.setProperty(JAXP_KEY_XPF, backup);
    }

    private final String source;

    SaxonXPathBinder(String source) {
        this.source = source;
    }

    // Boolean
    protected final EachBinder booleanEachBinder = new BooleanEachBinder() {
        public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            String xpath = element.getValue(field);
            try {
                String text = (String) xPath.evaluate(xpath, new InputSource(new StringReader(source)), XPathConstants.STRING);
                context.setValue(Boolean.parseBoolean(text));
            } catch (XPathExpressionException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    // Integer
    protected final EachBinder integerEachBinder = new IntegerEachBinder() {
        public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            String xpath = element.getValue(field);
            try {
                String text = (String) xPath.evaluate(xpath, new InputSource(new StringReader(source)), XPathConstants.STRING);
                context.setValue(Integer.parseInt(text));
            } catch (XPathExpressionException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    // Short
    protected final EachBinder shortEachBinder = new ShortEachBinder() {
        public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            String xpath = element.getValue(field);
            try {
                String text = (String) xPath.evaluate(xpath, new InputSource(new StringReader(source)), XPathConstants.STRING);
                context.setValue(Short.parseShort(text));
            } catch (XPathExpressionException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    // Byte
    protected final EachBinder byteEachBinder = new ByteEachBinder() {
        public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            String xpath = element.getValue(field);
            try {
                String text = (String) xPath.evaluate(xpath, new InputSource(new StringReader(source)), XPathConstants.STRING);
                context.setValue(Byte.parseByte(text));
            } catch (XPathExpressionException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    // Long
    protected final EachBinder longEachBinder = new LongEachBinder() {
        public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            String xpath = element.getValue(field);
            try {
                String text = (String) xPath.evaluate(xpath, new InputSource(new StringReader(source)), XPathConstants.STRING);
                context.setValue(Long.parseLong(text));
            } catch (XPathExpressionException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    // Float
    protected final EachBinder floatEachBinder = new FloatEachBinder() {
        public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            String xpath = element.getValue(field);
            try {
                String text = (String) xPath.evaluate(xpath, new InputSource(new StringReader(source)), XPathConstants.STRING);
                context.setValue(Float.parseFloat(text));
            } catch (XPathExpressionException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    // Double
    protected final EachBinder doubleEachBinder = new DoubleEachBinder() {
        public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            String xpath = element.getValue(field);
            try {
                String text = (String) xPath.evaluate(xpath, new InputSource(new StringReader(source)), XPathConstants.STRING);
                context.setValue(Double.parseDouble(text));
            } catch (XPathExpressionException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    // Character
    protected final EachBinder characterEachBinder = new CharacterEachBinder() {
        public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            String xpath = element.getValue(field);
            try {
                String text = (String) xPath.evaluate(xpath, new InputSource(new StringReader(source)), XPathConstants.STRING);
                context.setValue((char) Integer.parseInt(text));
            } catch (XPathExpressionException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    // Array
    protected final EachBinder arrayEachBinder = new ArrayEachBinder() {
        public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            Class<?> fieldElementClass = field.getType().getComponentType();
            DefaultBeanBinder.DefaultEachContext eachContext = (DefaultBeanBinder.DefaultEachContext) context;
            Object fieldValue = BeanUtil.getFieldValue(field, dstBean);
            if (fieldValue != null) {
                eachContext.size = Array.getLength(fieldValue);
            }
            String sizeScript = element.getValue(field);
logger.log(Level.TRACE, sizeScript);
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
                String xpath = element.getValue(field);
                Object nodeSet = xPath.evaluate(xpath, new InputSource(new StringReader(source)), XPathConstants.NODESET);
                @SuppressWarnings("unchecked")
                List<NodeInfo> nodeList = (List<NodeInfo>) nodeSet;
if (nodeList.isEmpty()) {
 logger.log(Level.WARNING, "no node list: " + xpath);
}
                List<Object> result = new ArrayList<>();
                for (int i = 0; i < eachContext.size; i++) {
                    Object fieldBean = fieldElementClass.getDeclaredConstructor().newInstance();
                    eachContext.deserialize(fieldBean, dstBean);
                    result.add(fieldBean);
                }
                context.setValue(result.toArray());
            } catch (InstantiationException | IllegalAccessException | XPathExpressionException |
                     NoSuchMethodException | InvocationTargetException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    // String
    protected final EachBinder stringEachBinder = new StringEachBinder() {
        public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            String xpath = element.getValue(field);
            try {
                String text = (String) xPath.evaluate(xpath, new InputSource(new StringReader(source)), XPathConstants.STRING);
logger.log(Level.TRACE, "xpath: " + xpath + ", string: " + text);
                context.setValue(text);
            } catch (XPathExpressionException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    /** */
    private final EachBinder[] eachBinders = {
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
