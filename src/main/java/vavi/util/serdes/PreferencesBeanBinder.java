/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util.serdes;

import java.io.IOException;
import java.io.InputStream;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

import vavi.util.serdes.PreferencesBeanBinder.NullIOSource;


/**
 * PreferencesBeanBinder.
 *
 * TODO needed? why don't use plain preferences?
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/02/26 umjammer initial version <br>
 */
public class PreferencesBeanBinder extends SimpleBeanBinder<NullIOSource> {

    Preferences prefs;

    static class NullIOSource implements BeanBinder.IOSource {
    }

    @Override
    public NullIOSource getIOSource(Object... args) throws IOException {
        NullIOSource io = new NullIOSource();
        if (args[0] instanceof String) {
            prefs = Preferences.userRoot().node((String) args[0]);
        } else if (args[0] instanceof InputStream) {
            try {
                // TODO how to know node added
                Preferences.importPreferences((InputStream) args[0]);
            } catch (InvalidPreferencesFormatException e) {
                throw new IOException(e);
            }
        } else if (args[0] instanceof Preferences) {
            prefs = (Preferences) args[0];
        } else {
            throw new IllegalArgumentException("unsupported class: " + args[0].getClass().getName());
        }
        return io;
    }

    @Override
    protected Binder getDefaultBinder() {
        return new PreferencesBinder(prefs);
    }
}
