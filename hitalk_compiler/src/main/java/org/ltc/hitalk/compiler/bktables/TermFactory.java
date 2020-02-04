package org.ltc.hitalk.compiler.bktables;

import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.NumberTerm;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.entities.HtEntityIdentifier;
import org.ltc.hitalk.entities.HtEntityKind;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.entities.PropertyOwner;
import org.ltc.hitalk.entities.context.Context;
import org.ltc.hitalk.entities.context.LoadContext;
import org.ltc.hitalk.parser.HiLogFunctor;
import org.ltc.hitalk.parser.PlToken.TokenKind;
import org.ltc.hitalk.term.*;
import org.ltc.hitalk.term.ListTerm.Kind;
import org.ltc.hitalk.wam.compiler.HtFunctor;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;
import static org.ltc.hitalk.core.BaseApp.getAppContext;
import static org.ltc.hitalk.parser.HiLogParser.HILOG_APPLY_INT;
import static org.ltc.hitalk.parser.PlPrologParser.ANONYMOUS;
import static org.ltc.hitalk.term.ListTerm.NIL;

/**
 *
 */
public class TermFactory implements ITermFactory {
    private IVafInterner interner;

    /**
     * @param interner
     */
    public TermFactory(IVafInterner interner) {
        this.interner = interner;
    }

    /**
     * @param value
     * @return
     */
    @Override
    public IFunctor newAtom(String value) {
        return newAtom(interner.internFunctorName(value, 0));
    }

    /**
     * ひとつ以上の引数を持つ関数子を作成します。
     *
     * @param hilogApply
     * @param value
     * @param args
     */
    public IFunctor newFunctor(int hilogApply, String value, ListTerm args) {
        return null;
    }

    public IFunctor newFunctor(String name, ListTerm args) {
        return newFunctor(args);
    }

    /**
     * @param value
     * @return
     */
    @Override
    public IFunctor newAtom(int value) {
        return new HtFunctor(value);
    }

    /**
     * @param name
     * @return
     */
    @Override
    public IFunctor newHiLogFunctor(String name, ListTerm listTerm) {
        int arity = listTerm.getHeads().size() - 1;
        return newHiLogFunctor(new IntTerm(interner.internFunctorName(name, arity)), listTerm);
    }

    /**
     * @param args
     * @return
     */
    @Override
    public IFunctor newFunctor(ListTerm args) {
        return new HtFunctor(args);
    }

    /**
     * @param vns
     * @param fns
     * @return
     */
    public IVafInterner getInterner(String vns, String fns) {
        if (interner == null) {
            final String[] ns = new String[]{vns, fns};
            interner = getAppContext().getInterner(ns);
        }
        return interner;
    }

    /**
     * @param value
     * @return
     */
    @Override
    public HtVariable newVariable(String value) {
        return new HtVariable(
                interner.internVariableName(value),
                null,
                value.equals(ANONYMOUS)
        );
    }

    public IFunctor newAtom(TokenKind lDelim, TokenKind rDelim) {
        String s = String.format("%s%s", lDelim.getImage(), rDelim.getImage());
        return new HtFunctor(interner.internFunctorName(s, 0), NIL);
    }


    public ListTerm newListTerm(ITerm... headTail) {
//        this.kind = kind;
//        this.headTail = headTail;
//        if (headTail.length ==1){ //[|VarOrList] []

        return new ListTerm(Arrays.asList(headTail));
    }

    public String createFlag(String scratch_directory, Path scratchDir) {//fixme
        return PropertyOwner.createProperty(scratch_directory, scratchDir.toString(), "").getV();
    }

    public IFunctor createMostGeneral(IFunctor functor) throws Exception {
        return newFunctor(functor.getName(), functor.getArity());
    }

    /**
     * @param name
     * @param arity
     * @return
     */
    public IFunctor newFunctor(int name, int arity) {
        return new HtFunctor(name, new ListTerm(arity));
    }

//    @Override
//    public IFunctor newFunctor(ITerm name, ListTerm args) {
//        return newHiLogFunctor(args.addHeads(name, new IntTerm(HILOG_APPLY_INT)));
//    }

    @Override
    public IFunctor newHiLogFunctor(ITerm name, ListTerm args) {
        final List<ITerm> heads = new ArrayList<>(args.getHeads());
        heads.add(name);
        heads.add(new IntTerm(HILOG_APPLY_INT));

        return new HiLogFunctor(new ListTerm(heads));
    }

    public IFunctor newHiLogFunctor(List<ITerm> namesHeads) {
        return new HiLogFunctor(new ListTerm(new ArrayList<>(namesHeads)));
    }

    public IFunctor newFunctor(IFunctor term, ListTerm args) {
        return newFunctor(args.addHead(term));
    }

    // commodity methods to parse numbers

