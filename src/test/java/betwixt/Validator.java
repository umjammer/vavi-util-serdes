/*
 * Copyright (c) 2003 by Naohide Sano, All Rights Reserved.
 *
 * Programmed by Naohide Sano
 */

package betwixt;

/**
 * Validator.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 031019 nsano initial version <br>
 */
public abstract class Validator {

    /** */
    private int sequence;

    /** */
    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    /** */
    public int getSequence() {
        return sequence;
    }

    /** */
//    public abstract boolean validate(String value);
}

/* */
