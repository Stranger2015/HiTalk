package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.bktables.HiTalkFlag;
import org.ltc.hitalk.interpreter.DcgRule;
import org.ltc.hitalk.interpreter.DcgRuleTranslator;
import org.ltc.hitalk.interpreter.HtResolutionEngine;
import org.ltc.hitalk.interpreter.ICompiler;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 */
public
class HiTalkAstCompiler implements ICompiler <HtClause, HtClause, HtClause> {

    protected final SymbolTable <Integer, String, Object> symbolTable;
    protected final HtPrologParser parser;
    protected final VariableAndFunctorInterner interner;
    protected final HtResolutionEngine <HtClause, HtClause> resolver;
    protected final DcgRuleTranslator translator;
    protected Logger logger;

    public
    HiTalkAstCompiler (
            SymbolTable <Integer, String, Object> symbolTable,
            VariableAndFunctorInterner interner,
            HtPrologParser parser,
            HtResolutionEngine <HtClause, HtClause> resolver ) {
        this.symbolTable = symbolTable;
        this.parser = parser;
        this.interner = interner;
        this.resolver = resolver;
        translator = new DcgRuleTranslator(symbolTable, interner, parser, );
    }

    @Override
    public
    Logger getConsole () {
        return logger;
    }

    /**
     * @return
     */
    @Override
    public
    HtPrologParser getParser () {
        return parser;
    }

    /**
     * @param sentence
     * @param flags
     * @throws SourceCodeException
     */
    @Override
    public
    void compile ( Sentence <HtClause> sentence, HiTalkFlag... flags ) throws SourceCodeException {

    }

    @Override
    public
    void compileDcgRule ( DcgRule rule ) throws SourceCodeException {
        List <HtClause> clauses = translate(rule);
        for (HtClause clause : clauses) {
            compile(clause);
        }
    }

    private
    List <HtClause> translate ( DcgRule rule ) {

        List <HtClause> l = new ArrayList <>();

        return l;
    }

    @Override
    public
    void compileQuery ( HtClause query ) {

    }

    @Override
    public
    void compileClause ( HtClause clause ) {

    }

    /**
     * Compiles a sentence into a (presumably binary) form, that provides a Java interface into the compiled structure.
     *
     * @param sentence The sentence to compile.
     * @throws SourceCodeException If there is an error in the source to be compiled that prevents its compilation.
     */
    @Override
    public
    void compile ( Sentence <HtClause> sentence ) throws SourceCodeException {
        HtClause clause = sentence.getT();
        if (clause.isQuery()) {
            compileQuery(clause);
        }
        else if (clause.isDcgRule()) {
            compileDcgRule((DcgRule) clause);//fixme
        }
    }

    /**
     * Establishes an observer on the compiled forms that the compiler outputs.
     *
     * @param observer The compiler output observer.
     */
    @Override
    public
    void setCompilerObserver ( LogicCompilerObserver <HtClause, HtClause> observer ) {

    }

    /**
     * Signal the end of a compilation scope, to trigger completion of the compilation of its contents.
     *
     * @throws SourceCodeException If there is an error in the source to be compiled that prevents its compilation.
     */
    @Override
    public
    void endScope () throws SourceCodeException {

    }

    public
    SymbolTable <Integer, String, Object> getSymbolTable () {
        return symbolTable;
    }
}
