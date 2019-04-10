/*
 * Copyright (c) 2003 by Naohide Sano, All Rights Reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.File;

import org.apache.commons.betwixt.io.BeanReader;

import vavi.util.injection.Injector;


/**
 * Injector Test.
 *
 * @author <a href=mailto:"umjammer@gmail.com">Naohide Sano</a>(nsano)
 * @version 0.00 031216 vavi initial version <br>
 */
public class t110_2 {

    /**
     *
     */
    public static void main(String[] args) throws Exception {

        BeanReader reader = new BeanReader();
        reader.registerBeanClass(Injector.class);

        Injector bean = (Injector) reader.parse(new File(args[0]));

        System.err.println("bean: " + bean);
    }
}

/* */
