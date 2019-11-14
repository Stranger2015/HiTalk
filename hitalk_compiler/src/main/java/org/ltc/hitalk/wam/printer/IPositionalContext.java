package org.ltc.hitalk.wam.printer;

import org.ltc.hitalk.term.ITerm;

public interface IPositionalContext {
    /**
     * Indicates whether the current term is a top-level functor in a clause head or body.
     *
     * @return <tt>true</tt> if the current term is a top-level functor in a clause head or body.
     */
    boolean isTopLevel ();

    /**
     * Indicates whether the current term is in a clause head.
     *
     * @return <tt>true</tt> if the current term is in a clause head.
     */
    boolean isInHead ();

    /**
     * Indicates whether the current term is the last functor in a clause body.
     *
     * @return <tt>true</tt> if the current term is the last functor in a clause body.
     */
    boolean isLastBodyFunctor ();

    /**
     * Provides the term at this position.
     *
     * @return The term at this position.
     */
    ITerm getTerm ();

    /**
     * Provides the 'position' of the current term within its parent.
     *
     * @return The 'position' of the current term within its parent.
     */
    int getPosition ();

    /**
     * Gets the positional context of the parent position to this one.
     *
     * @return The positional context of the parent position to this one, or <tt>null</tt> if there is no parent.
     */
    IPositionalContext getParentContext ();
}
