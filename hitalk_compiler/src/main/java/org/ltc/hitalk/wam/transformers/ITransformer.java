package org.ltc.hitalk.wam.transformers;

import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.IMetrics;
import org.ltc.hitalk.term.ITerm;

import java.util.List;

/**
 * Created by Anthony on 28.06.2015.
 */
public
interface ITransformer extends IOperation {

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
     * @param t
     * @return
     */
    List <ITerm> transform ( ITerm t );
}