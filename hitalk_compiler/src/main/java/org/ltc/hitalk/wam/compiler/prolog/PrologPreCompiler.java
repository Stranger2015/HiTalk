package org.ltc.hitalk.wam.compiler.prolog;

import com.thesett.aima.logic.fol.Resolver;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.bytecode.BaseMachine;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.PrologBuiltInTransform;
import org.ltc.hitalk.compiler.bktables.IApplication;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
abstract public
class PrologPreCompiler extends BaseMachine implements ICompiler <HtClause, HtPredicate, HtClause> {
    final protected Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    final protected PlPrologParser parser;
    final protected PrologDefaultBuiltIn defaultBuiltIn;
    /**
     * Holds the built in transformation.
     */
    protected final PrologBuiltInTransform <IApplication, Term> builtInTransform;
    final protected Resolver <HtClause, HtClause> resolver;
    protected final PrologWAMCompiler compiler;

    /**
     * @param symbolTable
     * @param interner
     * @param defaultBuiltIn
     * @param builtInTransform
     * @param resolver
     * @param compiler
     */
    public PrologPreCompiler ( SymbolTable <Integer, String, Object> symbolTable,
                               VariableAndFunctorInterner interner,
                               PrologDefaultBuiltIn defaultBuiltIn,
                               PrologBuiltInTransform <IApplication, Term> builtInTransform,
                               Resolver <HtClause, HtClause> resolver,
                               PlPrologParser parser,
                               PrologWAMCompiler compiler ) {
        super(symbolTable, interner);
        this.defaultBuiltIn = defaultBuiltIn;
        this.builtInTransform = builtInTransform;
        this.resolver = resolver;
        this.parser = parser;
        this.compiler = compiler;
    }

    @Override
    public Logger getConsole () {
        return logger;
    }

    @Override
    public PlPrologParser getParser () {
        return parser;
    }

//    @Override
//    public void compile ( HtClause clause, HtProperty... flags ) throws SourceCodeException {
//
//    }
//
//    @Override
//    public void compileDcgRule ( DcgRule rule ) throws SourceCodeException {
//
//    }

//    @Override
//    public void compileQuery ( HtClause query ) throws SourceCodeException {
//
//    }

//    @Override
//    public void compileClause ( HtClause clause ) {
//
//    }

//    @Override
//    public void compile ( Sentence <HtClause> sentence ) throws SourceCodeException {
//
//    }

//    @Override
//    public void endScope () throws SourceCodeException {
//
//    }
}