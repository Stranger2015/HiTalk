package org.ltc.hitalk.term;

/**
 *
 */
public interface IVariableVisitor extends ITermVisitor {

    /**
     * @param variable
     * @return
     */
    default void visit ( HtVariable variable ) {

    }

//    void visit ( HtVariable variable1, HtVariable variable2 );
}
