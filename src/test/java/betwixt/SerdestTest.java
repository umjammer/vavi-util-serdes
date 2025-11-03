/*
 * Copyright (c) 2003 by Naohide Sano, All Rights Reserved.
 *
 * Programmed by Naohide Sano
 */

package betwixt;

import java.io.File;

import org.apache.commons.betwixt.io.BeanReader;

import vavi.util.Debug;
import vavi.util.serdes.Serdes;


/**
 * Serdes Test.
 *
 * @author <a href=mailto:"umjammer@gmail.com">Naohide Sano</a>(nsano)
 * @version 0.00 031216 nsano initial version <br>
 */
public class SerdestTest {

    /**
     *
     */
    public static void main(String[] args) throws Exception {

        BeanReader reader = new BeanReader();
        reader.registerBeanClass(Serdes.class);

        Serdes bean = (Serdes) reader.parse(new File(args[0]));

Debug.println("bean: " + bean);
    }
}
