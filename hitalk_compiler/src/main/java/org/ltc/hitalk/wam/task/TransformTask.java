package org.ltc.hitalk.wam.task;

import org.jetbrains.annotations.Contract;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.IMetrics;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.transformers.ITransformer;
import org.ltc.hitalk.wam.transformers.TransformInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Function;

/**
 *
 *
 */
public
class TransformTask<T extends ITerm>
        extends CompilerTask <T>
        implements ITransformer <T> {

    /**
     * @param action
     */
    protected
    TransformTask ( Function <T, List <T>> action ) {
        super(action);
    }

    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    protected ITransformer <T> transformer;
    protected List <T> target;
    private IMetrics bestSoFar = initialMetrics();
    private TransformInfo <T> bestSoFarResult;

    /**
     * @param target
     * @param transformer
     */
    public TransformTask ( List <T> target, ITransformer <T> transformer ) {
        this(null, target);
        this.transformer = transformer;
    }

    /**
     * @param action
     * @param target
     */
    protected TransformTask ( Function <T, List <T>> action, List <T> target ) {
        super(action);
        this.target = target;
        transformer = this;//todo
    }

    /**
     * @param action
     * @param target
     * @param transformer
     */
    public TransformTask ( Function <T, List <T>> action, List <T> target, ITransformer <T> transformer ) {
        super(action);
        this.target = target;
        this.transformer = transformer;
    }

    /**
     * @return
     */
    private
    IMetrics initialMetrics () {
        return null;
    }

    @Contract(pure = true)
    private
    IMetrics selectBest ( IMetrics bestSoFar, IMetrics delta ) {
        if (bestSoFar.compareTo(delta) <= 0) {
            return bestSoFar;
        }

        return delta;
    }

    private
    void acceptTransform ( IMetrics delta, TransformInfo <T> result ) {
        if (isAcceptable(getContext().getMaxMetrics())) {
            if (bestSoFar == null) {
                bestSoFar = delta;
                bestSoFarResult = result;
            }
            else {
                IMetrics tmp = selectBest(bestSoFar, delta);
                if (tmp == delta) {
                    bestSoFar = tmp;
                    bestSoFarResult = result;
                }
            }
        }
        else {
            transformer.cancel();
        }
    }
//
//    /**
//     *
//     */
//    @Override
//    public final
//    void message () {
//        transformer.message();
//    }

//    /**
//     *
//     */
//    @Override
//    public final
//    void reset () {
//        transformer.reset();
//    }
//
    /**
     * @return
     */
    @Override
    public final
    ExecutionContext getContext () {
        return transformer.getContext();
    }
//
//    /**
//     * @param context
//     */
//    @Override
//    public final
//    void setContext ( ExecutionContext context ) {
//        transformer.setContext(context);
//    }
//

    /**
     * @param context
     */
//    @Override
    public final
    void setContext ( ExecutionContext context ) {
        transformer.setContext(context);
    }

    /**
     * @param max
     * @return
     */
//    @Override
    public
    boolean isAcceptable ( IMetrics max ) {
        return transformer.isAcceptable(max);
    }
//
//    /**
//     *
//     */
//    @Override
//    public final
//    void cancel () {
//        transformer.cancel();
//    }
//
//    /**
//     * @return
//     */
//    @Override
//    public final
//    TransformInfo getBestSoFarResult () {
//        return transformer.getBestSoFarResult();
//    }

    /**
     * Applies a transformation to the term.
     *
     * @param t The term to transform.
     * @return A term which is a transformation of the argument.
     */
    public List <T> transform ( T t ) {
        return transformer.transform(t);
    }

    public void cancel () {

    }
}