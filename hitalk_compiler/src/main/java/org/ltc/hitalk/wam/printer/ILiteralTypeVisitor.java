package org.ltc.hitalk.wam.printer;

import org.ltc.hitalk.term.ITermVisitor;

/**
 *
 */
public interface ILiteralTypeVisitor extends ITermVisitor {
    /**
     * @param term
     */
    default void visit ( HtLiteralType term ) {

    }

//    void visit ( HtLiteralType term1, HtLiteralType term2 );
}
