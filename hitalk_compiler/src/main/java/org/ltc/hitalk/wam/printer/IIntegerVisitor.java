package org.ltc.hitalk.wam.printer;

import org.ltc.hitalk.term.ITermVisitor;
import org.ltc.hitalk.term.IntTerm;

public interface IIntegerVisitor extends ITermVisitor {
    /**
     * @param term
     */
    void visit ( IntTerm term );
}
