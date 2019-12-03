package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.entities.context.IMetrics;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.transformers.ITransformer;

import java.util.List;
import java.util.function.Function;

/**
 *
 */
public
class StandardPreprocessor<T extends ITerm> extends TransformTask <T> {

    /**
     * @param transformer
     */
    public StandardPreprocessor ( Function <T, List <T>> action, List <T> target, ITransformer <T> transformer ) {
        super(action, target, transformer);
    }

    /**
     * @param max
     * @return
     */
    @Override
    public
    boolean isAcceptable ( IMetrics max ) {
        return true;
    }

}