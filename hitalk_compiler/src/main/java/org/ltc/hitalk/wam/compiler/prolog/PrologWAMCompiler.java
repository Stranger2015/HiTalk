package org.ltc.hitalk.wam.compiler.prolog;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.Resolver;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.BaseCompiler;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.interpreter.DcgRule;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledQuery;
import org.slf4j.Logger;

public class PrologWAMCompiler extends BaseCompiler <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> implements ICompiler <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> {

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     */
    public PrologWAMCompiler ( SymbolTable <Integer, String, Object> symbolTable, VariableAndFunctorInterner interner, HtPrologParser parser ) {
        super(symbolTable, interner, parser);
    }

    @Override
    public Logger getConsole () {
        return logger;
    }

    @Override
    public HtPrologParser getParser () {
        return parser;
    }

    @Override
    public void compile ( HtClause clause, HtProperty... flags ) throws SourceCodeException {

    }

    @Override
    public void compileDcgRule ( DcgRule rule ) throws SourceCodeException {

    }

    @Override
    public void compileQuery ( HtClause query ) throws SourceCodeException {

    }

    @Override
    public void compileClause ( HtClause clause ) {

    }

    @Override
    public void setResolver ( Resolver <HtClause, HiTalkWAMCompiledQuery> resolver ) {

    }

    @Override
    public void compile ( Sentence <HtClause> sentence ) throws SourceCodeException {

    }

    @Override
    public void setCompilerObserver ( LogicCompilerObserver <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> observer ) {

    }

    @Override
    public void endScope () throws SourceCodeException {

    }
}
