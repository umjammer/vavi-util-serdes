/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.jayway.jsonpath.JsonPath;
import vavi.beans.BeanUtil;
import vavi.util.Debug;


/**
 * JaywayJsonPathBinder.
 * <ul>
 * <li> {@link Element#value()} ... JsonPath
 * <li> fieldValue ... default value
 * </ul>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/12/18 umjammer initial version <br>
 * @see "https://github.com/json-path/JsonPath"
 */
public class JaywayJsonPathBinder implements Binder {

    private String source;

    JaywayJsonPathBinder(String source) {
        this.source = source;
    }

    // Boolean
    protected EachBinder booleanEachBinder = new BooleanEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String jsonPath = Element.Util.getValue(field);
            String text = JsonPath.parse(source).read(jsonPath);
            context.setValue(Boolean.parseBoolean(text));
        }
    };

    // Integer
    protected EachBinder integerEachBinder = new IntegerEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String jsonPath = Element.Util.getValue(field);
            String text = JsonPath.parse(source).read(jsonPath);
            context.setValue(Integer.parseInt(text));
        }
    };

    // Short
    protected EachBinder shortEachBinder = new ShortEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String jsonPath = Element.Util.getValue(field);
            String text = JsonPath.parse(source).read(jsonPath);
            context.setValue(Short.parseShort(text));
        }
    };

    // Byte
    protected EachBinder byteEachBinder = new ByteEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String jsonPath = Element.Util.getValue(field);
            String text = JsonPath.parse(source).read(jsonPath);
            context.setValue(Byte.parseByte(text));
        }
    };

    // Long
    protected EachBinder longEachBinder = new LongEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String jsonPath = Element.Util.getValue(field);
            String text = JsonPath.parse(source).read(jsonPath);
            context.setValue(Long.parseLong(text));
        }
    };

    // Float
    protected EachBinder floatEachBinder = new FloatEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String jsonPath = Element.Util.getValue(field);
            String text = JsonPath.parse(source).read(jsonPath);
            context.setValue(Float.parseFloat(text));
        }
    };

    // Double
    protected EachBinder doubleEachBinder = new DoubleEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String jsonPath = Element.Util.getValue(field);
            String text = JsonPath.parse(source).read(jsonPath);
            context.setValue(Double.parseDouble(text));
        }
    };

    // Character
    protected EachBinder characterEachBinder = new CharacterEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String jsonPath = Element.Util.getValue(field);
            String text = JsonPath.parse(source).read(jsonPath);
            context.setValue((char) Integer.parseInt(text));
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
                String jsonPath = Element.Util.getValue(field);
                List<String> list = JsonPath.read(source, jsonPath);
if (list.size() == 0) {
 Debug.println(Level.WARNING, "no list: " + jsonPath);
}
                List<Object> result = new ArrayList<>();
                for (int i = 0; i < eachContext.size; i++) {
                    Object fieldBean = fieldElementClass.newInstance();
                    eachContext.deserialize(fieldBean);
                    result.add(fieldBean);
                }
                context.setValue(result.toArray());
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    // String
    protected EachBinder stringEachBinder = new StringEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String jsonPath = Element.Util.getValue(field);
            String text = JsonPath.parse(source).read(jsonPath);
            context.setValue(text);
Debug.println(Level.FINER, "jsonPath: " + jsonPath + ", string: " + text);
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
