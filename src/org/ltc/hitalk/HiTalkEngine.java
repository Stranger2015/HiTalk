package org.ltc.hitalk;

import com.thesett.aima.logic.fol.LogicCompiler;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import org.ltc.hitalk.interpreter.HtResolutionEngine;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;

/**
 *
 */
public
class HiTalkEngine extends HtResolutionEngine <HtClause, HtClause> {

    /**
     * Builds an logical resolution engine from a parser, interner, compiler and resolver.
     * @param parser   The parser.
     * @param interner The interner.
     * @param compiler
     */
    public
    HiTalkEngine ( HtPrologParser parser,
                   VariableAndFunctorInterner interner,
                   LogicCompiler <HtClause, HtClause, HtClause> compiler ) {
        super(parser, interner, compiler);
    }

    /**
     * Resets the engine to its default state. This will typically load any bootstrapping libraries of built-ins that
     * the engine requires, but otherwise set its domain to empty.
     */
    @Override
    public
    void reset () {

    }
}
