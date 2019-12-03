package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.term.ITerm;

import java.util.List;
import java.util.function.Function;

/**
 * @param <T>
 */
public class Action<T extends ITerm> implements Function <T, List <T>> {


    public Action () {

    }

    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result
     */
    public List <T> apply ( T t ) {
        return null;
    }
}
