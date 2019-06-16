package org.ltc.hitalk.wam.task;


import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.wam.transformers.ITransformer;

/**
 *
 */
public
class StandardPreprocessor<T extends Term> extends TransformTask <T> {

    /**
     * @param transformer
     */
    public
    StandardPreprocessor ( T target, ITransformer <T> transformer ) {
        super(target, transformer);
    }
}