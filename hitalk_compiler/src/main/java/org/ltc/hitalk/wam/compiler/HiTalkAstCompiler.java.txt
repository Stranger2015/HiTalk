package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.Resolver;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.bktables.Flag;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.interpreter.DcgRule;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public
class HiTalkAstCompiler<T extends HtClause, P, Q> /*extends HiLogAstCompiler <T, P, Q> */ implements ICompiler <T, P, Q> {

    protected final SymbolTable <Integer, String, Object> symbolTable;
    protected final HtPrologParser <T> parser;
    protected final VariableAndFunctorInterner interner;
    protected Resolver <T, Q> resolver;
    //    protected final DcgRuleExpander ruleExpander;
    protected static final Logger logger = LoggerFactory.getLogger(HiTalkAstCompiler.class);//  todo
    private LogicCompilerObserver <P, Q> observer;

    /**
     * @param symbolTable
     * @param interner
     * @param parser
     */
    public
    HiTalkAstCompiler (
            SymbolTable <Integer, String, Object> symbolTable,
            VariableAndFunctorInterner interner,
            HtPrologParser <T> parser
    ) {
        this.symbolTable = symbolTable;
        this.parser = parser;
        this.interner = interner;
        this.setResolver(resolver);
//        HiTalkDefaultBuiltIn defaultBuiltIn = new HiTalkDefaultBuiltIn(symbolTable, interner);
//        ruleExpander = new DcgRuleExpander(symbolTable, interner, defaultBuiltIn, app);
    }

    /**
     * @return
     */
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
    HtPrologParser <T> getParser () {
        return parser;
    }

    /**
     * /**
     *
     * @param clause
     * @param flags
     * @throws SourceCodeException
     */
    @Override
    public
    void compile ( HtClause clause, Flag... flags ) throws SourceCodeException {
// TODO
    }

    /**
     * @param rule
     * @throws SourceCodeException
     */
    @Override
    public
    void compileDcgRule ( DcgRule rule ) throws SourceCodeException {
        List <Sentence <T>> clauses = translate(rule);
        for (Sentence <T> clause : clauses) {
            compile(clause);
        }
    }

    /**
     * @param resolver
     */
    @Override
    public
    void setResolver ( Resolver <T, Q> resolver ) {
        this.resolver = resolver;
    }

    /**
     * @param rule
     * @return
     */
    private
    List <Sentence <T>> translate ( DcgRule rule ) {
        List <Sentence <T>> result = new ArrayList <>();

        return result;
    }

    /**
     * @param query
     */
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
    void compile ( Sentence <T> sentence ) throws SourceCodeException {
        T clause = sentence.getT();
        if (clause.isQuery()) {
            compileQuery(clause);
        }
        else if (clause.isDcgRule()) {
            compileDcgRule((DcgRule) clause);//fixme
        }
        else {
            compileClause(clause);
        }
    }

    /**
     * Establishes an observer on the compiled forms that the compiler outputs.
     *
     * @param observer The compiler output observer.
     */
    @Override
    public
    void setCompilerObserver ( LogicCompilerObserver <P, Q> observer ) {

        this.observer = observer;
    }

    /**
     * Signal the end of a compilation scope, to trigger completion of the compilation of its contents.
     *
     * @throws SourceCodeException If there is an error in the source to be compiled that prevents its compilation.
     */
    @Override
    public
    void endScope () throws SourceCodeException {
//todo
    }

    /**
     * @return
     */
    public
    SymbolTable <Integer, String, Object> getSymbolTable () {
        return symbolTable;
    }

    public
    LogicCompilerObserver <P, Q> getObserver () {
        return observer;
    }
}
