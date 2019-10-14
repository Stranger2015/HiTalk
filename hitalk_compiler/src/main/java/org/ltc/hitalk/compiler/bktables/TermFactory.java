package org.ltc.hitalk.compiler.bktables;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.Variable;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.entities.HtEntityIdentifier;
import org.ltc.hitalk.entities.HtEntityKind;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.entities.context.Context;
import org.ltc.hitalk.entities.context.LoadContext;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlToken.TokenKind;
import org.ltc.hitalk.term.*;
import org.ltc.hitalk.term.DottedPair.Kind;
import org.ltc.hitalk.wam.compiler.HtFunctor;

import static org.ltc.hitalk.term.Atom.EMPTY_TERM_ARRAY;

/**
 *
 */
public class TermFactory implements ITermFactory {

    private VariableAndFunctorInterner interner;

    /**
     * @param interner
     */
    public TermFactory ( VariableAndFunctorInterner interner ) {
        this.interner = interner;
    }

    /**
     * @param value
     * @return
     */
    @Override
    public Atom newAtom ( String value ) {
        return newAtom(interner.internFunctorName(value, 0));
//        return newFunctor(HiLogParser.hilogApply, name, new DottedPair());
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
    public Atom newAtom ( double value ) {
        return null;
    }

    /**
     *
     * @param hilogApply
     * @param name
     * @return
     */
    @Override
    public Functor newFunctor ( int hilogApply, String name, DottedPair dottedPair ) {
        int arity = dottedPair.getArguments().length - 1;
        return newFunctor(interner.internFunctorName(name, arity), dottedPair);
    }

    /**
     * @param value
     * @param args
     * @return
     */
    @Override
    public Functor newFunctor ( int value, DottedPair args ) {
        return new HtFunctor(value, args.getArguments());
    }

    /**
     * @param value
     * @return
     */
    @Override
    public Term newVariable ( String value ) {
        return new Variable(interner.internVariableName(value), null, false);
    }

    /**
     * Prologの項(term)を作成します。
     *
     * @author shun
     */

    private Kind kind;
    private Term[] headTail;

    /**
     * 文字列アトムを生成します。
     */
//        public abstract Term newAtom ( String value );
    public Functor newAtom ( VariableAndFunctorInterner interner, TokenKind ldelim, TokenKind rdelim ) {
        String s = String.format("%s%s", ldelim.getImage(), rdelim.getImage());
        return new Functor(interner.internFunctorName(s, 0), EMPTY_TERM_ARRAY);
    }

    /**
     * @param kind
     * @param headTail
     * @return
     */
    public DottedPair newDottedPair ( Kind kind, Term[] headTail ) {
        this.kind = kind;
        this.headTail = headTail;
        DottedPair t;
        if (headTail.length == 0) {
            t = new DottedPair();
        } else { //if (headTail.length ==1){ //[|VarOrList] []
            t = new DottedPair(kind, headTail);
        }
        return t;
    }

    /**
     * @param s
     * @return
     */
    @Override
    public Functor createAtom ( String s ) {
        int ffn = interner.internFunctorName(s, 0);
        return new Atom(ffn);
    }

    @Override
    public Functor createCompound ( String s, Term[] head, Term tail ) {

        return null;
    }

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
    public HtEntityIdentifier createIdentifier ( HtEntityKind kind, String name, Term... args ) {
        int n = interner.internFunctorName(name, args.length);

        return new HtEntityIdentifier(n, args, kind);
    }

    /**
     * @param name
     * @param args
     * @return
     */
    @Override
    public HtProperty createFlag ( String name, Term... args ) {
        return createFlag(name, new ListTerm(args));
    }

    @Override
    public Functor newFunctor ( int hilogApply, Term name, DottedPair args ) {
        Term[] headTail = args.getArguments();
        Term[] nameHeadTail = new Term[headTail.length + 1];
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
        interner.internFunctorName(name, args.length());

        return null; //new Flag(n, args);
    }

    //    @Override
    public HtProperty createProperty ( String name, Term... args ) {
        int n = interner.internFunctorName(name, 0);

//        return new HtProperty(n, new ListTerm(args));
        return new HtProperty(new ListTerm(args));
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
    public Functor createCompound ( String s, int arity ) {
        int idx = interner.internFunctorName(s, arity);

        return createCompound(idx, arity);
    }

    /**
     * @param s
     * @param arity
     * @return
     */
    public Functor createCompound ( int s, int arity ) {
        Term[] args = new Term[arity];
        for (int i = 0, argsLength = args.length; i < argsLength; i++) {
            args[i] = new Variable(i, null, false);
        }
        return new Functor(s, args);
    }
}
