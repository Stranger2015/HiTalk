package org.ltc.hitalk.wam.transformers;


import org.apache.commons.lang3.tuple.Triple;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.IMetrics;
import org.ltc.hitalk.term.ITerm;

import java.util.List;

/**
 *
 */
public
class TransformInfo extends Triple <ExecutionContext, ExecutionInfo, IMetrics> {

    private final ITerm input;
    private final List <ITerm> output;
    private ExecutionContext context;
    private ExecutionInfo info;
    private IMetrics metrics;

    /**
     * @param context
     * @param executionInfo
     * @param delta
     */
    public TransformInfo ( ExecutionContext context,
                           ExecutionInfo executionInfo,
                           IMetrics delta,
                           ITerm input,
                           List <ITerm> output ) {

        this.context = context;
        info = executionInfo;
        metrics = delta;
        this.input = input;
        this.output = output;
    }
//
//    /**
//     *
//     */
//    public TransformInfo () {
//
//    }

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
    public List <ITerm> getTarget () {
        return output;
    }
}
