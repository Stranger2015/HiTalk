package org.ltc.hitalk.interpreter;

import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.parser.HtClause;

public
class HtProlog<T extends HtClause, P, Q, PC, QC> extends TopLevel <T, P, Q, PC, QC> {
    /**
     * @param engine
     * @param interpreter
     * @param compiler
     */
    public HtProlog ( HtResolutionEngine <T, P, Q, PC, QC> engine,
                      IInterpreter <T, P, Q> interpreter,
                      ICompiler <T, P, Q> compiler ) {
        super(engine, interpreter, compiler);
    }
}
