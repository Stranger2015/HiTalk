package org.ltc.hitalk.compiler.bktables;

import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.task.TransformTask;

import java.util.List;

/**
 *
 */
public
interface IComposite<T extends HtClause, TC extends ITerm, TT extends TransformTask <T, TC>> {
    /**
     * @param t
     */
    default
    void add ( TT t ) {
        getComponents().add(t);
    }

    List <TT> getComponents ();
}
