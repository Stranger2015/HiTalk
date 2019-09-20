package org.ltc.hitalk.wam.printer;

import com.thesett.aima.logic.fol.TermVisitor;
import org.ltc.hitalk.parser.HtClause;

/**
 *
 */
public
interface HtClauseVisitor extends TermVisitor {
    /**
     * Visits a clause.
     *
     * @param clause The clause to visit.
     */
    void visit ( HtClause clause );
}
