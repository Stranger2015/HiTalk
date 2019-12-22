package org.ltc.hitalk.term.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 *
 */
public class HtTermWriter {
    protected HiTalkOutputStream output;

    /**
     * @param output
     * @param path
     * @param encoding
     * @param options
     * @throws IOException
     */
    public HtTermWriter ( BufferedWriter output, Path path, String encoding, StandardOpenOption... options ) throws IOException {
//        super(output, path, encoding, options);
    }
}
