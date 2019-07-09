package org.ltc.hitalk.wam.transformers;

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.IMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Created by Anthony on 19.07.2016.
 */
public
class DefaultTransformer<T extends Clause> implements ITransformer <T> {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getName());
    private final List <? extends Clause> target;

    protected ExecutionContext context;
    protected ExecutionContext contextBefore;
    //
    protected ExecutionInfo executionInfo;
    protected ExecutionInfo executionInfoBefore;
    protected final ITransformer <T> transformer;
    private TransformInfo result;

    /**
     * @param term
     */
    public
    DefaultTransformer ( T term ) {
        target = Collections.singletonList(term);
        this.transformer = new <T>ZeroTransformer <T>();//todo wtf??
    }

    /**
     * @param target
     */
    public
    DefaultTransformer ( List <T> target ) {

        this.target = target;
        transformer = this;
    }

    @Override
    public
    void cancel () {
        //todo
    }

    public
    ITransformer <T> getTransformer () {
        return transformer;
    }

    /**
     *
     */
    @Override
    public
    void message () {
        logger.info("Default transformer is launched ...");
    }

    /**
     * @param target
     * @return
     */
    public
    T transform ( Clause target ) {
        message();
        List <T> termList = Collections.emptyList();// execute();
        IMetrics after = context.getCurrentMetrics();
        IMetrics delta = after.subtract(contextBefore.getCurrentMetrics());
        if (termList.size() > 0) {
            executionInfo = null;// todo????
        }
        if (!isAcceptable(context.getMaxMetrics())) {
            result = new TransformInfo(contextBefore, executionInfoBefore, null, target); //todo
            cancel();
        }
        else {
            result = new TransformInfo(context, executionInfo, delta, target);
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

    /**
     * @param t
     * @return
     */
    @Override
    public
    T transform ( Term t ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    void run () {
        result = transform(null);
    }
}
