package org.ltc.hitalk.wam.printer;

import com.thesett.aima.logic.fol.LinkageException;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.ITermVisitor;

/**
 *
 */
public
interface IClauseVisitor extends ITermVisitor {
    /**
     * Visits a clause.
     *
     * @param clause The clause to visit.
     */
    default void visit ( HtClause clause ) throws LinkageException {

    }

//    void visit ( HtClause clause1, HtClause clause2 ) throws LinkageException;
}
