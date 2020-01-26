package org.ltc.hitalk.wam.compiler.prolog;

import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtSourceCodeException;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.BaseInstructionCompiler;

import java.util.ArrayList;
import java.util.List;

import static org.ltc.hitalk.core.BaseApp.getAppContext;

/**
 *
 */
public class PrologInstructionCompiler<T extends HtClause, PC, QC> extends BaseInstructionCompiler <T, PC, QC> {

    private int clauseNumber = 0;

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

    public PrologInstructionCompiler() throws Exception {
        this(getAppContext().getSymbolTable(),
                getAppContext().getInterner(),
                getAppContext().getDefaultBuiltIn(),
                getAppContext().getObserverIC(),
                getAppContext().getParser());
    }

    /**
     * @param fnl
     * @throws Exception
     * @throws HtSourceCodeException
     */
    public List <HtClause> compileFiles ( List <String> fnl ) throws Exception {
        return null;
    }

    public List <HtClause> compileFiles ( List <String> fnl, HtProperty... flags ) throws Exception {
        final List <HtClause> list = new ArrayList <>();
        return list;
    }

    /**
     * @param fn
     * @param flags
     * @throws Exception
     */
    public List <HtClause> compileFile ( String fn, HtProperty... flags ) throws Exception {
        final List <HtClause> list = new ArrayList <>();
        return list;
    }

    //    @Override
    public List<HtClause> compile(PlLexer tokenSource, HtProperty... flags) throws HtSourceCodeException {
        logger.info("Precompiling (" + tokenSource.getPath() + ") ...");
//        clauseNumber

//        final HiTalkWAMCompiledPredicate predicate = new HiTalkWAMCompiledPredicate(clause.getHead().getName());
//        compileClause(clause, (PC) predicate, true, true, true, 0);
        final List<HtClause> list = new ArrayList<>();
        return list;
    }

    /**
     * @param clause
     * @param flags
     * @throws HtSourceCodeException
     */
    public void compile ( T clause, HtProperty... flags ) throws HtSourceCodeException {

    }

    /**
     * @param resolver
     */
    public void setResolver ( IResolver <PC, QC> resolver ) {
        this.resolver = resolver;
    }

    /**
     * @param clause
     * @throws HtSourceCodeException
     */
    @Override
    public void compile ( HtClause clause ) throws Exception {
        compile((T) clause, new HtProperty[0]);
    }

    public void compileQuery ( QC query ) {

    }

    public void toString0 ( StringBuilder sb ) {
    }
}
