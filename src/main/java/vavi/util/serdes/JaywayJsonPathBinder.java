/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import com.jayway.jsonpath.JsonPath;
import vavi.beans.BeanUtil;

import static java.lang.System.getLogger;


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

    private static final Logger logger = getLogger(JaywayJsonPathBinder.class.getName());

    private final String source;

    JaywayJsonPathBinder(String source) {
        this.source = source;
    }

    // Boolean
    protected final EachBinder booleanEachBinder = new BooleanEachBinder() {
        public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            String jsonPath = Element.Util.getValue(field);
            String text = JsonPath.parse(source).read(jsonPath);
            context.setValue(Boolean.parseBoolean(text));
        }
    };

    // Integer
    protected final EachBinder integerEachBinder = new IntegerEachBinder() {
        public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            String jsonPath = Element.Util.getValue(field);
            String text = JsonPath.parse(source).read(jsonPath);
            context.setValue(Integer.parseInt(text));
        }
    };

    // Short
    protected final EachBinder shortEachBinder = new ShortEachBinder() {
        public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            String jsonPath = Element.Util.getValue(field);
            String text = JsonPath.parse(source).read(jsonPath);
            context.setValue(Short.parseShort(text));
        }
    };

    // Byte
    protected final EachBinder byteEachBinder = new ByteEachBinder() {
        public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            String jsonPath = Element.Util.getValue(field);
            String text = JsonPath.parse(source).read(jsonPath);
            context.setValue(Byte.parseByte(text));
        }
    };

    // Long
    protected final EachBinder longEachBinder = new LongEachBinder() {
        public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            String jsonPath = Element.Util.getValue(field);
            String text = JsonPath.parse(source).read(jsonPath);
            context.setValue(Long.parseLong(text));
        }
    };

    // Float
    protected final EachBinder floatEachBinder = new FloatEachBinder() {
        public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            String jsonPath = Element.Util.getValue(field);
            String text = JsonPath.parse(source).read(jsonPath);
            context.setValue(Float.parseFloat(text));
        }
    };

    // Double
    protected final EachBinder doubleEachBinder = new DoubleEachBinder() {
        public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            String jsonPath = Element.Util.getValue(field);
            String text = JsonPath.parse(source).read(jsonPath);
            context.setValue(Double.parseDouble(text));
        }
    };

    // Character
    protected final EachBinder characterEachBinder = new CharacterEachBinder() {
        public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            String jsonPath = Element.Util.getValue(field);
            String text = JsonPath.parse(source).read(jsonPath);
            context.setValue((char) Integer.parseInt(text));
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

            // object array
            // TODO exclude primitives and base classes
            Serdes annotation = fieldElementClass.getAnnotation(Serdes.class);
            if (annotation == null) {
                throw new UnsupportedOperationException("use @Bound: " + fieldElementClass.getTypeName() + "] at " + field.getName() + " (" + context.getSequence() + ")");
            }

            try {
                String jsonPath = Element.Util.getValue(field);
                List<String> list = JsonPath.read(source, jsonPath);
if (list.isEmpty()) {
 logger.log(Level.WARNING, "no list: " + jsonPath);
}
                List<Object> result = new ArrayList<>();
                for (int i = 0; i < eachContext.size; i++) {
                    Object fieldBean = fieldElementClass.getDeclaredConstructor().newInstance();
                    eachContext.deserialize(fieldBean);
                    result.add(fieldBean);
                }
                context.setValue(result.toArray());
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    // String
    protected final EachBinder stringEachBinder = new StringEachBinder() {
        public void bind(EachContext context, Object dstBean, Field field) throws IOException {
            String jsonPath = Element.Util.getValue(field);
            String text = JsonPath.parse(source).read(jsonPath);
            context.setValue(text);
logger.log(Level.TRACE, "jsonPath: " + jsonPath + ", string: " + text);
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
