package org.ltc.hitalk.compiler.bktables;

import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.task.TransformTask;

import java.util.List;

/**
 *
 */
public
interface IComposite<T extends HtClause, TC extends Term, TT extends TransformTask <T, TC>> {
    /**
     * @param t
     */
    default
    void add ( TT t ) {
        getComponents().add(t);
    }

    List <TT> getComponents ();
}
