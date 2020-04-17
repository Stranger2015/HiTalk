package org.ltc.hitalk.interpreter;

import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledQuery;

public
class HtProlog<T extends HtClause, P, Q, PC extends HiTalkWAMCompiledPredicate, QC extends HiTalkWAMCompiledQuery>
        extends TopLevel<T, P, Q, PC, QC> {
    /**
     * @param engine
     * @param interpreter
     * @param compiler
     */
    public HtProlog(HtResolutionEngine<T, P, Q, PC, QC> engine,
                    IInterpreter<T, P, Q, PC, QC> interpreter,
                    ICompiler<T, P, Q, PC, QC> compiler) {
        super(engine, interpreter, compiler);
    }
}
