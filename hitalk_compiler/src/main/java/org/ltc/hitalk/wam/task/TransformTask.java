package org.ltc.hitalk.wam.task;

import org.jetbrains.annotations.Contract;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.IMetrics;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.transformers.ITransformer;
import org.ltc.hitalk.wam.transformers.TransformInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Function;

/**
 *
 */
public
class TransformTask extends CompilerTask implements ITransformer <ITerm> {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    /**
     * @param action
     * @param kind
     */
    protected TransformTask ( Function <ITerm, List <ITerm>> action, DirectiveKind kind ) {
        super(action, kind);
    }

    protected ITransformer <ITerm> transformer;
    protected ITerm target;
    private IMetrics bestSoFar = initialMetrics();
    private TransformInfo <ITerm> bestSoFarResult;

    /**
     * @return
     */
    private IMetrics initialMetrics () {
        return null;
    }

    @Contract(pure = true)
    private IMetrics selectBest ( IMetrics bestSoFar, IMetrics delta ) {
        if (bestSoFar.compareTo(delta) <= 0) {
            return bestSoFar;
        }

        return delta;
    }

    private void acceptTransform ( IMetrics delta, TransformInfo <ITerm> result ) {
        if (isAcceptable(getContext().getMaxMetrics())) {
            if (bestSoFar == null) {
                bestSoFar = delta;
                bestSoFarResult = result;
            } else {
                IMetrics tmp = selectBest(bestSoFar, delta);
                if (tmp == delta) {
                    bestSoFar = tmp;
                    bestSoFarResult = result;
                }
            }
        } else {
            transformer.cancel();
        }
    }

    /**
     * @return
     */
    @Override
    public final ExecutionContext getContext () {
        return transformer.getContext();
    }

    /**
     * @param context
     */
    public final void setContext ( ExecutionContext context ) {
        transformer.setContext(context);
    }

    /**
     * @param max
     * @return
     */
    public boolean isAcceptable ( IMetrics max ) {
        return transformer.isAcceptable(max);
    }

    /**
     * Applies a transformation to the term.
     *
     * @param t The term to transform.
     * @return A term which is a transformation of the argument.
     */
    public List <ITerm> transform ( ITerm t ) {
        return transformer.transform(t);
    }

    public void cancel () {

    }
}