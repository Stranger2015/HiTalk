package org.ltc.hitalk.core;

import java.util.Deque;

/**
 * @param <T>
 */
public interface IQueueHolder<T extends IHitalkObject> {

    /**
     * @return
     */
    Deque<T> getTaskQueue();

    /**
     * @param item
     */
    default void push ( T item ) {
        getTaskQueue().push(item);
    }

    /**
     *
     */
    default T poll () {
        return getTaskQueue().poll();
    }

    /**
     * @return
     */
    default T pop () {
        return getTaskQueue().pop();
    }
}
