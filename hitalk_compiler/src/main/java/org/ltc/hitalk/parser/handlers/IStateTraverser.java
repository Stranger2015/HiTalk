package org.ltc.hitalk.parser.handlers;

public interface IStateTraverser<T extends ParserStateHandler> extends IContext<T> {
    void traverse(T n);

    /**
     * Indicates that a call is being made to a term visitor because its context is being established.
     *
     * @return <tt>true</tt> if a call is being made to a term visitor because its context is being established.
     */
    boolean isEnteringContext();

    /**
     * Indicates that a call is being made to a term visitor because its context is being left.
     *
     * @return <tt>true</tt> if a call is being made to a term visitor because its context is being left.
     */
    boolean isLeavingContext();

    /**
     * Indicates that a call is being made to a term visitor because its context is being changed.
     *
     * @return <tt>true</tt> if a call is being made to a term visitor because its context is being changed.
     */
    boolean isContextChange();

    /**
     * Allows a visitor to notify on context changes to be set.
     *
     * @param contextChangeVisitor The visitor to notify on context changes.
     */
    void setContextChangeVisitor(IStateVisitor<T> contextChangeVisitor);
}
