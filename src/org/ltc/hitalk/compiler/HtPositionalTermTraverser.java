package org.ltc.hitalk.compiler;

import com.thesett.aima.logic.fol.FunctorTraverser;
import com.thesett.aima.logic.fol.TermVisitor;
import com.thesett.aima.logic.fol.compiler.PositionalContext;

public
interface HtPositionalTermTraverser extends HtClauseTraverser,
                                            FunctorTraverser,
                                            PositionalContext {

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
