package org.ltc.hitalk.wam.compiler.prolog;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.AbstractBaseMachine;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.PrologBuiltInTransform;
import org.ltc.hitalk.compiler.bktables.IApplication;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.interpreter.DcgRule;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlTokenSource;
import org.slf4j.Logger;

import java.io.IOException;

/**
 *
 */
public
class PrologPreCompiler extends AbstractBaseMachine implements ICompiler <HtClause, HtPredicate, HtClause> {

    final protected PlPrologParser parser;
    final protected PrologDefaultBuiltIn defaultBuiltIn;
    /**
     * Holds the built in transformation.
     */
    protected final PrologBuiltInTransform <IApplication, HtClause> builtInTransform;
    final protected IResolver <HtPredicate, HtClause> resolver;

    /**
     * @param symbolTable
     * @param interner
     * @param defaultBuiltIn
     * @param builtInTransform
     * @param resolver
     */
    public PrologPreCompiler ( SymbolTable <Integer, String, Object> symbolTable,
                               IVafInterner interner,
                               PrologDefaultBuiltIn defaultBuiltIn,
                               PrologBuiltInTransform <IApplication, HtClause> builtInTransform,
                               IResolver <HtPredicate, HtClause> resolver,
                               PlPrologParser parser ) {
        super(symbolTable, interner);

        this.defaultBuiltIn = defaultBuiltIn;
        this.builtInTransform = builtInTransform;
        this.resolver = resolver;
        this.parser = parser;
    }

    public void compile ( PlTokenSource tokenSource, HtProperty... flags ) throws IOException, SourceCodeException {

    }

    @Override
    public Logger getConsole () {
        return logger;
    }

    @Override
    public PlPrologParser getParser () {
        return parser;
    }

    public void compile ( HtClause clause, HtProperty... flags ) throws SourceCodeException {

    }

    public void compileDcgRule ( DcgRule rule ) throws SourceCodeException {

    }

    public void compileQuery ( HtClause query ) throws SourceCodeException {

    }

    /**
     * @param resolver
     */
    public void setResolver ( IResolver <HtPredicate, HtClause> resolver ) {

    }

    public void compile ( String fileName, HtProperty... flags ) throws IOException, SourceCodeException {

    }

    public void compile ( HtClause clause ) {

    }

    /**
     * Compiles a sentence into a (presumably binary) form, that provides a Java interface into the compiled structure.
     *
     * @param sentence The sentence to compile.
     * @throws SourceCodeException If there is an error in the source to be compiled that prevents its compilation.
     */
    public void compile ( Sentence <HtClause> sentence ) throws SourceCodeException {

    }

    public void setCompilerObserver ( LogicCompilerObserver <HtPredicate, HtClause> observer ) {

    }

    public void endScope () throws SourceCodeException {

    }
}