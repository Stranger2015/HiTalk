package org.ltc.hitalk.wam.compiler.hitalk;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.BaseInstructionCompiler;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledQuery;
import org.ltc.hitalk.wam.compiler.prolog.PrologDefaultBuiltIn;

/**
 *
 */
public class PrologInstructionCompiler extends BaseInstructionCompiler {

    protected PrologDefaultBuiltIn defaultBuiltIn;

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     * @param parser
     * @param defaultBuiltIn
     * @param observer
     */
    public PrologInstructionCompiler ( SymbolTable <Integer, String, Object> symbolTable,
                                       IVafInterner interner,
                                       PrologDefaultBuiltIn defaultBuiltIn,
                                       LogicCompilerObserver <
                                               HiTalkWAMCompiledPredicate,
                                               HiTalkWAMCompiledQuery> observer,
                                       PlPrologParser parser ) {
        super(symbolTable, interner, defaultBuiltIn, observer, parser);
    }

    @Override
    public void compile ( HtClause clause, HtProperty... flags ) throws SourceCodeException {

    }

    public void compileQuery ( HiTalkWAMCompiledQuery query ) throws SourceCodeException {

    }

    /**
     * @param resolver
     */
    public void setResolver ( IResolver <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> resolver ) {

    }

    public void compile ( HtClause clause ) {

    }

    public void compileQuery ( HtClause query ) throws SourceCodeException {

    }
}
