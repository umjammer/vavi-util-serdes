/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.io.IOException;
import java.lang.reflect.Field;

import org.jsoup.nodes.Document;


/**
 * JSoupBinder.
 * <ul>
 * <li> {@link Element#value()} ... css selector
 * <li> fieldValue ... default value
 * </ul>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/11/07 umjammer initial version <br>
 */
public class JSoupBinder implements Binder {

    Document document;

    JSoupBinder(Document document) {
        this.document = document;
    }

    // Boolean
    protected EachBinder booleanEachBinder = new BooleanEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String selector = Element.Util.getValue(field);
            context.setValue(document.select(selector));
        }
    };

    // Integer
    protected EachBinder integerEachBinder = new IntegerEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String selector = Element.Util.getValue(field);
            context.setValue(document.select(selector));
        }
    };

    // Short
    protected EachBinder shortEachBinder = new ShortEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String selector = Element.Util.getValue(field);
            context.setValue(document.select(selector));
        }
    };

    // Byte
    protected EachBinder byteEachBinder = new ByteEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String selector = Element.Util.getValue(field);
            context.setValue(document.select(selector));
        }
    };

    // Long
    protected EachBinder longEachBinder = new LongEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String selector = Element.Util.getValue(field);
            context.setValue(document.select(selector));
        }
    };

    // Float
    protected EachBinder floatEachBinder = new FloatEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String selector = Element.Util.getValue(field);
            context.setValue(document.select(selector));
        }
    };

    // Double
    protected EachBinder doubleEachBinder = new DoubleEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String selector = Element.Util.getValue(field);
            context.setValue(document.select(selector));
        }
    };

    // Character
    protected EachBinder characterEachBinder = new CharacterEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String selector = Element.Util.getValue(field);
            context.setValue(document.select(selector));
        }
    };

    // Array
    protected EachBinder arrayEachBinder = new ArrayEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            Class<?> fieldElementClass = field.getType().getComponentType();
            if (fieldElementClass.equals(Byte.TYPE)) {
                // byte array
                String selector = Element.Util.getValue(field);
                context.setValue(document.select(selector));
            } else {
                throw new IllegalArgumentException("unsupported array: " + fieldElementClass.getName());
            }
        }
    };

    // String
    protected EachBinder stringEachBinder = new StringEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String selector = Element.Util.getValue(field);
            context.setValue(document.select(selector));
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
