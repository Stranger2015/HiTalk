package org.ltc.hitalk.term.io;

import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.entities.PropertyOwner;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 *
 */
public class HtTermWriter extends PropertyOwner implements PropertyChangeListener {
    private final BufferedWriter out;
    private final Path path;
    protected HiTalkOutputStream output;

    /**
     * @param out
     * @param path
     * @throws IOException
     */
//    public HtTermWriter ( BufferedWriter output, Path path, String encoding, StandardOpenOption... options ) throws IOException {
//        super(output, path, encoding, options);
//    }
    public HtTermWriter(BufferedWriter out, Path path, HtProperty[] props, HtMethodDef[] methods, Map<String, HtProperty> map, Map<String, HtMethodDef> mmap) {
        super(props, methods, map, mmap);
        this.out = out;
        this.path = path;
    }

    /**
     * This method gets called when a bound property is changed.
     *
     * @param evt A PropertyChangeEvent object describing the event source
     *            and the property that has changed.
     */
    public void propertyChange(PropertyChangeEvent evt) {

    }
}
