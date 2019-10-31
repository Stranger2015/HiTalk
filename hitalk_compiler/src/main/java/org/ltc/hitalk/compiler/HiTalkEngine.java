package org.ltc.hitalk.compiler;

import com.thesett.aima.logic.fol.Variable;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.interpreter.DcgRule;
import org.ltc.hitalk.interpreter.HtResolutionEngine;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;

import java.util.Set;
import java.util.function.Consumer;

/**
 *
 */
public
class HiTalkEngine extends HtResolutionEngine {

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
                          ICompiler <HtClause, HtPredicate, HtClause> compiler ) {
        super(parser, interner, compiler);
//        compiler.setResolver(this);
    }

    /**
     * Resets the engine to its default state. This will typically load any bootstrapping libraries of built-ins that
     * the engine requires, but otherwise set its domain to empty.
     */
    @Override
    public void reset () {
//todo
    }

    /**
     * @param sentence
     * @param flags
     * @throws SourceCodeException
     */
    @Override
    public void compile ( HtClause sentence, HtProperty... flags ) throws SourceCodeException {
//todo
    }

    /**
     * @param rule
     */
    @Override
    public void compileDcgRule ( DcgRule rule ) throws SourceCodeException {
//todo
    }

    /**
     * @param query
     */
    @Override
    public void compileQuery ( HtClause query ) {
//todo
    }

    /**
     * @param clause
     */
    @Override
    public void compileClause ( HtClause clause ) {
//todo
    }

    //    @Override
    public void forEach ( Consumer <? super Set <Variable>> action ) {

    }
}
