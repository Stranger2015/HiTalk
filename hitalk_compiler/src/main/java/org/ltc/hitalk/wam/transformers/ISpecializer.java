package org.ltc.hitalk.wam.transformers;

import org.ltc.hitalk.term.ITerm;

import java.util.Collections;
import java.util.List;

/**
 *
 */
public
interface ISpecializer extends ITransformer {

    /**
     * @param clause
     * @return
     */
    default List <ITerm> specialize ( ITerm clause ) {
        return Collections.singletonList(clause);
    }

    /**
     * Applies a transformation to the clause.
     *
     * @param clause The clause to transform.
     * @return A clause which is the transformation of the argument.
     */
    @Override
    default List <ITerm> transform ( ITerm clause ) {
        return specialize(clause);
    }
}