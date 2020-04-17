package org.ltc.hitalk.interpreter;

import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledQuery;
import org.ltc.hitalk.wam.machine.HiTalkWAMResolvingMachine;

/**
 *
 */
public abstract
class TopLevel<T extends HtClause, P, Q, PC extends HiTalkWAMCompiledPredicate, QC extends HiTalkWAMCompiledQuery> {
    HiTalkWAMResolvingMachine<PC, QC> engine;
    private final IInterpreter<T, P, Q, PC, QC> interpreter;
    private final ICompiler<T, P, Q, PC, QC> compiler;

    /**
     * @param engine
     * @param interpreter
     * @param compiler
     */
    public TopLevel(HiTalkWAMResolvingMachine<PC, QC> engine,
                    IInterpreter<T, P, Q, PC, QC> interpreter,
                    ICompiler<T, P, Q, PC, QC> compiler) {

        this.engine = engine;
        this.interpreter = interpreter;
        this.compiler = compiler;
    }

    public IInterpreter getInterpreter() {
        return interpreter;
    }

    public ICompiler<T, P, Q, PC, QC> getCompiler() {
        return compiler;
    }

    public HiTalkWAMResolvingMachine<PC, QC> getEngine() {
        return engine;
    }
}
