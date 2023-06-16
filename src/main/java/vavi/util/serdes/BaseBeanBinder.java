/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import vavi.util.Debug;
import vavi.util.serdes.BeanBinder.IOSource;
import vavi.util.serdes.Binder.EachContext;


/**
 * BaseBeanBinder.
 * <p>
 * {@link #getIOSource(Object...)} argument args index 1 is boolean true: big endian
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/02/26 umjammer initial version <br>
 */
public abstract class BaseBeanBinder<T extends IOSource> implements BeanBinder<T> {

    /** called multiple times, should be optimized */
    protected abstract Binder getDefaultBinder();

    /** */
    protected abstract Context getContext(IOSource in, List<Field> fields, Object bean);

    /** */
    protected abstract EachContext getEachContext(int sequence, Boolean isBigEndian, Field field, Context context);

    @Override
    public Object deserialize(Object io, Object destBean) throws IOException {
        T in = getIOSource(io, Serdes.Util.isBigEndian(destBean));
        deserialize0(in, destBean);
        return destBean;
    }

    /**
     * <ol>
     * <li>retrieve fields which has {@link Element}
     * <li>check condition
     * <li>do injection
     * <li>do validation
     * </ol>
     */
    protected void deserialize0(T in, Object destBean) throws IOException {
        Serdes.Util.getAnnotation(destBean);

        // list up fields
        List<Field> elementFields = Serdes.Util.getElementFields(destBean);

        // injection
        Context context = getContext(in, elementFields, destBean);

        for (Field field : elementFields) {

            int sequence = Element.Util.getSequence(field);

            // each endian
            Boolean bigEndian = Element.Util.isBigEndian(field);
            EachContext eachContext = getEachContext(sequence, bigEndian, field, context);

            // condition
            String condition = Element.Util.getCondition(field);
            if (!condition.isEmpty()) {
                if (!eachContext.condition(condition)) {
Debug.println(Level.FINE, "condition check is false");
                    continue;
                }
            }

            // each injection
            Binder binder = getDefaultBinder();
            if (Bound.Util.isBound(field)) {
                binder = Bound.Util.getBinder(field);
            }
Debug.println(Level.FINER, "binder: " + binder.getClass().getName());
            binder.bind(eachContext, destBean, field);
Debug.println(Level.FINE, field.getName() + ": " + field.getType() + ", " + eachContext);
            eachContext.settleValues();

            // validation
            String validation = Element.Util.getValidation(field);
            if (!validation.isEmpty()) {
                eachContext.validate(validation);
            }
        }
    }

    @Override
    public Object serialize(Object destBean, Object io) throws IOException {
        throw new UnsupportedOperationException("not implemented yet");
    }

    /** @throws IllegalArgumentException validation failed */
    protected static void validateSequences(List<Field> fields) {
        Set<Integer> numbers = new HashSet<>();
        for (Field field : fields) {
            int sequence = Element.Util.getSequence(field);
            if (sequence < 1) {
                throw new IllegalArgumentException("sequence should be > 0: " + field.getName() + ", " + sequence);
            }
            if (numbers.contains(sequence)) {
                throw new IllegalArgumentException("duplicate sequence: " + field.getName() + ", " + sequence);
            } else {
                numbers.add(sequence);
            }
        }
    }
}

/* */
