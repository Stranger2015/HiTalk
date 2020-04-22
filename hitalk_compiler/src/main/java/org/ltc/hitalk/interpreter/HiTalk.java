package org.ltc.hitalk.interpreter;

import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledQuery;
import org.ltc.hitalk.wam.compiler.prolog.PrologInterpreter;
import org.ltc.hitalk.wam.task.PreCompilerTask;

/**
 *
 */
public
class HiTalk<T extends HtClause, P, Q,
        PC extends HiTalkWAMCompiledPredicate,
        QC extends HiTalkWAMCompiledQuery>
        extends TopLevel<T, P, Q, PC, QC> {

    /**
     *
     */
    public HiTalk(HtResolutionEngine<T, PreCompilerTask<T>, P, Q, PC, QC> engine,
                  PrologInterpreter<T, P, Q, PC, QC> interpreter,
                  ICompiler<T, P, Q, PC, QC> compiler) {

        super(engine, interpreter, compiler);
    }
}
