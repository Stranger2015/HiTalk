package org.ltc.hitalk.compiler.bktables;

import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.wam.task.TransformTask;

import java.util.List;

/**
 * @param <TT>
 */
public
interface IComposite<T extends Term, TT extends TransformTask <T>> {
    /**
     * @param t
     */
    default
    void add ( TT t ) {
        getComponents().add(t);
    }

    List <TT> getComponents ();
}
