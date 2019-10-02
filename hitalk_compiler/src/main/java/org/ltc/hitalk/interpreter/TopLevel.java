package org.ltc.hitalk.interpreter;

import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.parser.HtClause;

/**
 *
 */
public abstract
class TopLevel<T extends HtClause, P, Q> {
    private final HtResolutionEngine <T, P, Q> engine;
    private final IInterpreter <P, Q> interpreter;
    private final ICompiler <T, P, Q> compiler;

    /**
     * @param engine
     * @param interpreter
     * @param compiler
     */
    public
    TopLevel ( HtResolutionEngine <T, P, Q> engine, IInterpreter <P, Q> interpreter, ICompiler <T, P, Q> compiler ) {

        this.engine = engine;
        this.interpreter = interpreter;
        this.compiler = compiler;
    }

    public
    IInterpreter getInterpreter () {
        return interpreter;
    }

    public
    ICompiler <T, P, Q> getCompiler () {
        return compiler;
    }

    public
    HtResolutionEngine <T, P, Q> getEngine () {
        return engine;
    }
}
