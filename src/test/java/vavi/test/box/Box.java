/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.test.box;

import vavi.util.injection.Element;
import vavi.util.injection.Injector;


/**
 * Box.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/04/05 umjammer initial version <br>
 */
@Injector
public class Box {

    /** */
    @Element(sequence = 0, value = "unsigned int")
    long offset;

    /** */
    @Element(sequence = 1, value = "4")
    byte[] id = new byte[4];

    /** */
    @Element(sequence = 2, value = "$0 - sizeof($0) - len($1)")
    byte[] data;
}

/* */
