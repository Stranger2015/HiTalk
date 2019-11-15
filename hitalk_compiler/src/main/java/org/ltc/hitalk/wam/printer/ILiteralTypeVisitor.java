package org.ltc.hitalk.wam.printer;

import org.ltc.hitalk.term.ITermVisitor;

/**
 *
 */
public interface ILiteralTypeVisitor extends ITermVisitor {
    /**
     * @param term
     */
    void visit ( HtLiteralType term );
}
