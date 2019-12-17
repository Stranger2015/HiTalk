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
class DefaultTransformer implements ITransformer {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    protected List <ITerm> output;

    protected ExecutionContext context;
    protected ExecutionContext contextBefore;
    protected ExecutionInfo executionInfo;
    protected ExecutionInfo executionInfoBefore;
    protected ITransformer transformer;
    protected TransformInfo result;
    protected ITerm input;


    /**
     * @return
     */
    public ExecutionInfo getExecutionInfoBefore () {
        return executionInfoBefore;
    }

    /**
     * @param term
     */
    public DefaultTransformer ( ITerm term ) {
        output = Collections.singletonList(term);
        transformer = this;
    }

    public
    DefaultTransformer () {
        this(null);
    }

    @Override
    public
    void cancel () {
        //todo
    }

    public ITransformer getTransformer () {
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
    public List <ITerm> transform ( ITerm term ) {
        output = Collections.singletonList(term);
        IMetrics after = context.getCurrentMetrics();
        IMetrics delta = after.subtract(contextBefore.getCurrentMetrics());
        if (output.size() > 0) {
            executionInfo = null;// todo????
        }
        if (!isAcceptable(context.getMaxMetrics())) {
            result = new TransformInfo(context, executionInfo, delta, term, output); //todo
            cancel();
        } else {
            result = new TransformInfo(contextBefore, executionInfo, delta, term, output);//todo
        }

        return output;//todo
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

    /**
     * @return
     */
    public ExecutionContext getContext () {
        return null;
    }

    /**
     * @param context
     */
    public void setContext ( ExecutionContext context ) {

    }

    public boolean isAcceptable ( IMetrics max ) {
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
        output = transform(input);
    }
}
