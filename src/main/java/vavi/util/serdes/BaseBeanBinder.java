/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.cache.annotation.CacheResult;

import vavi.util.serdes.BeanBinder.IOSource;
import vavi.util.serdes.Binder.EachContext;

import static java.lang.System.getLogger;


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

    private static final Logger logger = getLogger(BaseBeanBinder.class.getName());

    /** called multiple times, should be optimized */
    protected abstract Binder getDefaultBinder();

    /** */
    protected abstract Context getContext(IOSource in, List<Field> fields, Object bean, Object parent);

    /** */
    protected abstract EachContext getEachContext(int sequence, Boolean isBigEndian, Field field, Context context);

    @Override
    public Object deserialize(Object io, Object dstBean) throws IOException {
        T in = getIOSource(io, serdes.isBigEndian(dstBean.getClass()));
        deserialize0(in, dstBean, null);
        return dstBean;
    }

    /**
     * <ol>
     * <li>retrieve fields which has {@link Element}
     * <li>check condition
     * <li>do injection
     * <li>do validation
     * </ol>
     * @param parent nullable
     */
    protected void deserialize0(T in, Object dstBean, Object parent) throws IOException {
        serdes.getAnnotation(dstBean.getClass());

        // list up fields
        List<Field> elementFields = serdes.getElementFields(dstBean.getClass());

        // injection
        Context context = getContext(in, elementFields, dstBean, parent);

        for (Field field : elementFields) {

            int sequence = element.getSequence(field);

            // each endian
            Boolean bigEndian = element.isBigEndian(field);
            EachContext eachContext = getEachContext(sequence, bigEndian, field, context);

            // condition
            String condition = element.getCondition(field);
            if (!condition.isEmpty()) {
                if (!eachContext.condition(condition)) {
logger.log(Level.DEBUG, "condition check is false");
                    continue;
                }
            }

            // each injection
            Binder binder = getDefaultBinder();
            if (Bound.Util.isBound(field)) {
                binder = Bound.Util.getBinder(field);
            }
logger.log(Level.TRACE, "binder: " + binder.getClass().getName());
            binder.bind(eachContext, dstBean, field);
logger.log(Level.DEBUG, field.getName() + ": " + field.getType() + ", " + eachContext);
            eachContext.settleValues();

            // validation
            String validation = element.getValidation(field);
            if (!validation.isEmpty()) {
                eachContext.validate(validation);
            }
        }
    }

    @Override
    public Object serialize(Object srcBean, Object io) throws IOException {
        T out = getIOSource(io, serdes.isBigEndian(srcBean.getClass()));
        serialize0(srcBean, out, null);
        return io;
    }

    /**
     * <ol>
     * <li>retrieve fields which has {@link Element}
     * <li>check condition
     * <li>do extraction
     * </ol>
     * @param parent nullable
     */
    protected void serialize0(Object srcBean, T out, Object parent) throws IOException {
        serdes.getAnnotation(srcBean.getClass());

        // list up fields
        List<Field> elementFields = serdes.getElementFields(srcBean.getClass());

        // extraction
        Context context = getContext(out, elementFields, srcBean, parent);

        for (Field field : elementFields) {

            int sequence = element.getSequence(field);

            // each endian
            Boolean bigEndian = element.isBigEndian(field);
            EachContext eachContext = getEachContext(sequence, bigEndian, field, context);

            // condition
            String condition = element.getCondition(field);
            if (!condition.isEmpty()) {
                if (!eachContext.condition(condition)) {
                    logger.log(Level.DEBUG, "condition check is false");
                    continue;
                }
            }

            // validation
            String validation = element.getValidation(field);
            if (!validation.isEmpty()) {
                eachContext.validate(validation);
            }

            eachContext.settleValues(); // TODO set all fields before loop?

            // each extraction
            Binder binder = getDefaultBinder();
            if (Bound.Util.isBound(field)) {
                binder = Bound.Util.getBinder(field);
            }
logger.log(Level.TRACE, "binder: " + binder.getClass().getName());
            binder.bind(srcBean, field, eachContext);
logger.log(Level.DEBUG, field.getName() + ": " + field.getType() + ", " + eachContext);
        }
    }

    public static class SequenceValidator {

        /** @throws IllegalArgumentException validation failed */
        public void validateSequences(Class<?> clazz) {
            String r = getValidateSequencesState(clazz);
            if (!r.isEmpty()) throw new IllegalArgumentException(r);
        }

        @CacheResult(cacheName = "beanBinder_validateSequences")
        public String getValidateSequencesState(Class<?> clazz) {
            List<Field> fields = serdes.getElementFields(clazz);
            Set<Integer> numbers = new HashSet<>();
            for (Field field : fields) {
                int sequence = element.getSequence(field);
                if (sequence < 1) {
                    return "sequence should be > 0: " + field.getName() + ", " + sequence;
                }
                if (numbers.contains(sequence)) {
                    return "duplicate sequence: " + field.getName() + ", " + sequence;
                } else {
                    numbers.add(sequence);
                }
            }
            return "";
        }
    }
}
