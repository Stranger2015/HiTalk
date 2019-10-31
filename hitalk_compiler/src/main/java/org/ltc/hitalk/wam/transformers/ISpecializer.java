package org.ltc.hitalk.wam.transformers;

import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.parser.HtClause;

import java.util.Collections;
import java.util.List;

/**
 * @param <T>
 * @param <TC>
 */
public
interface ISpecializer<T extends HtClause, TC extends Term> extends ITransformer <T, TC> {

    /**
     * @param clause
     * @return
     */
    default List <T> specialize ( T clause ) {
        return Collections.singletonList(clause);
    }

    /**
     * Applies a transformation to the clause.
     *
     * @param clause The clause to transform.
     * @return A clause which is the transformation of the argument.
     */
    @Override
    default List <T> transform ( T clause ) {
        return specialize(clause);
    }
}