package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.term.ITerm;

import java.util.List;
import java.util.function.Function;

/**
 *
 */
public class Action implements Function <ITerm, List <ITerm>> {


    public Action () {

    }

    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result
     */
    public List <ITerm> apply ( ITerm t ) {
        return null;
    }
}
