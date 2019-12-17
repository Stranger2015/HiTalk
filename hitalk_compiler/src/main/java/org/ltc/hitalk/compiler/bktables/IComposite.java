package org.ltc.hitalk.compiler.bktables;

import org.ltc.hitalk.wam.task.PreCompilerTask;

import java.util.Deque;

/**
 *
 */
public
interface IComposite<T extends PreCompilerTask> {
    /**
     * @param t
     */
    default void add ( T t ) {
        getComponents().add(t);
    }

    Deque <T> getComponents ();
}
