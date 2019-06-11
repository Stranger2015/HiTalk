package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.term.Atom;
import org.ltc.hitalk.term.HiLogCompound;
import org.ltc.hitalk.term.ListTerm;

public
class TermFactory implements ITermFactory {

    private final VariableAndFunctorInterner interner;

    public
    TermFactory ( VariableAndFunctorInterner interner ) {
        this.interner = interner;
    }

    /**
     * @return
     */
    @Override
    public
    HiLogCompound createHiLogCompound ( Term name, ListTerm args ) {
        return new HiLogCompound(name, args);
    }

    /**
     * @param s
     * @return
     */
    @Override
    public
    Atom createAtom ( String s ) {
        return new Atom(interner.internFunctorName(s, 0));
    }

    @Override
    public
    Functor createCompound ( String s, Term[] head, Term tail ) {
        int index = interner.internFunctorName(s, 0);
        return new Functor(index, null);//fixme
    }
}
