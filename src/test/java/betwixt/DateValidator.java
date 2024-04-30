/*
 * Copyright (c) 2003 by Naohide Sano, All Rights Reserved.
 *
 * Programmed by Naohide Sano
 */

package betwixt;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


/**
 * DateValidator.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 031019 nsano initial version <br>
 */
public class DateValidator extends Validator {

    /** collection of format strings using with {@link DateFormat} */
    private Set<String> formats = new HashSet<>();

    /**
     * @param format using with {@link DateFormat}
     */
    public void addFormat(String format) {
        this.formats.add(format);
    }

    /** */
    public Collection<String> getFormats() {
        return formats;
    }

    /** */
    public boolean validate(String value) {

        Calendar calendar = Calendar.getInstance();
        calendar.setLenient(false);

        try {
            for (String format : formats) {
                DateFormat sdf = new SimpleDateFormat(format);
                sdf.format(calendar.getTime());
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
