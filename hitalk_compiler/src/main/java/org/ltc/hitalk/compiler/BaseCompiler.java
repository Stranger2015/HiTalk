package org.ltc.hitalk.compiler;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.Resolver;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.bytecode.BaseMachine;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.interpreter.DcgRule;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.wam.compiler.BaseInstructionCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @param <P>
 * @param <Q>
 */
public class BaseCompiler<P, Q> extends BaseMachine implements ICompiler <P, Q> {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    protected LogicCompilerObserver <P, Q> observer;
    protected HtPrologParser parser;
    protected Resolver <HtClause, Q> resolver;
    protected Resolver <HtClause, HtClause> resolver2;
    protected BaseInstructionCompiler <P, Q> instructionCompiler;
    protected BaseInstructionCompiler <P, Q> preCompiler;

    public BaseCompiler ( SymbolTable <Integer, String, Object> symbolTable, VariableAndFunctorInterner interner, HtPrologParser parser ) {
        super(symbolTable, interner);
        this.parser = parser;
    }

    @Override
    public void compile ( Sentence <HtClause> sentence ) throws SourceCodeException {

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
     * Signal the end of a compilation scope, to trigger completion of the compilation of its contents.
     *
     * @throws SourceCodeException If there is an error in the source to be compiled that prevents its compilation.
     */
    @Override
    public void endScope () throws SourceCodeException {

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
    public void setResolver ( Resolver <HtClause, Q> resolver ) {
        this.resolver = resolver;
    }

    /**
     * Chains compilation completion events onto the instruction compiler.
     */
    class ClauseChainObserver implements LogicCompilerObserver <HtClause, HtClause> {
        /**
         * {@inheritDoc}
         */
        public void onCompilation ( Sentence <HtClause> sentence ) throws SourceCodeException {
            instructionCompiler.compile(sentence);
        }

        /**
         * {@inheritDoc}
         */
        public void onQueryCompilation ( Sentence <HtClause> sentence ) throws SourceCodeException {
            instructionCompiler.compile(sentence);
        }
    }
}
