package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.Resolver;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.compiler.bktables.Flag;
import org.ltc.hitalk.interpreter.DcgRule;
import org.ltc.hitalk.interpreter.ICompiler;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.slf4j.Logger;

public
class HiLogAstCompiler <T extends HtClause> implements ICompiler <T, T, T> {
    /**
     * @return
     */
    @Override
    public
    Logger getConsole () {
        return null;
    }

    /**
     * @return
     */
    @Override
    public
    HtPrologParser getParser () {
        return null;
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
    void compileQuery ( HtClause query ) throws SourceCodeException {

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
    void setResolver ( Resolver <T, T> resolver ) {

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

    }

    /**
     * Establishes an observer on the compiled forms that the compiler outputs.
     *
     * @param observer The compiler output observer.
     */
    @Override
    public
    void setCompilerObserver ( LogicCompilerObserver <T, T> observer ) {

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
}
