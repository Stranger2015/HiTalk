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
import org.ltc.hitalk.term.Atom;
import org.ltc.hitalk.term.HiLogCompound;
import org.ltc.hitalk.term.ListTerm;

import static org.ltc.hitalk.term.Atom.EMPTY_TERM_ARRAY;

/**
 *
 */
public
class TermFactory implements ITermFactory {

    private VariableAndFunctorInterner interner;

    /**
     * @param interner
     */
    public
    TermFactory ( VariableAndFunctorInterner interner ) {
        this.interner = interner;
    }

    /**
     * @param term
     * @param args
     * @return
     */
    @Override
    public
    HiLogCompound createHiLogCompound ( Term term, ListTerm args ) {
        return null;
    }

    /**
     * @param s
     * @return
     */
    @Override
    public
    Atom createAtom ( String s ) {
        int ffn = interner.internFunctorName(s, 0);
        return new Atom(ffn);
    }

    @Override
    public
    Functor createCompound ( String s, Term[] head, Term tail ) {

        return null;
    }

    @Override
    public
    HiTalkFlag createFlag ( String flagName, String flagValue ) {
        int ffn = interner.internFunctorName(flagName, 0);
        int ffv = interner.internFunctorName(flagValue, 0);
        return new HiTalkFlag(ffn, new Functor(ffv, EMPTY_TERM_ARRAY));
    }

    /**
     * @param kind
     * @param name
     * @param args
     * @return
     */
    @Override
    public
    HtEntityIdentifier createIdentifier ( HtEntityKind kind, String name, Term... args ) {
        int n = interner.internFunctorName(name, args.length);

        return new HtEntityIdentifier(n, args, kind);
    }

    /**
     * @param name
     * @param args
     * @return
     */
    @Override
    public
    HiTalkFlag createFlag ( String name, Term... args ) {
        return createFlag(name, new ListTerm(args));
    }

    @Override
    public
    HtProperty createProperty ( String name, String value ) {
        return null;
    }

    /**
     * @param name
     * @param args
     * @return
     */
    public
    HiTalkFlag createFlag ( String name, ListTerm args ) {
        int n = interner.internFunctorName(name, args.length());

        return new HiTalkFlag(n, args);
    }

    @Override
    public
    HtProperty createProperty ( String name, Term... args ) {
        int n = interner.internFunctorName(name, 0);

        return new HtProperty(n, new ListTerm(args));
    }

    /**
     * @param kind
     * @return
     */
    public
    Context createContext ( Context.Kind kind ) {
        HtProperty[] props;
        switch (kind) {
            case LOADING:
                props = new HtProperty[]{
                        createProperty("entity_identifier", ""),
                        createProperty("entity_prefix", ""),
                        createProperty("entity_type", ""),
                        createProperty("source", ""),
                        createProperty("file", ""),
                        createProperty("basename", ""),
                        createProperty("directory", ""),
                        createProperty("stream", ""),
                        createProperty("target", ""), createProperty("flags", ""), createProperty("term", ""),
                        createProperty("term_position", ""), createProperty("variable_names")
                };
                break;
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
                        createProperty("variable_names")
                };
                break;
            case EXECUTION:
                props = new HtProperty[]{
                        createProperty("context", ""),
                        createProperty("entity", ""),
                        createProperty("sender", ""),
                        createProperty("this", ""),
                        createProperty("self", ""),
                        createProperty("file", ""),
                        createProperty("metacall_context"),
                        createProperty("coinduction_stack"),
                        createProperty("context_stack")
                };
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
    public
    Functor createCompound ( String s, int arity ) {
        int idx = interner.internFunctorName(s, arity);

        return createCompound(idx, arity);
    }

    /**
     * @param s
     * @param arity
     * @return
     */
    public
    Functor createCompound ( int s, int arity ) {
        Term[] args = new Term[arity];
        for (int i = 0, argsLength = args.length; i < argsLength; i++) {
            args[i] = new Variable(i, null, false);
        }
        return new Functor(s, args);
    }
}
