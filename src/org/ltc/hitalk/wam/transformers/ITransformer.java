package org.ltc.hitalk.wam.transformers;

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.ClauseTransformer;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.IMetrics;

/**
 * Created by Anthony on 28.06.2015.
 */
public
interface ITransformer<T extends Clause> extends IOperation, ClauseTransformer {

    /**
     *
     */
    default
    void message () {
//        getLogger().info(String.format("\n%s is launched\n", getClass().getSimpleName()));
    }

    /**
     *
     */
    void reset ();

    /**
     * @return
     */
    ExecutionContext getContext ();

    /**
     * @param context
     */
    void setContext ( ExecutionContext context );

    /**
     * @param max
     * @return
     */
    boolean isAcceptable ( IMetrics max );

    /**
     * @return
     */
    TransformInfo getBestSoFarResult ();

    /**
     * @param t
     * @return
     */
    T transform ( Clause t );
}