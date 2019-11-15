package org.ltc.hitalk.wam.task;


import org.ltc.hitalk.term.ITerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Function;

import static java.lang.String.format;

/**
 *
 */
abstract public
class CompilerTask<T extends ITerm> implements IInvokable <T> {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    protected final Function <T, List <T>> action;

    /**
     *
     */
    protected
    CompilerTask ( Function <T, List <T>> action ) {
        this.action = action;
    }


    /**
     * @param t
     * @return
     */
    @Override
    public
    List <T> invoke ( T t ) {
        List <T> list = IInvokable.super.invoke(t);
        for (int i = 0; i < list.size(); i++) {
            T t1 = list.get(i);
            list.addAll(action.apply(t1));
        }
        return list;
    }

    /**
     *
     */
    public
    void banner () {
        logger.info(format("\nPerforming %s task ...", getClass().getSimpleName()));
    }
}
