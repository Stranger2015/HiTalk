package org.ltc.hitalk.wam.compiler;


import org.ltc.hitalk.term.ITerm;

/**
 * atom, compound, or var
 */
public
interface ICallable extends ITerm {
    /**
     * Reports whether or not this term is a number.
     *
     * @return <tt>true</tt> if this term is a number, <tt>false</tt> otherwise.
     */
    @Override
    default
    boolean isNumber () {
        return false;
    }

    /**
     * Reports whether or not this term is a functor.
     *
     * @return <tt>true</tt> if this term is a functor, <tt>false</tt> otherwise.
     */
    @Override
    default
    boolean isFunctor () {
        return !isVar();
    }

    /**
     * Reports whether or not this term is a variable.
     *
     * @return <tt>true</tt> if this term is a variable, <tt>false</tt> otherwise.
     */
    @Override
    boolean isVar ();

    /**
     * Reports whether or not this term is a constant (a number of a functor of arity zero).
     *
     * @return <tt>true</tt> if this term is constant, <tt>false</tt> otherwise.
     */
    @Override
    default
    boolean isConstant () {
        return !isVar() && !isCompound();
    }

    /**
     * Reports whether or not this term is compound (a functor of arity one or more).
     *
     * @return <tt>true</tt> if this term is compound, <tt>fals</tt> otherwise.
     */
    @Override
    boolean isCompound ();

    /**
     * Reports whether or not this term is an atom (a functor of arity zero).
     *
     * @return <tt>true</tt> if this term is an atom, <tt>false</tt> otherwise.
     */
    @Override
    boolean isAtom ();

    /**
     * Reports whether or not this term is a ground term.
     *
     * @return <tt>true</tt> if this term is a ground term, <tt>false</tt> othewise.
     */
    @Override
    boolean isGround ();
}
