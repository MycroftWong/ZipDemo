package com.mycroft.zip.util;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by Mycroft_Wong on 2015/12/30.
 */
public final class CloseUtil {

    private CloseUtil() {
        /* cannot be instantiated */
        throw new UnsupportedOperationException("cannot be instantiated");
    }

    public static void quietClose(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            // ignore
        }
    }
}
