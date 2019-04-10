/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.test.box;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import vavi.util.injection.Injector;


/**
 * InjectorTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/04/07 umjammer initial version <br>
 */
class InjectorTest {

    static final String file = "/Users/nsano/Music/0/11 - Blockade.m4a";

    @Test
    void test() throws Exception {
        InputStream is = new FileInputStream(Paths.get(file).toFile());
        Box box = new Box();
        Injector.Util.inject(is, box);
    }
}

/* */
