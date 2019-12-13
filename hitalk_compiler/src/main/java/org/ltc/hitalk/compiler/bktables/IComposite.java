package org.ltc.hitalk.compiler.bktables;

import org.ltc.hitalk.wam.task.CompilerTask;

import java.util.Deque;

/**
 *
 */
public
interface IComposite<T extends CompilerTask> {
    /**
     * @param t
     */
    default void add ( T t ) {
        getComponents().add(t);
    }

    Deque <T> getComponents ();
}
