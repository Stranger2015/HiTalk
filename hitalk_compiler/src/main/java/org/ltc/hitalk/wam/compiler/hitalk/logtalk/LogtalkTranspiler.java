package org.ltc.hitalk.wam.compiler.hitalk.logtalk;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.BaseCompiler;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;

public class LogtalkTranspiler extends BaseCompiler <HtClause, HtPredicate, HtClause> {
    /**
     * @param symbolTable
     * @param interner
     * @param parser
     * @param observer
     */
    protected LogtalkTranspiler ( SymbolTable <Integer, String, Object> symbolTable, VariableAndFunctorInterner interner, PlPrologParser parser, LogicCompilerObserver <HtPredicate, HtClause> observer ) {
        super(symbolTable, interner, parser, observer);
    }

    @Override
    public void compile ( HtClause clause, HtProperty... flags ) throws SourceCodeException {

    }

    @Override
    public void compileQuery ( HtClause query ) throws SourceCodeException {

    }

    @Override
    public void compileClause ( HtClause clause ) {

    }

    @Override
    public void endScope () throws SourceCodeException {

    }
}