    IntTerm parseInteger(String s) {
        long num = Long.parseLong(s);
        return newAtomic(num > MIN_VALUE && num < MAX_VALUE ? (int) num : Math.toIntExact(num));
    }

    FloatTerm parseFloat(String s) {
        return newAtomic(Double.parseDouble(s));
    }

    public NumberTerm createNumber(String s) {
        try {
            return parseInteger(s);
        } catch (Exception e) {
            return parseFloat(s);
        }
    }

    @Override
    public HtNonVar createNonvar(String value) {
        return null;//atomic or compound
    }

    public HtVariable createVariable(String vname) {
        return new HtVariable(interner.internVariableName(vname), null, false);
    }

    /**
     * @param s
     * @return
     */
    @Override
    public IFunctor createAtom(String s) {
        int ffn = interner.internFunctorName(s, 0);
        return new HtFunctor(ffn, NIL);
    }

    /**
     * @param flagName
     * @param flagValue
     * @return
     */
    @Override
    public HtProperty createFlag(String flagName, String flagValue) {
        return createProperty(flagName, flagValue);
    }

    /**
     * @param kind
     * @param name
     * @param args
     * @return
     */
    public HtEntityIdentifier createIdentifier(HtEntityKind kind, String name, ITerm... args) {
        return new HtEntityIdentifier(interner.internFunctorName(name, 0), new ListTerm(Arrays.asList(args)), kind);
    }

    /**
     * @param name
     * @param args
     * @return
     */
    public HtProperty createFlag(String name, ITerm... args) {
        return PropertyOwner.createProperty(name, "");//fixme
    }

//    @Override
//    public IFunctor newFunctor(int hilogApply, ITerm name, ListTerm args) {
//        ITerm[] headTail = args.getHeads();
//        ITerm[] nameHeadTail = new ITerm[headTail.length + 1];
//        System.arraycopy(headTail, 0, nameHeadTail, 1, headTail.length);
//        nameHeadTail[0] = name;
//        return new HtFunctor(hilogApply, new ListTerm(nameHeadTail));
//    }

    @Override
    public IntTerm newAtomic(int i) {
        return new IntTerm(i);
    }

    @Override
    public FloatTerm newAtomic(double f) {
        return new FloatTerm((float) f);
    }

    /**
     * @param kind
     * @param headTail
     * @return
     */
    public ListTerm newListTerm(Kind kind, List<ITerm> headTail) {
        return new ListTerm(kind, headTail);//fixme
    }

    /**
     * @param kind
     * @param terms
     * @return
     */
//    @Override
    public ListTerm newListTerm(Kind kind, ListTerm terms) {
//        final ITerm tail = terms.size() == 0 ? NIL : getLast(terms.getHeads());

        return new ListTerm(kind, terms);
    }

    public ITerm getLast(List<ITerm> heads) {
        return heads.get(heads.size() - 1);
    }

    //    @Override
    public HtProperty createProperty(String name, String value) {
        return PropertyOwner.createProperty(name, value);
    }

    /**
     * @param kind
     * @return
     */
    public Context createContext(Context.Kind kind) {
        HtProperty[] props;
        switch (kind) {
            case LOADING:
            case COMPILATION:
                props = new HtProperty[]{
                        createProperty("entity_identifier", ""),
                        createProperty("entity_prefix", ""),
                        createProperty("entity_type", ""),
                        createProperty("source", ""),
                        createProperty("file", ""),
                        createProperty("basename", ""),
                        createProperty("directory", ""),
                        createProperty("stream", ""),
                        createProperty("target", ""),
                        createProperty("flags", ""),
                        createProperty("term", ""),
                        createProperty("term_position", ""),
                        createProperty("variable_names", "")};
                break;
            case EXECUTION:
                props = new HtProperty[]{
                        createProperty("context", ""),
                        createProperty("entity", ""),
                        createProperty("sender", ""),
                        createProperty("this", ""),
                        createProperty("self", ""),
                        createProperty("file", ""),
                        createProperty("metacall_context", ""),
                        createProperty("coinduction_stack", ""),
                        createProperty("context_stack", "")};
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + kind);
        }

        return new LoadContext(props);

    }

    /**
     * @param s
     * @param arity
     * @return
     */
    public IFunctor createCompound(String s, int arity) {
        int idx = interner.internFunctorName(s, arity);

        return createMostGeneral(idx);
    }

    /**
     * @param name
     * @return
     */
    public IFunctor createMostGeneral(int name) {
        ITerm[] args = new ITerm[interner.getFunctorArity(name)];
        for (int i = 0, argsLength = args.length; i < argsLength; i++) {
            args[i] = new HtVariable(i, null, false);
        }
        return new HtFunctor(name, newListTerm(args));
    }

    public void toString0(StringBuilder sb) {

    }
}
