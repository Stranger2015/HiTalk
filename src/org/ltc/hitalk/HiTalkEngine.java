package org.ltc.hitalk;

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.LogicCompiler;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.isoprologparser.ClauseParser;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledQuery;
import org.ltc.hitalk.wam.machine.HiTalkWAMEngine;

/**
 *
 */
public
class HiTalkEngine extends HiTalkWAMEngine {

    /**
     * Builds an logical resolution engine from a parser, interner, compiler and resolver.
     *
     * @param parser   The parser.
     * @param interner The interner.
     * @param compiler The compiler.
     */
    public
    HiTalkEngine ( ClauseParser parser, VariableAndFunctorInterner interner, LogicCompiler <Clause, HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> compiler ) {
        super(parser, interner, compiler);
    }
}
