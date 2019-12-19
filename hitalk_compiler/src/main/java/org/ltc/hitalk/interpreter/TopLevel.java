package org.ltc.hitalk.interpreter;

import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.parser.HtClause;

/**
 *
 */
public abstract
class TopLevel<T extends HtClause, P, Q, PC, QC> {
    private final HtResolutionEngine <T, P, Q, PC, QC> engine;
    private final IInterpreter <T, P, Q> interpreter;
    private final ICompiler <T, P, Q> compiler;

    /**
     * @param engine
     * @param interpreter
     * @param compiler
     */
    public TopLevel ( HtResolutionEngine <T, P, Q, PC, QC> engine,
                      IInterpreter <T, P, Q> interpreter,
                      ICompiler <T, P, Q> compiler ) {

        this.engine = engine;
        this.interpreter = interpreter;
        this.compiler = compiler;
    }

    public IInterpreter getInterpreter () {
        return interpreter;
    }

    public ICompiler <T, P, Q> getCompiler () {
        return compiler;
    }

    public HtResolutionEngine <T, P, Q, PC, QC> getEngine () {
        return engine;
    }
}
