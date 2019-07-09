package org.ltc.hitalk.wam.task;


import com.thesett.aima.logic.fol.Clause;
import org.ltc.hitalk.entities.context.IMetrics;
import org.ltc.hitalk.wam.transformers.ITransformer;

import java.util.List;

/**
 *
 */
public
class StandardPreprocessor<T extends Clause> extends TransformTask <T> {

    /**
     * @param transformer
     */
    public
    StandardPreprocessor ( List <T> target, ITransformer <T> transformer ) {
        super(target, transformer);
    }

//    /**
//     * @param context
//     */
//    @Override
//    public
//    void setContext ( ExecutionContext context ) {
//
//    }

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