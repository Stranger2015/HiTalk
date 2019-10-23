package org.ltc.hitalk.compiler;

import com.thesett.aima.logic.fol.*;
import com.thesett.aima.logic.fol.bytecode.BaseMachine;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolKey;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.interpreter.DcgRule;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlTokenSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static com.thesett.aima.logic.fol.wam.compiler.SymbolTableKeys.SYMKEY_PREDICATES;

/**
 * @param <P>
 * @param <Q>
 */
abstract public class BaseCompiler<P, Q> extends BaseMachine implements ICompiler <P, Q> {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    protected SymbolTable <Integer, String, Object> scopeTable;
    protected int scope;
    protected Deque <SymbolKey> predicatesInScope = new ArrayDeque <>();
    protected LogicCompilerObserver <P, Q> observer;
    protected PlPrologParser parser;
    protected Resolver <HtClause, Q> resolver;
    protected Resolver <HtClause, HtClause> resolver2;
//    protected BaseInstructionCompiler <P, Q> instructionCompiler;
//    protected PrologPreCompiler preCompiler;

//    /**
//     * @return
//     */
//    public PrologPreCompiler getPreCompiler () {
//        return preCompiler;
//    }


    /**
     * @param symbolTable
     * @param interner
     * @param parser
     */
    protected BaseCompiler ( SymbolTable <Integer, String, Object> symbolTable,
                             VariableAndFunctorInterner interner, PlPrologParser parser ) {
        super(symbolTable, interner);
        this.parser = parser;
    }

    @Override
    public void compile ( Sentence <HtClause> sentence ) throws SourceCodeException {
        logger.debug("public WAMCompiledClause compile(Sentence<HtClause> sentence = " + sentence + "): called");

        // Extract the clause to compile from the parsed sentence.
        HtClause clause = sentence.getT();

        // Classify the sentence to compile by the different sentence types in the language.
        if (clause.isQuery()) {
            compileQuery(clause);
        } else {
            // Initialize a nested symbol table for the current compilation scope, if it has not already been.
            if (scopeTable == null) {
                scopeTable = symbolTable.enterScope(scope);
            }

            // Check in the symbol table, if a compiled predicate with name matching the program clause exists, and if
            // not create it.
            SymbolKey predicateKey = scopeTable.getSymbolKey(clause.getHead().getName());
            List <HtClause> clauses = (List <HtClause>) scopeTable.get(predicateKey, SYMKEY_PREDICATES);

            if (clauses == null) {
                clauses = new ArrayList <>();
                scopeTable.put(predicateKey, SYMKEY_PREDICATES, clauses);
                predicatesInScope.offer(predicateKey);
            }

            // Add the clause to compile to its parent predicate for compilation at the end of the current scope.
            clauses.add(clause);
        }
    }

    /**
     * Establishes an observer on the compiled forms that the compiler outputs.
     *
     * @param observer The compiler output observer.
     */
    @Override
    public void setCompilerObserver ( LogicCompilerObserver <P, Q> observer ) {
        this.observer = observer;
    }

    /**
     * @param resolver2
     */
    public void setResolver2 ( Resolver <HtClause, HtClause> resolver2 ) {
        this.resolver2 = resolver2;
    }

    public Resolver <HtClause, Q> getResolver () {
        return resolver;
    }

    public Resolver <HtClause, HtClause> getResolver2 () {
        return resolver2;
    }

    @Override
    public Logger getConsole () {
        return logger;
    }

    @Override
    public PlPrologParser getParser () {
        return parser;
    }

    @Override
    public void compileDcgRule ( DcgRule rule ) throws SourceCodeException {

    }

    @Override
    public void compileClause ( HtClause clause ) {

    }

    @Override
    public void setResolver ( Resolver <HtClause, Q> resolver ) {
        this.resolver = resolver;
    }

    @Override
    public void compile ( String fileName, HtProperty[] flags ) throws IOException, SourceCodeException {
        PlTokenSource ts = PlTokenSource.getTokenSourceForIoFile(new File(fileName));
        compile(ts, flags);
    }

    @Override
    public void compile ( PlTokenSource tokenSource, HtProperty... flags ) throws IOException, SourceCodeException {
        getConsole().info("Compiling " + tokenSource.getPath() + "... ");
        parser.setTokenSource(tokenSource);
        while (true) {
            Term t = parser.next();
            if (t == null) {
                break;
            }
            HtClause c = parser.convert(t);
            compile(c, flags);
        }
    }

    /**
     * Chains compilation completion events onto the instruction compiler.
     */
    public class ClauseChainObserver implements LogicCompilerObserver <HtClause, HtClause> {
        /**
         * {@inheritDoc}
         */
        public void onCompilation ( Sentence <HtClause> sentence ) throws SourceCodeException {
            BaseCompiler.this.instructionCompiler.compile(sentence);
        }

        /**
         * {@inheritDoc}
         */
        public void onQueryCompilation ( Sentence <HtClause> sentence ) throws SourceCodeException {

        }
    }
}
