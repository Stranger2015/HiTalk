package org.ltc.hitalk.wam.transformers;


import com.thesett.aima.logic.fol.Term;
import org.apache.commons.lang3.tuple.Triple;
import org.ltc.hitalk.wam.context.ExecutionContext;
import org.ltc.hitalk.wam.context.ExecutionInfo;

public
class TransformInfo<T extends Term> extends Triple <ExecutionContext, ExecutionInfo, IMetrics> {


    /**
     * @param context
     * @param executionInfo
     * @param delta
     */
    public
    TransformInfo ( ExecutionContext context, ExecutionInfo executionInfo, IMetrics delta, T target ) {

        this.context = context;
        info = executionInfo;
        metrics = delta;
        this.target = target;
    }

    public
    ExecutionContext getContext () {
        return context;
    }

    public
    ExecutionInfo getInfo () {
        return info;
    }

    public
    IMetrics getMetrics () {
        return metrics;
    }

    private ExecutionContext context;
    private ExecutionInfo info;
    private IMetrics metrics;
    private final T target;

    /**
     * <p>Gets the left element from this triple.</p>
     *
     * @return the left element, may be null
     */
    @Override
    public
    ExecutionContext getLeft () {
        return getContext();
    }

    /**
     * <p>Gets the middle element from this triple.</p>
     *
     * @return the middle element, may be null
     */
    @Override
    public
    ExecutionInfo getMiddle () {
        return getInfo();
    }

    /**
     * <p>Gets the right element from this triple.</p>
     *
     * @return the right element, may be null
     */
    @Override
    public
    IMetrics getRight () {
        return getMetrics();
    }

    public
    T getTarget () {
        return target;
    }
}
