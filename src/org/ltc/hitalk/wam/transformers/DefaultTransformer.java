package org.ltc.hitalk.wam.transformers;


import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.IMetrics;
import org.ltc.hitalk.wam.context.ExecutionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Created by Anthony on 19.07.2016.
 */
public
class DefaultTransformer<T extends Term> implements ITransformer <T> {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getName());

    protected ExecutionContext context;
    protected ExecutionContext contextBefore;
    //
    protected ExecutionInfo executionInfo;
    protected ExecutionInfo executionInfoBefore;
    //    protected final IApplication app;
    protected T term;
    protected final ITransformer <T> transformer;
    protected Object result;

    public
    DefaultTransformer ( T term ) {
//        this.app = app;
        this.term = term;
        this.transformer = new <T>ZeroTransformer <T>();
    }

    @Override
    public
    void cancel () {
        //todo
    }

    public
    ITransformer <T> getTransformer () {
        return null;
    }

    /**
     *
     */
    @Override
    public
    void message () {
        logger.info("Zero transformer is launched...");
    }

    /**
     * @param target
     * @return
     */
    public
    T transform ( Term target ) {
        message();
        List <T> termList = Collections.emptyList();// execute();
        IMetrics after = context.getCurrentMetrics();
        IMetrics delta = after.subtract(contextBefore.getCurrentMetrics());
        if (termList.size() > 0) {
            executionInfo = null;// todo????
        }
        if (!isAcceptable(context.getMaxMetrics())) {
            result = new TransformInfo(contextBefore, executionInfoBefore, null, term); //todo
            cancel();
        }
        else {
            result = new TransformInfo(context, executionInfo, delta, term); //todo
        }
        //
        return (T) target;
    }

    @Override
    public
    void reset () {
        //todo
    }

    @Override
    public
    ExecutionContext getContext () {
        return context;
    }

    @Override
    public
    void setContext ( ExecutionContext context ) {
        this.context = context;
    }

    @Override
    public
    boolean isAcceptable ( IMetrics max ) {
        return false;
    }

    @Override
    public
    TransformInfo getBestSoFarResult () {
        return null;
    }


    @Override
    public
    void run () {
        result = transform(null);
    }


}
