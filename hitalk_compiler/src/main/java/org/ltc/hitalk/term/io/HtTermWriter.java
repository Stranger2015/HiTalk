package org.ltc.hitalk.term.io;

import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.core.BaseApp;
import org.ltc.hitalk.entities.IProperty;
import org.ltc.hitalk.term.*;
import org.ltc.hitalk.wam.compiler.HtFunctor;
import org.ltc.hitalk.wam.compiler.HtFunctorName;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.REPRESENTATION_ERROR;
import static org.ltc.hitalk.core.BaseApp.getAppContext;

/**
 *
 */
public class HtTermWriter extends HtTermIO {
    IOperatorTable optable = BaseApp.appContext.getOpTable();

    /**
     * @param path
     * @param stream
     * @throws FileNotFoundException
     */
    public HtTermWriter(Path path, HiTalkOutputStream stream) throws FileNotFoundException {
        super(path, stream);
        stream.setOutputStream(new FileOutputStream(path.toFile()));

    }

    public HtTermWriter(ITerm term) {
        super(term);
    }


    /**
     * @param sb
     * @param t
     * @return
     */
    public String writeTerm(StringBuilder sb, ITerm t) {
        if (t.isFunctor()) {
            writeFunctor(sb, (HtFunctor) t);
        } else if (t.isNumber()) {
            if (t instanceof IntTerm) {
                sb.append(((IntTerm) t).getInt());
            } else if (t instanceof FloatTerm) {
                sb.append(Float.intBitsToFloat(((FloatTerm) t).getImage()));
            }
        } else if (t.isVar()) {
            sb.append(interner.getVariableName((HtVariable) t));
        } else if (t.isList()) {
            sb.append("[ ");
            writeSeq(sb, (ListTerm) t);
            sb.append(" ]");
        }
        return sb.toString();
    }

    private void writeFunctor(StringBuilder sb, HtFunctor functor) {
        final HtFunctorName fn = interner.getDeinternedFunctorName(t.getName());
        final String name = fn.getName();
        int arity = fn.getArity();

        final Set<OpSymbolFunctor> ops = optable.getOperators(name, arity);//writeName(sb, functor);
        if (ops.isEmpty()) {
            sb.append("( ");
            writeSeq(sb, functor.getArgs());
            sb.append(" )");
        } else { ///ops.size == 1
            for (OpSymbolFunctor op : ops) {
                if (op.isPrefix()) {
                    sb.append(op.getArgument(0).);//fixme
                    writeTerm(sb, op.getArgument(0));
                } else if (op.isInfix()) {//todo

                }
            }
        }
    }

    private void writeName(StringBuilder sb, String name) {
        if (quoteRequired(name)) {
            sb.append('\'');
            sb.append(name);
            sb.append('\'');
        } else {
            sb.append(name);
        }
//        return optable.getOperators(name, arity);
    }

    private boolean quoteRequired(String name) {
        return !isAlphaNumeric(name) && !isSymbolic(name);
    }

    protected void writeSeq(StringBuilder sb, ListTerm t) {
        writeHeads(sb, t);
        writeTail(sb, t);
    }

    private void writeTail(StringBuilder sb, ListTerm t) {
        if (t.getTail() != ListTerm.NIL) {
            sb.append("| ");
            writeTerm(sb, t.getTail());
        }
    }

    private void writeHeads(StringBuilder sb, ListTerm t) {
        final List<ITerm> heads = t.getHeads();
        for (int i = 0; i < heads.size(); i++) {
            writeTerm(sb, heads.get(i));
            sb.append(i + 1 < heads.size() ? ", " : " ");
        }

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
            final Set<OpSymbolFunctor> ops = getAppContext().getOpTable().getOperators(name);
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
        return priorityIsBeDropped(name) || !isAlphaNumeric(name) && !isSymbolic(name);
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
