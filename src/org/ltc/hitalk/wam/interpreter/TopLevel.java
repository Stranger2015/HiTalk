package org.ltc.hitalk.wam.interpreter;

import com.thesett.aima.logic.fol.interpreter.ResolutionEngine;

/**
 *
 */
public abstract
class TopLevel {
    private final ResolutionEngine engine;
    private final IInterpreter interpreter;
    private final ICompiler compiler;

    /**
     * @param engine
     * @param interpreter
     * @param compiler
     */
    public
    TopLevel ( ResolutionEngine engine, IInterpreter interpreter, ICompiler compiler ) {

        this.engine = engine;
        this.interpreter = interpreter;
        this.compiler = compiler;
    }
}
