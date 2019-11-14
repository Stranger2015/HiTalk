package org.ltc.hitalk.compiler;

import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.term.ITermVisitor;

/**
 *
 */
public
interface
IPredicateVisitor extends ITermVisitor {

    /**
     * Visits a predicate.
     *
     * @param predicate The predicate to visit.
     */
    void visit ( HtPredicate predicate );
}
