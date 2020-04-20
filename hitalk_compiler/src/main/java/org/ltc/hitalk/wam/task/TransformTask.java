package org.ltc.hitalk.wam.task;

import org.jetbrains.annotations.Contract;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.IMetrics;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.transformers.ITransformer;
import org.ltc.hitalk.wam.transformers.TransformInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;

/**
 *
 */
public
class TransformTask extends PreCompilerTask<HtClause> implements ITransformer {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    /**
     * @param tokenSource
     * @param kind
     */
    protected TransformTask(IPreCompiler preCompiler,
                            PlLexer tokenSource,
                            EnumSet<DirectiveKind> kind) {
        super(preCompiler, tokenSource, kind);
    }

    protected ITransformer transformer;
    private IMetrics bestSoFar = initialMetrics();
    private TransformInfo bestSoFarResult;

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

    private void acceptTransform ( IMetrics delta, TransformInfo result ) {
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

    /**
     * @param item
     */
    public void remove(PreCompilerTask<HtClause> item) {

    }

    public void toString0 ( StringBuilder sb ) {
    }
}