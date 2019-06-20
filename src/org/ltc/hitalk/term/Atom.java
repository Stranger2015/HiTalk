package org.ltc.hitalk.term;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;

public
class Atom extends Functor {

    public static final Term[] EMPTY_TERM_ARRAY = new Term[0];

    static
    Functor create ( String name, VariableAndFunctorInterner interner ) {
        return new Functor(interner.internFunctorName(name, 0));
    }

    /**
     * Creates a new Atom.
     *
     * @param name The name of the functor.
     */
    public
    Atom ( int name ) {
        super(name, EMPTY_TERM_ARRAY);
    }


    /**
     * Reports whether or not this term is a functor.
     *
     * @return Always <tt>false</tt>.
     */
    @Override
    public
    boolean isFunctor () {
        return false;
    }

    /**
     * Reports whether or not this term is a constant (an atom).
     *
     * @return Always  <tt>true</tt>
     */
    @Override
    public
    boolean isConstant () {
        return true;
    }

    /**
     * Reports whether or not this term is compound (a functor of arity one or more).
     *
     * @return Always  <tt>false</tt>
     */
    @Override
    public
    boolean isCompound () {
        return false;
    }

    /**
     * @return
     */
//    @Override
    public
    boolean isHiLog () {
        return false;
    }

    /**
     * Reports whether or not this term is an atom (a functor of arity zero).
     *
     * @return Always <tt>true</tt>
     */
    @Override
    public
    boolean isAtom () {
        return true;
    }

    /**
     * Reports whether or not this term is a ground term, i.e. contains no vars.
     *
     * @return Always <tt>true</tt>
     */
    @Override
    public
    boolean isGround () {
        return true;
    }
}
