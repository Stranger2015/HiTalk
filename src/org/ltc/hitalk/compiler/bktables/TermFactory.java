package org.ltc.hitalk.compiler.bktables;


import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.Variable;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.entities.HtEntityIdentifier;
import org.ltc.hitalk.entities.HtEntityKind;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.term.Atom;
import org.ltc.hitalk.term.HiLogCompound;
import org.ltc.hitalk.term.ListTerm;

/**
 *
 */
public
class TermFactory implements ITermFactory {

    private VariableAndFunctorInterner interner;

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
        return null;
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
        return new HiTalkFlag(ffn, ffv);
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
        int n = interner.internFunctorName(name, args.length);

        return new HiTalkFlag(n, args.length);
    }

    @Override
    public
    HtProperty createProperty ( String name, Term... args ) {
        return new HtProperty(name, args);
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
