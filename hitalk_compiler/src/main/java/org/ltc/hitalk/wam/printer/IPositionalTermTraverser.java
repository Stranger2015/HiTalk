package org.ltc.hitalk.wam.printer;

import com.thesett.aima.logic.fol.TermVisitor;

public
interface IPositionalTermTraverser extends IClauseTraverser,
        IFunctorTraverser,
        IPositionalContext {

    /**
     * Indicates that a call is being made to a term visitor because its context is being established.
     *
     * @return <tt>true</tt> if a call is being made to a term visitor because its context is being established.
     */
    boolean isEnteringContext ();

    /**
     * Indicates that a call is being made to a term visitor because its context is being left.
     *
     * @return <tt>true</tt> if a call is being made to a term visitor because its context is being left.
     */
    boolean isLeavingContext ();

    /**
     * Indicates that a call is being made to a term visitor because its context is being changed.
     *
     * @return <tt>true</tt> if a call is being made to a term visitor because its context is being changed.
     */
    boolean isContextChange ();

    /**
     * Allows a visitor to notify on context changes to be set.
     *
     * @param contextChangeVisitor The visitor to notify on context changes.
     */
    void setContextChangeVisitor ( TermVisitor contextChangeVisitor );
}
