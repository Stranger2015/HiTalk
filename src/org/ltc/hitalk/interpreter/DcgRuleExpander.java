package org.ltc.hitalk.interpreter;

import com.thesett.aima.logic.fol.Resolver;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.bktables.HiTalkFlag;
import org.ltc.hitalk.compiler.bktables.IApplication;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.wam.compiler.HiTalkDefaultBuiltIn;
import org.ltc.hitalk.wam.compiler.HiTalkPreCompiler;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 */
public
class DcgRuleExpander extends HiTalkPreCompiler <HtClause> {

    /**
     * Creates a new PreCompiler.
     *
     * @param symbolTable    The symbol table.
     * @param interner       The machine to translate functor and variable names.
     * @param defaultBuiltIn The default built in, for standard compilation and interners and symbol tables.
     * @param app
     */
    public
    DcgRuleExpander ( SymbolTable <Integer, String, Object> symbolTable,
                      VariableAndFunctorInterner interner,
                      HiTalkDefaultBuiltIn defaultBuiltIn,
                      IApplication app ) {
        super(symbolTable, interner, defaultBuiltIn, app);
    }

    /**
     * {@inheritDoc}
     *
     * @param sentence
     */
    @Override
    public
    void compile ( Sentence <HtClause> sentence ) throws SourceCodeException {
//todo
    }

    /**
     * @param clauses
     */
    @Override
    protected
    void saveResult ( List <HtClause> clauses ) {
//todo
    }

    /**
     * @param clause
     * @return
     */
    @Override
    protected
    List <HtClause> preprocess ( HtClause clause ) {
        List <HtClause> l = new ArrayList <>();
        //todo
        return l;
    }

    /**
     * @return
     */
    @Override
    public
    Logger getConsole () {
//        todo
        return null;
    }

    /**
     * @return
     */
    @Override
    public
    HtPrologParser getParser () {
        //        todo
        return null;
    }

    /**
     * @param sentence
     * @param flags
     * @throws SourceCodeException
     */
    @Override
    public
    void compile ( HtClause sentence, HiTalkFlag... flags ) throws SourceCodeException {
//        todo
    }

    /**
     * @param rule
     */
    @Override
    public
    void compileDcgRule ( DcgRule rule ) throws SourceCodeException {
//todo
    }

    /**
     * @param query
     */
    @Override
    public
    void compileQuery ( HtClause query ) throws SourceCodeException {
//todo
    }

    /**
     * @param clause
     */
    @Override
    public
    void compileClause ( HtClause clause ) {
//todo
    }

    /**
     * @param resolver
     */
    @Override
    public
    void setResolver ( Resolver <HtClause, HtClause> resolver ) {

    }
}


