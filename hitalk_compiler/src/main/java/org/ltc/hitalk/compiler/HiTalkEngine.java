package org.ltc.hitalk.compiler;

import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.compiler.bktables.Flag;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.interpreter.DcgRule;
import org.ltc.hitalk.interpreter.HtResolutionEngine;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;

/**
 *
 */
public
class HiTalkEngine<T extends HtClause, P, Q> extends HtResolutionEngine <T, P, Q> {

    /**
     * Builds an logical resolution engine from a parser, interner, compiler and resolver.
     *
     * @param parser   The parser.
     * @param interner The interner.
     * @param compiler
     */
    public
    HiTalkEngine ( HtPrologParser <T> parser,
                   VariableAndFunctorInterner interner,
                   ICompiler <T, P, Q> compiler ) {
        super(parser, interner, compiler);
        compiler.setResolver(this);
    }

    /**
     * Resets the engine to its default state. This will typically load any bootstrapping libraries of built-ins that
     * the engine requires, but otherwise set its domain to empty.
     */
    @Override
    public
    void reset () {
//todo
    }

    /**
     * @param sentence
     * @param flags
     * @throws SourceCodeException
     */
    @Override
    public
    void compile ( HtClause sentence, Flag... flags ) throws SourceCodeException {
//todo
    }

    /**
     * @param rule
     */
    @Override
    public
    void compileDcgRule ( DcgRule rule ) throws SourceCodeException {
//todo
    }

    /**
     * @param query
     */
    @Override
    public
    void compileQuery ( HtClause query ) {
//todo
    }

    /**
     * @param clause
     */
    @Override
    public
    void compileClause ( HtClause clause ) {
//todo
    }
}
