package org.ltc.hitalk.interpreter;

import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.prolog.PrologInterpreter;

/**
 *
 */
public
class HiTalk<T extends HtClause, P, Q> extends TopLevel <T, P, Q> {

    /**
     *
     */
    public HiTalk ( HtResolutionEngine <T, P, Q> engine,
                    PrologInterpreter <T, P, Q> interpreter,
                    ICompiler <T, P, Q> compiler ) {

        super(engine, interpreter, compiler);
    }
}
