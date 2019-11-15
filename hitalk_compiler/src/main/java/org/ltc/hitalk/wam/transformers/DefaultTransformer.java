package org.ltc.hitalk.wam.transformers;

import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.IMetrics;
import org.ltc.hitalk.term.ITerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Created by Anthony on 19.07.2016.
 */
public
class DefaultTransformer<T extends ITerm> implements ITransformer <T> {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    protected T targetIn;
    protected List <T> targetOut;

    protected ExecutionContext context;
    protected ExecutionContext contextBefore;
    protected ExecutionInfo executionInfo;
    protected ExecutionInfo executionInfoBefore;
    protected ITransformer <T> transformer;
    protected TransformInfo <T> result;


    /**
     * @return
     */
    public ExecutionInfo getExecutionInfoBefore () {
        return executionInfoBefore;
    }

    /**
     * @param term
     */
    public
    DefaultTransformer ( T term ) {
        targetOut = Collections.singletonList(term);
        transformer = this;
    }

    public
    DefaultTransformer () {
        this((T) null);
    }

    @Override
    public
    void cancel () {
        //todo
    }

    public ITransformer <T> getTransformer () {
        return transformer;
    }

//    /**
//     *
//     */
//    @Override
//    public
//    void message () {
//        logger.info("Default transformer is launched ...");
//    }

    /**
     * @param term
     * @return
     */
    public List <T> transform ( T term ) {
        targetOut = Collections.singletonList(targetIn);
        IMetrics after = context.getCurrentMetrics();
        IMetrics delta = after.subtract(contextBefore.getCurrentMetrics());
        if (targetOut.size() > 0) {
            executionInfo = null;// todo????
        }
        if (!isAcceptable(context.getMaxMetrics())) {
            result = new TransformInfo <>(context, executionInfo, null, targetOut); //todo
            cancel();
        } else {
            result = new TransformInfo <>(contextBefore, executionInfo, delta, targetOut);//todo
        }

        return targetOut;//todo
    }

//    @Override
//    public
//    ExecutionContext getContext () {
//        return context;
//    }

//    @Override
//    public
//    void setContext ( ExecutionContext context ) {
//        this.context = context;
//    }

    public
    boolean isAcceptable ( IMetrics max ) {
        return false;
    }

//    @Override
//    public
//    TransformInfo getBestSoFarResult () {
//        return null;
//    }

    @Override
    public
    void run () {
        targetOut = transform(targetIn);
    }
}
