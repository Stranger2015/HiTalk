package org.ltc.hitalk.term;

import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.wam.compiler.HtFunctor;

import static org.ltc.hitalk.term.ListTerm.NIL;

/**
 *
 */
public
class Atom extends HtFunctor {

    public static final ITerm[] EMPTY_TERM_ARRAY = new ITerm[0];

    /**
     * Creates a new Atom.
     *
     * @param name The name of the functor.
     */
    public
    Atom ( int name ) {
        super(name, NIL);
    }


    /**
     * Reports whether or not this term is a functor.
     *
     * @return Always <tt>false</tt>.
     */
    @Override
    public
    boolean isFunctor () {
        return true;
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
    @Override
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
    public boolean isAtom () {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @param interner
     * @param printVarName
     * @param printBindings
     */
    public String toString ( IVafInterner interner, boolean printVarName, boolean printBindings ) {
        return super.toString(interner, printVarName, printBindings);
    }

    /**
     * Reports whether or not this term is a ground term, i.e. contains no vars.
     *
     * @return Always <tt>true</tt>
     */
    @Override
    public boolean isGround () {
        return true;
    }
}
