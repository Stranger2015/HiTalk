package org.ltc.hitalk.interpreter;

public
class HtProlog<T,Q> extends TopLevel<T,Q> {
    /**
     * @param engine
     * @param interpreter
     * @param compiler
     */
    public
    HtProlog ( HtResolutionEngine <T, Q> engine, IInterpreter interpreter, ICompiler compiler ) {
        super(engine, interpreter, compiler);
    }
}
