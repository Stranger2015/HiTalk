package org.ltc.hitalk.wam.transformers;

import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.IMetrics;
import org.ltc.hitalk.parser.HtClause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Created by Anthony on 19.07.2016.
 */
public
class DefaultTransformer<T extends HtClause, TC extends Term> implements ITransformer <T, TC> {

    protected final static Logger logger = LoggerFactory.getLogger(DefaultTransformer.class);

    protected List <T> target;

    protected ExecutionContext context;
    protected ExecutionContext contextBefore;
    //
    protected ExecutionInfo executionInfo;
    protected ExecutionInfo executionInfoBefore;
    protected final ITransformer <T, TC> transformer;
    private TransformInfo <T> result;

    /**
     * @param term
     */
    public
    DefaultTransformer ( T term ) {
        target = Collections.singletonList(term);
        this.transformer = new <T>ZeroTransformer <T, TC>();//todo wtf??
    }

    /**
     * @param target
     */
    public
    DefaultTransformer ( List <T> target ) {

        this.target = target;
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

    public
    ITransformer <T, TC> getTransformer () {
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
    List <T> transform ( List <T> target ) {
        message();
        List <T> termList = Collections.emptyList();// execute();
        IMetrics after = context.getCurrentMetrics();
        IMetrics delta = after.subtract(contextBefore.getCurrentMetrics());
        if (termList.size() > 0) {
            executionInfo = null;// todo????
        }
        if (!isAcceptable(context.getMaxMetrics())) {
            result = new TransformInfo <>(contextBefore, executionInfoBefore, null, target); //todo
            cancel();
        }
        else {
            result = new TransformInfo <>(context, executionInfo, delta, target);
        }
        //
        return target;
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
    T transform ( T t ) {
        return t;
    }

    /**
     * @param t
     * @return
     */
    @Override
    public
    TC transform ( TC t ) {
        return t;
    }

    @Override
    public
    void run () {
        target = transform(getTarget());
    }

    private
    List <T> getTarget () {
        return target;
    }
}
