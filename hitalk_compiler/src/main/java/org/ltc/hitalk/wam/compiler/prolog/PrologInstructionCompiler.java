package org.ltc.hitalk.wam.compiler.prolog;

import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtSourceCodeException;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.BaseInstructionCompiler;

import static org.ltc.hitalk.core.BaseApp.getAppContext;

/**
 *
 */
public class PrologInstructionCompiler<T extends HtClause, PC, QC> extends BaseInstructionCompiler <T, PC, QC> {

    public PrologDefaultBuiltIn getDefaultBuiltIn () {
        return defaultBuiltIn;
    }

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
    public PrologInstructionCompiler ( ISymbolTable <Integer, String, Object> symbolTable,
                                       IVafInterner interner,
                                       PrologDefaultBuiltIn defaultBuiltIn,
                                       ICompilerObserver <PC, QC> observer,
                                       PlPrologParser parser ) {
        super(symbolTable, interner, defaultBuiltIn, observer, parser);
    }

    public PrologInstructionCompiler () {
        this(getAppContext().getSymbolTable(),
                getAppContext().getInterner(),
                getAppContext().getDefaultBuiltIn(),
                getAppContext().getObserverIC(),
                getAppContext().getParser());
    }

    @Override
    public void compile ( T clause, HtProperty... flags ) throws HtSourceCodeException {
        logger.info("Compiling (" + clause + ")");

    }

    /**
     * @param resolver
     */
    public void setResolver ( IResolver <PC, QC> resolver ) {
        this.resolver = resolver;
    }

    public void compile ( T clause ) throws HtSourceCodeException {
        compile(clause, new HtProperty[0]);
    }

    public void compileQuery ( QC query ) {

    }

    public void toString0 ( StringBuilder sb ) {
    }
}
