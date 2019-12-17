package org.ltc.hitalk.wam.transformers;

import org.ltc.hitalk.term.ITerm;

import java.util.List;

/**
 *
 */
public
interface IInliner extends ITransformer {

    /**
     * @param clause
     * @return
     */
    List <ITerm> inline ( ITerm clause );

    /**
     * Applies a transformation to the clause.
     *
     * @param clause The clause to transform.
     * @return A clause which is a transformation of the argument.
     */
    @Override
    default List <ITerm> transform ( ITerm clause ) {
        return inline(clause);
    }
}
