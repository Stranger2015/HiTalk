package org.ltc.hitalk.term;

/**
 *
 */
public interface IVariableVisitor extends ITermVisitor {
    /**
     * @param variable
     */
    void visit ( HtVariable variable );
}
