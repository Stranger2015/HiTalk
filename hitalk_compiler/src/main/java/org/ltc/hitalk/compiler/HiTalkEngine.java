package org.ltc.hitalk.compiler;

import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.interpreter.HtResolutionEngine;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;

import java.util.function.Consumer;

/**
 *
 */
public
class HiTalkEngine<T extends HtClause, P, Q, PC, QC> extends HtResolutionEngine <T, P, Q, PC, QC> {

    private final IResolver <P, Q> resolver;

//    protected final Logger logger = Logger.getLogger(getClass().getSimpleName());

    /**
     * Builds an logical resolution engine from a parser, interner, compiler and resolver.
     *
     * @param parser   The parser.
     * @param interner The interner.
     * @param compiler
     */
    public HiTalkEngine ( HtPrologParser parser,
                          IVafInterner interner,
                          ICompiler <T, P, Q> compiler,
                          IResolver <P, Q> resolver ) {
        super(parser, interner, compiler);
        this.resolver = resolver;
    }

    /**
     * Resets the engine to its default state. This will typically load any bootstrapping libraries of built-ins that
     * the engine requires, but otherwise set its domain to empty.
     */
    @Override
    public void reset () {
//todo
    }

    //    @Override
    public void forEach ( Consumer action ) {

    }

    public IResolver <P, Q> getResolver () {
        return resolver;
    }
}
