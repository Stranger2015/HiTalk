package org.ltc.hitalk.term.io;

import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.core.BaseApp;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.entities.PropertyOwner;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.IFunctor;

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
    protected IVafInterner interner = BaseApp.getAppContext().getInterner();

    /**
     * @param out
     * @param path
     * @throws IOException
     */
//    public HtTermWriter ( BufferedWriter output, Path path, String encoding, StandardOpenOption... options ) throws IOException {
//        super(output, path, encoding, options);
//    }
    public HtTermWriter(BufferedWriter out,
                        Path path,
                        HtProperty[] props,
                        HtMethodDef[] methods,
                        Map<String, HtProperty> map,
                        Map<String, HtMethodDef> mmap) {
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

    void write(HiTalkOutputStream stream, ITerm term) throws Exception {
        if (term.isAtom()) {
            IFunctor f = (IFunctor) term;
            final String name = interner.getFunctorName(f.getName());
            int arity = interner.getFunctorArity(f.getName());
            if (f.getArity() != arity) {
                throw new ExecutionError(ExecutionError.Kind.REPRESENTATION_ERROR,
                        interner.getDeinternedFunctorName(f.getName()));
            }
            
        }
    }
}
