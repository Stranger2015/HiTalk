package org.ltc.hitalk.wam.transformers;

import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.TermTransformer;
import org.ltc.hitalk.wam.context.ExecutionContext;

/**
 * Created by Anthony on 28.06.2015.
 */
public

interface ITransformer<T extends Term> extends IOperation, TermTransformer {

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
    T transform ( Term t );
}