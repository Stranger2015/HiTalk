package org.ltc.hitalk.compiler;

import com.thesett.aima.logic.fol.Resolver;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.interpreter.HtResolutionEngine;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;

import java.util.function.Consumer;

/**
 *
 */
public
class HiTalkEngine<T extends HtClause, P, Q> extends HtResolutionEngine <T, P, Q> {

//    protected final Logger logger = Logger.getLogger(getClass().getSimpleName());

    /**
     * Builds an logical resolution engine from a parser, interner, compiler and resolver.
     *
     * @param parser   The parser.
     * @param interner The interner.
     * @param compiler
     */
    public HiTalkEngine ( PlPrologParser parser,
                          VariableAndFunctorInterner interner,
                          ICompiler <T, P, Q> compiler,
                          Resolver <P, Q> resolver ) {
        super(parser, interner, compiler, resolver);
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
}
