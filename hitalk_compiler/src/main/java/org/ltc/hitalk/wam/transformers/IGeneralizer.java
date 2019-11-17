package org.ltc.hitalk.wam.transformers;

import org.ltc.hitalk.term.ITerm;

import java.util.List;

/**
 *
 */
public
interface IGeneralizer<T extends ITerm> extends ITransformer <T> {
    /**
     *  @param term
     * @return
     */
    List <T> generalize ( T term );

    /**
     * Applies a transformation to the term.
     *
     * @param term The term to transform.
     * @return A term which is a transformation of the argument.
     */
    @Override
    default List <T> transform ( T term ) {
        return generalize(term);
    }
}
