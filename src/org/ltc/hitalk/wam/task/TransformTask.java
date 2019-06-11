package org.ltc.hitalk.wam.task;

import com.thesett.aima.logic.fol.Term;
import org.jetbrains.annotations.Contract;
import org.ltc.hitalk.wam.context.ExecutionContext;
import org.ltc.hitalk.wam.context.ExecutionInfo;
import org.ltc.hitalk.wam.transformers.DefaultTransformer;
import org.ltc.hitalk.wam.transformers.IMetrics;
import org.ltc.hitalk.wam.transformers.ITransformer;
import org.ltc.hitalk.wam.transformers.TransformInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * created by Anthony on 31.01.2016.
 */
public
class TransformTask<T extends Term> extends CompilerTask implements ITransformer <T> {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private IMetrics bestSoFar = initialMetrics();

    private TransformInfo <T> bestSoFarResult;
    protected final ITransformer <T> transformer;

    protected T target;

    public
    TransformTask ( T target, ITransformer <T> transformer ) {
//        super(app);
        bestSoFarResult = new TransformInfo <>(getContext(), getInfo(), initialMetrics(), target);
        this.transformer = transformer;
    }

    public
    TransformTask () {
//        super(app);
        bestSoFarResult = new TransformInfo <>(getContext(), getInfo(), initialMetrics(), null);
        transformer = (ITransformer <T>) new DefaultTransformer(target);
    }


    private
    ExecutionInfo getInfo () {
        return null;
    }

    private
    IMetrics initialMetrics () {
        return null;
    }

    public final
    void invoke () {
        super.invoke();

//        acceptTransform(initialMetrics(), transform(target));

        reset(); //cleanup
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
    public final
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
     * @param term The term to transform.
     * @return A term which is a transformation of the argument.
     */
    @Override
    public final
    T transform ( Term term ) {
        return (T) transformer.transform(term);
    }

    public final
    T getTarget () {
        return target;
    }
}