package org.ltc.hitalk.term.io;

import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.entities.IProperty;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.IdentifiedTerm;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Set;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.REPRESENTATION_ERROR;
import static org.ltc.hitalk.core.BaseApp.getAppContext;

/**
 *
 */
public class HtTermWriter extends HtTermIO {

    /**
     * @param path
     * @param stream
     * @throws FileNotFoundException
     */
    public HtTermWriter(Path path, HiTalkOutputStream stream) throws FileNotFoundException {
        super(path, stream);
        stream.setOutputStream(new FileOutputStream(path.toFile()));

    }

    /**
     * @param sb
     */
    public void toString0(StringBuilder sb) {

    }

//    public HtTermWriter(HtProperty[] properties) {
//        super(properties);
//    }

    abstract static class WriteMethod {
        static class Write extends WriteMethod {

        }

        static class WriteQ extends WriteMethod {

        }

        static class WriteCanonical extends WriteMethod {

        }

        static class Print extends WriteMethod {

        }
    }

    /**
     * @param t
     * @param writeMethod
     * @return
     */
    int write(ITerm t, WriteMethod writeMethod) {
        return 0;
    }

    /**
     * @param stream
     * @param term
     * @throws Exception
     */
    void write(HiTalkOutputStream stream, ITerm term) throws Exception {

    }

    /**
     * @param stream
     * @param term
     * @throws Exception
     */
    void write_canonical(HiTalkOutputStream stream, ITerm term) throws Exception {
        if (term.isFunctor()) {
            if (!term.isHiLog()) {
                IFunctor f = (IFunctor) term;
                writeFunctor(stream, f, !f.isHiLog());
            } else {
                IFunctor f = (IFunctor) ((IFunctor) term).getArgs().getHead(0);
                writeFunctor(stream, f, f.isHiLog());
            }

        }
    }

    private void writeFunctor(HiTalkOutputStream stream, IFunctor f, boolean isHilog) throws Exception {
        if (isHilog) {
            final String name = interner.getFunctorName(f.getName());
            int arity = interner.getFunctorArity(f.getName());
            if (f.getArity() != arity) {
                throw new ExecutionError(REPRESENTATION_ERROR, interner.getDeinternedFunctorName(f.getName()));
            }
            final Set<IdentifiedTerm> ops = getAppContext().getOpTable().getOperators(name);
            if (ops.isEmpty()) {
                //working withn operator
            }
            if (quotesNeeded(name)) {
                stream.writeChar('\'');
                stream.writeChars(name);
                stream.writeChar('\'');
            } else {
                stream.writeChars(name);
            }

            stream.writeChar('(');
            writeSequence(stream, f);
            stream.writeChar(')');
        }
    }

    private boolean quotesNeeded(String name) {
        if (priorityIsBeDropped(name) || !isAlphaNumeric(name) && !isSymbolic(name)) {
            return true;
        }

        return false;
    }

    private boolean isSymbolic(String name) {
        return false;
    }

    private boolean isAlphaNumeric(String name) {
        return false;
    }

    private boolean priorityIsBeDropped(String name) {
        return false;//todo
    }

    private void writeSequence(HiTalkOutputStream stream, IFunctor functor) throws Exception {
        if (!functor.isHiLog()) {
            final ListTerm args = functor.getArgs();
            int ofs = functor.getHeadsOffset();
            for (int i = ofs, numHeads = args.getHeads().size() - ofs; i < numHeads; i++) {
                write_canonical(stream, args.getHead(i));
            }
            if (args.getTail() != ListTerm.NIL) {
                stream.writeChar('|');
                write_canonical(stream, args.getTail());
            }
        }
    }

    void writeq(HiTalkOutputStream stream, ITerm term) throws Exception {

    }

    void write_term(HiTalkOutputStream stream, ITerm term, IProperty... peops) throws Exception {

    }
}
