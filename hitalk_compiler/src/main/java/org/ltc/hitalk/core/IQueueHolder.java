package org.ltc.hitalk.core;

import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.task.PreCompilerTask;

import java.util.Deque;

/**
 * @param <T>
 */
public interface IQueueHolder<T extends HtClause, TT extends PreCompilerTask<T>> {

    /**
     * @return
     */
    Deque<TT> getTaskQueue();

    /**
     * @param item
     */
    default void push(TT item) {
        getTaskQueue().push(item);
    }

    /**
     * @return
     */
    default TT poll() {
        return getTaskQueue().poll();
    }

    /**
     * @return
     */
    default TT pop() {
        return getTaskQueue().pop();
    }
}
