package org.ltc.hitalk.compiler;

import com.thesett.aima.logic.fol.TermVisitor;
import org.ltc.hitalk.entities.HtPredicate;

/**
 *
 */
public
interface
HtPredicateVisitor extends TermVisitor {

    /**
     * Visits a predicate.
     *
     * @param predicate The predicate to visit.
     */
    void visit ( HtPredicate predicate );
}
