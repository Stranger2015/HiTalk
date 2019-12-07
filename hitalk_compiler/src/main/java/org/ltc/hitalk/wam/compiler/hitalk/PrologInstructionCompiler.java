package org.ltc.hitalk.wam.compiler.hitalk;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.BaseInstructionCompiler;
import org.ltc.hitalk.wam.compiler.prolog.PrologDefaultBuiltIn;

/**
 *
 */
public abstract class PrologInstructionCompiler<T extends HtClause, PC, QC> extends BaseInstructionCompiler <T, PC, QC> {

    protected PrologDefaultBuiltIn defaultBuiltIn;

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable    The symbol table for the machine.
     * @param interner       The interner for the machine.
     * @param parser
     * @param defaultBuiltIn
     * @param observer
     */
    public PrologInstructionCompiler ( SymbolTable <Integer, String, Object> symbolTable,
                                       IVafInterner interner,
                                       PrologDefaultBuiltIn defaultBuiltIn,
                                       LogicCompilerObserver <PC, QC> observer,
                                       PlPrologParser parser ) {
        super(symbolTable, interner, defaultBuiltIn, observer, parser);
    }

    @Override
    public void compile ( T clause, HtProperty... flags ) throws SourceCodeException {

    }

    /**
     * @param resolver
     */
    public void setResolver ( IResolver <PC, QC> resolver ) {

    }

    public void compile ( T clause ) throws SourceCodeException {
        compile(clause, new HtProperty[0]);
    }

    public void compileQuery ( QC query ) throws SourceCodeException {

    }
}
