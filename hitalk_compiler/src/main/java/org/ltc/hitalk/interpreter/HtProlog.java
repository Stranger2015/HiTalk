package org.ltc.hitalk.interpreter;

import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.parser.HtClause;

public
class HtProlog<T extends HtClause, P, Q> extends TopLevel <T, P, Q> {
    /**
     * @param engine
     * @param interpreter
     * @param compiler
     */
    public HtProlog ( HtResolutionEngine <T, P, Q> engine, IInterpreter <P, Q> interpreter, ICompiler <P, Q> compiler ) {
        super(engine, interpreter, compiler);
    }
}
