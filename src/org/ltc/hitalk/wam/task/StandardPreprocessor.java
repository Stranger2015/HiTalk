package org.ltc.hitalk.wam.task;


import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.entities.context.IMetrics;
import org.ltc.hitalk.wam.transformers.ITransformer;

import java.util.List;
import java.util.function.Function;

/**
 *
 */
public
class StandardPreprocessor<T extends Clause, TC extends Term> extends TransformTask <T, TC> {

    /**
     * @param transformer
     */
    public
    StandardPreprocessor ( Function <T, List <T>> action, List <TC> target, ITransformer <T, TC> transformer ) {
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