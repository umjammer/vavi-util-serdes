/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.io.IOException;


/**
 * Binders.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/02/26 umjammer initial version <br>
 */
public interface BeanBinder<IO extends BeanBinder.IOSource> {

    /** holds io complexity like endian */
    interface IOSource {
    }

    /** holds conversion context */
    interface Context {
    }

    /**
     * @throws IllegalArgumentException args is not supported
     */
    IO getIOSource(Object... args) throws IOException;

    /**
     * @return the same as destBean
     * @throws IllegalArgumentException in type is not supported
     */
    Object deserialize(Object in, Object destBean) throws IOException;

    /**
     * @return the same as out
     * @throws IllegalArgumentException out type is not supported
     */
    Object serialize(Object destBean, Object out) throws IOException;
}

/* */
