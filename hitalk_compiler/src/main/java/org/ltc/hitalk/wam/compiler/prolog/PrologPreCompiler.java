package org.ltc.hitalk.wam.compiler.prolog;

import com.thesett.aima.logic.fol.Resolver;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.bytecode.BaseMachine;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.PrologBuiltInTransform;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
abstract public class PrologPreCompiler<T extends HtClause, P, Q>
        extends BaseMachine implements ICompiler <T, P, Q> {

    protected Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    protected PlPrologParser parser;
    protected PrologDefaultBuiltIn defaultBuiltIn;
    /**
     * Holds the built in transformation.
     */
    protected final PrologBuiltInTransform/*<?,?>*/ builtInTransform;
    protected Resolver <HtClause, Q> resolver;

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     */
    protected PrologPreCompiler ( SymbolTable <Integer, String, Object> symbolTable,
                                  VariableAndFunctorInterner interner,
                                  PrologBuiltInTransform/*<?,?>*/ builtInTransform
    ) {
        super(symbolTable, interner);
        this.builtInTransform = builtInTransform;
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

    @Override
    public void setResolver ( Resolver <HtClause, Q> resolver ) {
        this.resolver = resolver;
    }

//    @Override
//    public void compile ( Sentence <HtClause> sentence ) throws SourceCodeException {
//
//    }

//    @Override
//    public void endScope () throws SourceCodeException {
//
//    }
}