package org.ltc.hitalk.compiler.bktables;

import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.entities.HtEntityIdentifier;
import org.ltc.hitalk.entities.HtEntityKind;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.entities.context.Context;
import org.ltc.hitalk.entities.context.LoadContext;
import org.ltc.hitalk.parser.PlToken.TokenKind;
import org.ltc.hitalk.term.*;
import org.ltc.hitalk.term.ListTerm.Kind;
import org.ltc.hitalk.wam.compiler.HtFunctor;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.nio.file.Path;

import static org.ltc.hitalk.term.Atom.EMPTY_TERM_ARRAY;

/**
 *
 */
public class TermFactory implements ITermFactory {

    private IVafInterner interner;

    /**
     * @param interner
     */
    public TermFactory ( IVafInterner interner ) {
        this.interner = interner;
    }

    /**
     * @param value
     * @return
     */
    @Override
    public Atom newAtom ( String value ) {
        return newAtom(interner.internFunctorName(value, 0));
    }

    /**
     * @param value
     * @return
     */
    @Override
    public Atom newAtom ( int value ) {
        return new Atom(value);
    }

    /**
     * @param value
     * @return
     */
    @Override
    public FloatTerm newAtom ( double value ) {
        return newAtomic(value);
    }

    /**
     * @param hilogApply
     * @param name
     * @return
     */
    @Override
    public IFunctor newFunctor ( int hilogApply, String name, ListTerm listTerm ) {
        int arity = listTerm.getArguments().length - 1;
        return newFunctor(interner.internFunctorName(name, arity), listTerm);
    }

    /**
     * @param value
     * @param args
     * @return
     */
    @Override
    public IFunctor newFunctor ( int value, ListTerm args ) {
        return new HtFunctor(value, args.getArguments());
    }

    /**
     * @param value
     * @return
     */
    @Override
    public HtVariable newVariable ( String value ) {
        return new HtVariable(
                interner.internVariableName(value),
                null,
                false
        );
    }

    /**
     * Prologの項(term)を作成します。
     *
     * @author shun
     */

    private Kind kind;
    private ITerm[] headTail;

    /**
     * 文字列アトムを生成します。
     */
//        public abstract Term newAtom ( String value );
    public IFunctor newAtom ( IVafInterner interner, TokenKind ldelim, TokenKind rdelim ) {
        String s = String.format("%s%s", ldelim.getImage(), rdelim.getImage());
        return new HtFunctor(interner.internFunctorName(s, 0), EMPTY_TERM_ARRAY);
    }

    /**
     * @param kind
     * @param headTail
     * @return
     */
    public ListTerm newListTerm ( Kind kind, ITerm[] headTail ) {
        this.kind = kind;
        this.headTail = headTail;
        ListTerm t;
        if (headTail.length == 0) {
            t = new ListTerm(Kind.LIST, headTail);
        } else { //if (headTail.length ==1){ //[|VarOrList] []
            t = new ListTerm(kind, headTail);
        }

        return t;
    }

    @Override
    public HtProperty createFlag ( String scratch_directory, Path scratchDir ) {
        return null;
    }

    public IFunctor createMostGeneral ( IFunctor functor ) {
//        List <ITerm> l = new ArrayList <>();
//        for (int i = 0; i < functor.getArity(); i++) {
//            l.add(createVariable(String.format("$VAR%d", i)));
//        }

        return createMostGeneral(functor.getName());
    }

    public HtVariable createVariable ( String vname ) {
        return new HtVariable(interner.internVariableName(vname), null, false);
    }

    /**
     * @param s
     * @return
     */
    @Override
    public IFunctor createAtom ( String s ) {
        int ffn = interner.internFunctorName(s, 0);
        return new HtFunctor(ffn, EMPTY_TERM_ARRAY);
    }

    /**
     * @param flagName
     * @param flagValue
     * @return
     */
    @Override
    public HtProperty createFlag ( String flagName, String flagValue ) {
        int ffn = interner.internFunctorName(flagName, 0);
        int ffv = interner.internFunctorName(flagValue, 0);
        return null;//new Flag(ffn, new Functor(ffv, EMPTY_TERM_ARRAY));
    }


    /**
     * @param kind
     * @param name
     * @param args
     * @return
     */
    @Override
    public HtEntityIdentifier createIdentifier ( HtEntityKind kind, String name, ITerm... args ) {
        int n = interner.internFunctorName(name, args.length);

        return new HtEntityIdentifier(n, args, kind);
    }

    /**
     * @param name
     * @param args
     * @return
     */
    @Override
    public HtProperty createFlag ( String name, ITerm... args ) {
        return createFlag(name, new ListTerm(Kind.LIST, args));
    }

    @Override
    public IFunctor newFunctor ( int hilogApply, ITerm name, ListTerm args ) {
        ITerm[] headTail = args.getArguments();
        ITerm[] nameHeadTail = new ITerm[headTail.length + 1];
        System.arraycopy(headTail, 0, nameHeadTail, 1, headTail.length);
        nameHeadTail[0] = name;
        return new HtFunctor(hilogApply, nameHeadTail);
    }

    @Override
    public IntTerm newAtomic ( int i ) {
        return null;
    }

    @Override
    public FloatTerm newAtomic ( double f ) {
        return null;
    }

    public ListTerm newDottedPair ( Kind kind, ITerm[] terms ) {
        return null;
    }

    //    @Override
    public HtProperty createProperty ( String name, String value ) {
        return null;
    }

    /**
     * @param name
     * @param args
     * @return
     */
    public HtProperty createFlag ( String name, ListTerm args ) {
        int n = -1;
        interner.internFunctorName(name, args.size());

        return null; //new Flag(n, args);
    }

    //    @Override
    public HtProperty createProperty ( String name, ITerm... args ) {
        int n = interner.internFunctorName(name, 0);

//        return new HtProperty(n, new ListTerm(args));
        return new HtProperty(new ListTerm(Kind.LIST, args));
    }

    /**
     * @param kind
     * @return
     */
    public Context createContext ( Context.Kind kind ) {
        HtProperty[] props;
        switch (kind) {
            case LOADING:
            case COMPILATION:
                props = new HtProperty[]{createProperty("entity_identifier", ""), createProperty("entity_prefix", ""), createProperty("entity_type", ""), createProperty("source", ""), createProperty("file", ""), createProperty("basename", ""), createProperty("directory", ""), createProperty("stream", ""), createProperty("target", ""), createProperty("flags", ""), createProperty("term", ""), createProperty("term_position", ""), createProperty("variable_names")};
                break;
            case EXECUTION:
                props = new HtProperty[]{createProperty("context", ""), createProperty("entity", ""), createProperty("sender", ""), createProperty("this", ""), createProperty("self", ""), createProperty("file", ""), createProperty("metacall_context"), createProperty("coinduction_stack"), createProperty("context_stack")};
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
    public IFunctor createCompound ( String s, int arity ) {
        int idx = interner.internFunctorName(s, arity);

        return createMostGeneral(idx);
    }

    /**
     * @param name
     * @return
     */
    public IFunctor createMostGeneral ( int name ) {
        ITerm[] args = new ITerm[interner.getFunctorArity(name)];
        for (int i = 0, argsLength = args.length; i < argsLength; i++) {
            args[i] = new HtVariable(i, null, false);
        }
        return new HtFunctor(name, args);
    }
}
