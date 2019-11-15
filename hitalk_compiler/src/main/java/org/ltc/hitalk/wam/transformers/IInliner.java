package org.ltc.hitalk.wam.transformers;

import org.ltc.hitalk.term.ITerm;

import java.util.List;

/**
 *
 */
public
interface IInliner<T extends ITerm> extends ITransformer <T> {

    /**
     * @param clause
     * @return
     */
    List <T> inline ( T clause );

    /**
     * Applies a transformation to the clause.
     *
     * @param clause The clause to transform.
     * @return A clause which is a transformation of the argument.
     */
    @Override
    default List <T> transform ( T clause ) {
        return inline(clause);
    }
}
