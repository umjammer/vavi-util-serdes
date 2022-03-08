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

    /** */
    interface IOSource {
    }

    /** */
    interface Context {
    }

    /**
     * @throws IllegalArgumentException
     */
    IO getIOSource(Object... args) throws IOException;

    void deserialize(Object in, Object destBean) throws IOException;

    void serialize(Object destBean, Object out) throws IOException;
}

/* */
