package org.ltc.hitalk.core;

import java.util.Deque;

/**
 * @param <T>
 */
public interface IQueueHolder<T extends IHitalkObject> {

    /**
     * @return
     */
    Deque <T> getQueue ();

    /**
     * @param item
     */
    default void push ( T item ) {
        getQueue().push(item);
    }

    /**
     *
     */
    default T poll () {
        return getQueue().poll();
    }

    /**
     * @return
     */
    default T pop () {
        return getQueue().pop();
    }
}
