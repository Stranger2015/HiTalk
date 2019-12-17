package org.ltc.hitalk.wam.transformers;

import org.ltc.hitalk.term.ITerm;

import java.util.List;

/**
 *
 */
public
interface IGeneralizer extends ITransformer {
    /**
     *  @param term
     * @return
     */
    List <ITerm> generalize ( ITerm term );

    /**
     * Applies a transformation to the term.
     *
     * @param term The term to transform.
     * @return A term which is a transformation of the argument.
     */
    @Override
    default List <ITerm> transform ( ITerm term ) {
        return generalize(term);
    }
}
