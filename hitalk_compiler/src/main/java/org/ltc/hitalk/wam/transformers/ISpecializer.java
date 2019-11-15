package org.ltc.hitalk.wam.transformers;

import org.ltc.hitalk.term.ITerm;

import java.util.Collections;
import java.util.List;

/**
 * @param <T>
 */
public
interface ISpecializer<T extends ITerm> extends ITransformer <T> {

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