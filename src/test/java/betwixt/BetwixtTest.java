/*
 * Copyright (c) 2003 by Naohide Sano, All Rights Reserved.
 *
 * Programmed by Naohide Sano
 */

package betwixt;

import java.io.File;

import org.apache.commons.betwixt.io.BeanReader;
import vavi.util.Debug;


/**
 * Binary Binding test.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 031215 nsano initial version <br>
 */
public class BetwixtTest {

    /**
     *
     */
    public static void main(String[] args) throws Exception {

        BeanReader reader = new BeanReader();
        reader.registerBeanClass(CsvFormat.class);
        CsvFormat bean = (CsvFormat) reader.parse(new File(args[0]));

Debug.println("bean: " + bean);
Debug.println("count: " + bean.getCount());
Debug.println("validators: " + bean.getValidators().size());
        for (Validator validator : bean.getValidators()) {
Debug.println("validator: " + validator);
        }
    }
}
