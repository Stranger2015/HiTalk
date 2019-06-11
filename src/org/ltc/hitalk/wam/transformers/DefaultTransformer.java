package org.ltc.hitalk.wam.transformers;


import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.wam.context.ExecutionContext;
import org.ltc.hitalk.wam.context.ExecutionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Created by Anthony on 19.07.2016.
 */
public
class DefaultTransformer implements ITransformer <Term> {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getName());

    protected ExecutionContext context;
    protected ExecutionContext contextBefore;
    //
    protected ExecutionInfo executionInfo;
    protected ExecutionInfo executionInfoBefore;
    //    protected final IApplication app;
    protected Term term;
    protected final ITransformer <Term> transformer;
    protected Object result;

    public
    DefaultTransformer ( Term term ) {
//        this.app = app;
        this.term = term;
        this.transformer = new ZeroTransformer();
    }

    @Override
    public
    void cancel () {
        //todo
    }

    public
    ITransformer <Term> getTransformer () {
        return null;
    }

    /**
     *
     */
    @Override
    public
    void message () {
        logger.info("Default transformer is launched...");
    }

    /**
     * @param target
     * @return
     */
    public
    Term transform ( Term target ) {
        message();
        List <Term> termList = Collections.emptyList();// execute();
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
        return target;
    }

    /**
     *
     */
//    @Override
//    public
//    List <Term> execute () {
////        List <Term> l = new ArrayList <>();
//        List <Term> l = execute0();
////        for (Term t : r) {
////            setTerm(t);
////            l.addAll(getNext().execute());
////        }
//        //
//        return l;
//    }
//
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
