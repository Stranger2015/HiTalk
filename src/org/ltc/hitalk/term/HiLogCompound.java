package org.ltc.hitalk.term;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.Term;

/**
 *
 */
public
class HiLogCompound extends Functor implements Term {
    private Term name;

    /**
     * Creates a new functor with the specified arguments.
     *
     * @param name      The name of the functor.
     * @param arguments The functors arguments.
     */
    public
    HiLogCompound ( Term name, Term[] arguments ) {
        super(-1, arguments);

        this.name = name;
    }

    /**
     * @param arguments
     */
    public
    HiLogCompound ( Atom name, Term[] arguments ) {
        this((Term) name, arguments);

    }

    public
    HiLogCompound ( Term t, Term args ) {
        this(t, new Term[]{args});
    }

    /**
     * Gets the actual value of a term, which is either the term itself, or in the case of variables, the value that is
     * currently assigned to the variable.
     *
     * @return The term itself, or the assigned value for variables.
     */
    @Override
    public
    Term getValue () {
        return null;
    }

    /**
     * Frees all assigned variables in the term, leaving them unassigned.
     */
    @Override
    public
    void free () {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public
    boolean isGoal () {
        return super.isGoal();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public
    boolean isNumber () {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public
    boolean isFunctor () {
        return true;
    }

    /**
     *
     */
    @Override
    public
    boolean isVar () {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public
    boolean isConstant () {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public
    boolean isCompound () {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public
    boolean isAtom () {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public
    boolean isGround () {
        return super.isGround();
    }
}
