package org.ltc.hitalk.wam.transformers;

import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.IMetrics;
import org.ltc.hitalk.term.ITerm;

import java.util.List;

/**
 * Created by Anthony on 28.06.2015.
 */
public
interface ITransformer<T extends ITerm> extends IOperation {

//    /**
//     *
//     */
//    default
//    void message () {
////        getLogger().info(String.format("\n%s is launched\n", getClass().getSimpleName()));
//    }
//
//    /**
//     *
//     */
//    void reset ();
//
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
//
//    /**
//     * @return
//     */
//    TransformInfo getBestSoFarResult ();

    /**
     * @param t
     * @return
     */
    List <T> transform ( T t );

}