package org.ltc.hitalk.wam.task;


import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.Term;
import org.jetbrains.annotations.Contract;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.IMetrics;
import org.ltc.hitalk.wam.transformers.ITransformer;
import org.ltc.hitalk.wam.transformers.TransformInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Function;

/**
 * created by Anthony on 31.01.2016.
 */
public
class TransformTask<T extends Clause, TC extends Term>
        extends CompilerTask <T, TC>
        implements ITransformer <T, TC> {

    /**
     * @param action
     */
    protected
    TransformTask ( Function <T, List <T>> action ) {
        super(action);
    }
    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    protected ITransformer <T, TC> transformer;
    protected List <TC> target;
    private IMetrics bestSoFar = initialMetrics();
    private TransformInfo <T> bestSoFarResult;

    /**
     * @param target
     * @param transformer
     */
    public
    TransformTask ( List <TC> target, ITransformer <T, TC> transformer ) {
        this(null, target);
        this.transformer = transformer;
    }

    /**
     * @param action
     * @param target
     */
    protected
    TransformTask ( Function <T, List <T>> action, List <TC> target ) {
        super(action);
        this.target = target;
        transformer = this;//todo
    }

    /**
     * @param action
     * @param target
     * @param transformer
     */
    public
    TransformTask ( Function <T, List <T>> action, List <TC> target, ITransformer <T, TC> transformer ) {
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

    /**
     *
     */
    @Override
    public final
    void message () {
        transformer.message();
    }

    /**
     *
     */
    @Override
    public final
    void reset () {
        transformer.reset();
    }

    /**
     * @return
     */
    @Override
    public final
    ExecutionContext getContext () {
        return transformer.getContext();
    }

    /**
     * @param context
     */
    @Override
    public final
    void setContext ( ExecutionContext context ) {
        transformer.setContext(context);
    }

    /**
     * @param max
     * @return
     */
    @Override
    public
    boolean isAcceptable ( IMetrics max ) {
        return transformer.isAcceptable(max);
    }

    /**
     *
     */
    @Override
    public final
    void cancel () {
        transformer.cancel();
    }

    /**
     * @return
     */
    @Override
    public final
    TransformInfo getBestSoFarResult () {
        return transformer.getBestSoFarResult();
    }

    /**
     * Applies a transformation to the term.
     *
     * @param t The term to transform.
     * @return A term which is a transformation of the argument.
     */
    public
    T transform ( T t ) {
        return (T) transformer.transform(t);
    }

    /**
     * @param t
     * @return
     */
    @Override
    public
    TC transform ( TC t ) {
        return transformer.transform(t);
    }

    public final
    List <TC> getTarget () {
        return target;
    }
}