package org.ltc.hitalk.compiler.bktables;

import com.thesett.aima.logic.fol.Clause;
import org.ltc.hitalk.wam.task.TransformTask;

import java.util.List;

/**
 *
 */
public
interface IComposite<T extends Clause> {
    /**
     * @param t
     */
    default
    void add ( TransformTask <T> t ) {
        getComponents().add(t);
    }

    List <TransformTask <T>> getComponents ();
}
