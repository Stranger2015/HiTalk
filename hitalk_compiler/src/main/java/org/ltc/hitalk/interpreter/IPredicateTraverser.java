package org.ltc.hitalk.interpreter;

import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.search.Operator;
import org.ltc.hitalk.entities.HtPredicate;

import java.util.Iterator;

public
interface IPredicateTraverser {
    /**
     * Traverses a predicate.
     *
     * @param predicate The predicate to traverse.
     * @param reverse   <tt>true</tt> if the child operators should be presented in reverse order to what is deemed to
     *                  be a natural, left-to-right ordering.
     * @return An iterator over operators producing the traversed elements of the predicate.
     */
    Iterator <Operator <Term>> traverse ( HtPredicate predicate, boolean reverse );
}
