package org.ltc.hitalk.interpreter;

import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.IFunctor;

/**
 *
 */
public
class DcgRule extends HtClause {

    /**
     * @param head
     * @param body
     */
    public DcgRule ( IFunctor head, IFunctor[] body ) {
        super(null, head, body);
    }
}
