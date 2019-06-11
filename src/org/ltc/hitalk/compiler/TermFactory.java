package org.ltc.hitalk.compiler;


import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.Variable;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import org.ltc.hitalk.ITermFactory;
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

    public
    Functor createCompound ( String s, int arity ) {


        int idx = interner.internFunctorName(s, arity);
        return createCompound(idx, arity);
    }

    public
    Functor createCompound ( int s, int arity ) {
        Term[] args = new Term[arity];
        for (int i = 0, argsLength = args.length; i < argsLength; i++) {
            args[i] = new Variable(i, null, false);
        }
        return new Functor(s, args);
    }


}
