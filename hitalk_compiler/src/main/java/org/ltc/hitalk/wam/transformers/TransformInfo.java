package org.ltc.hitalk.wam.transformers;


import org.apache.commons.lang3.tuple.Triple;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.IMetrics;
import org.ltc.hitalk.term.ITerm;

import java.util.Collections;
import java.util.List;

/**
 * @param <T>
 */
public
class TransformInfo<T extends ITerm> extends Triple <ExecutionContext, ExecutionInfo, IMetrics> {

    private ExecutionContext context;
    private ExecutionInfo info;
    private IMetrics metrics;
    private List <T> target;

    /**
     * @param context
     * @param executionInfo
     * @param delta
     */
    public TransformInfo ( ExecutionContext context,
                           ExecutionInfo executionInfo,
                           IMetrics delta,
                           T target ) {

        this.context = context;
        info = executionInfo;
        metrics = delta;
        this.target = Collections.singletonList(target);
    }

    /**
     *
     */
    public TransformInfo () {

    }

    /**
     * @return
     */
    public ExecutionContext getContext () {
        return context;
    }

    /**
     * @return
     */
    public ExecutionInfo getInfo () {
        return info;
    }

    /**
     * @return
     */
    public IMetrics getMetrics () {
        return metrics;
    }

    /**
     * <p>Gets the left element from this triple.</p>
     *
     * @return the left element, may be null
     */
    @Override
    public ExecutionContext getLeft () {
        return getContext();
    }

    /**
     * <p>Gets the middle element from this triple.</p>
     *
     * @return the middle element, may be null
     */
    @Override
    public ExecutionInfo getMiddle () {
        return getInfo();
    }

    /**
     * <p>Gets the right element from this triple.</p>
     *
     * @return the right element, may be null
     */
    @Override
    public IMetrics getRight () {
        return getMetrics();
    }

    /**
     * @return
     */
    public List <T> getTarget () {
        return target;
    }
}
