package org.ltc.hitalk.compiler.bktables;

import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.task.TransformTask;

import java.util.List;

/**
 *
 */
public
interface IComposite<TC extends ITerm, TT extends TransformTask <TC>> {
    /**
     * @param t
     */
    default void add ( TT t ) {
        getComponents().add(t);
    }

    List <TT> getComponents ();
}
