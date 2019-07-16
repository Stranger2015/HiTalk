package org.ltc.hitalk.compiler.bktables;

import com.thesett.aima.logic.fol.Clause;
import org.ltc.hitalk.wam.task.TransformTask;

import java.util.List;

/**
 *
 */
public
interface IComposite<T extends Clause, TT extends TransformTask <T>> {
    /**
     * @param t
     */
    default
    void add ( TT t ) {
        getComponents().add(t);
    }

    List <TT> getComponents ();
}
