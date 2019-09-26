package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.*;
import com.thesett.aima.logic.fol.bytecode.BaseMachine;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.bktables.Flag;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.interpreter.DcgRule;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.wam.task.TransformTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;

public
class HiTalkWAMCompiler extends BaseMachine implements ICompiler <HtClause, HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    private final HiTalkDefaultBuiltIn defbi;

    private HiTalkInstructionCompiler instructionCompiler;
    private HiTalkPreprocessor <Term, TransformTask <HtClause, Term>> preCompiler;
    private HtPrologParser <HtClause> parser;
    private Resolver <HtClause, HiTalkWAMCompiledQuery> resolver;
    private Resolver <HtClause, HtClause> resolver2;

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    r The interner for the machine.
     * @param parser
     */
    public
    HiTalkWAMCompiler ( SymbolTable <Integer, String, Object> symbolTable, VariableAndFunctorInterner interner, HtPrologParser <HtClause> parser ) throws LinkageException {
        super(symbolTable, interner);
        this.parser = parser;
        defbi = new HiTalkDefaultBuiltIn(symbolTable, interner);
        instructionCompiler = new HiTalkInstructionCompiler(symbolTable, interner, defbi);
        getPreCompiler().getCompilerObserver(new ClauseChainObserver());
    }

    public
    HiTalkPreCompiler getPreCompiler () throws LinkageException {
        if (preCompiler == null) {
            preCompiler = new HiTalkPreprocessor <>(
                    getSymbolTable(),
                    getInterner(),
                    getDefaultBuiltIn(),
                    getResolver2(),
                    this);
        }

        return preCompiler;
    }

    private
    HiTalkDefaultBuiltIn getDefaultBuiltIn () {

        return new HiTalkDefaultBuiltIn(getSymbolTable(), getInterner());
    }


    ICompiler <HtClause, HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> getInstructionCompiler () {
        if (instructionCompiler == null) {
            instructionCompiler = new HiTalkInstructionCompiler(
                    getSymbolTable(),
                    getInterner(),
                    new HiTalkDefaultBuiltIn(getSymbolTable(), getInterner()));
        }
        return instructionCompiler;
    }

    /**
     * Establishes an observer on the compiled forms that the compiler outputs.
     *
     * @param observer The compiler output observer.
     */
    @Override
    public
    void setCompilerObserver ( LogicCompilerObserver <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> observer ) {
        instructionCompiler.setCompilerObserver(observer);
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

    }

    /**
     * {@inheritDoc}
     */
    public
    void endScope () throws SourceCodeException {
        preCompiler.endScope();
        instructionCompiler.endScope();
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
    HtPrologParser <HtClause> getParser () {
        return parser;
    }

    /**
     * @param clause
     * @param flags
     * @throws SourceCodeException
     */
    @Override
    public
    void compile ( HtClause clause, Flag... flags ) throws SourceCodeException {

    }

    /**
     * @param rule
     */
    @Override
    public
    void compileDcgRule ( DcgRule rule ) throws SourceCodeException {

    }

    /**
     * @param query
     */
    @Override
    public
    void compileQuery ( HiTalkWAMCompiledQuery query ) throws SourceCodeException {

    }

    /**
     * @param clause
     */
    @Override
    public
    void compileClause ( HtClause clause ) {

    }

    /**
     * @param resolver
     */
    @Override
    public
    void setResolver ( Resolver <HtClause, HiTalkWAMCompiledQuery> resolver ) {
        this.resolver = resolver;
    }

    public
    Resolver <HtClause, HtClause> getResolver2 () {
        return resolver2;
    }

    public
    void setResolver2 ( Resolver <HtClause, HtClause> resolver2 ) {
        this.resolver2 = resolver2;
    }

    public
    Resolver <HtClause, HiTalkWAMCompiledQuery> getResolver () {
        return resolver;
    }

    public
    void compileFile ( File file ) throws FileNotFoundException {
        compile(HtTokenSource.getTokenSourceForFile(file));
    }

    /**
     * Chains compilation completion events onto the instruction compiler.
     */
    class ClauseChainObserver implements LogicCompilerObserver <HtClause, HtClause> {
        /**
         * {@inheritDoc}
         */
        public
        void onCompilation ( Sentence <HtClause> sentence ) throws SourceCodeException {
            instructionCompiler.compile(sentence);
        }

        /**
         * {@inheritDoc}
         */
        public
        void onQueryCompilation ( Sentence <HtClause> sentence ) throws SourceCodeException {
            instructionCompiler.compile(sentence);
        }
    }
}
