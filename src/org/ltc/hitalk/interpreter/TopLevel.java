package org.ltc.hitalk.interpreter;

import org.ltc.hitalk.parser.HtClause;

/**
 *
 */
public abstract
class TopLevel<T,Q> {
    private final HtResolutionEngine<T,Q> engine;
    private final IInterpreter interpreter;
    private final ICompiler<HtClause,T,Q> compiler;

    /**
     * @param engine
     * @param interpreter
     * @param compiler
     */
    public
    TopLevel ( HtResolutionEngine<T,Q> engine, IInterpreter interpreter, ICompiler<HtClause,T,Q> compiler ) {

        this.engine = engine;
        this.interpreter = interpreter;
        this.compiler = compiler;
    }
}
