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
 * JSoupCssSelectorBinder.
 * <ul>
 * <li> {@link Element#value()} ... css selector
 * <li> fieldValue ... default value
 * </ul>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/11/07 umjammer initial version <br>
 */
public class JSoupCssSelectorBinder implements Binder {

    final Document document;

    JSoupCssSelectorBinder(Document document) {
        this.document = document;
    }

    // Boolean
    protected final EachBinder booleanEachBinder = new BooleanEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String selector = Element.Util.getValue(field);
            context.setValue(document.select(selector));
        }
    };

    // Integer
    protected final EachBinder integerEachBinder = new IntegerEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String selector = Element.Util.getValue(field);
            context.setValue(document.select(selector));
        }
    };

    // Short
    protected final EachBinder shortEachBinder = new ShortEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String selector = Element.Util.getValue(field);
            context.setValue(document.select(selector));
        }
    };

    // Byte
    protected final EachBinder byteEachBinder = new ByteEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String selector = Element.Util.getValue(field);
            context.setValue(document.select(selector));
        }
    };

    // Long
    protected final EachBinder longEachBinder = new LongEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String selector = Element.Util.getValue(field);
            context.setValue(document.select(selector));
        }
    };

    // Float
    protected final EachBinder floatEachBinder = new FloatEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String selector = Element.Util.getValue(field);
            context.setValue(document.select(selector));
        }
    };

    // Double
    protected final EachBinder doubleEachBinder = new DoubleEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String selector = Element.Util.getValue(field);
            context.setValue(document.select(selector));
        }
    };

    // Character
    protected final EachBinder characterEachBinder = new CharacterEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String selector = Element.Util.getValue(field);
            context.setValue(document.select(selector));
        }
    };

    // Array
    protected final EachBinder arrayEachBinder = new ArrayEachBinder() {
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
    protected final EachBinder stringEachBinder = new StringEachBinder() {
        public void bind(EachContext context, Object destBean, Field field) throws IOException {
            String selector = Element.Util.getValue(field);
            context.setValue(document.select(selector));
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
