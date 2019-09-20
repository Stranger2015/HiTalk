package org.ltc.hitalk.interpreter;

import com.thesett.aima.logic.fol.Functor;
import org.ltc.hitalk.parser.HtClause;

/**
 *
 */
public
class DcgRule extends HtClause {

    /**
     * @param head
     * @param body
     */
    public
    DcgRule ( Functor head, Functor[] body ) {
        super(head, body);
    }
}
