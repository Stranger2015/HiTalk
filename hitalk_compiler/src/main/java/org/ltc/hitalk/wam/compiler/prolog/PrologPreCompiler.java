package org.ltc.hitalk.wam.compiler.prolog;

import com.thesett.aima.logic.fol.FunctorName;
import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.compiler.AbstractBaseMachine;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.PrologBuiltInTransform;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.interpreter.DcgRule;
import org.ltc.hitalk.parser.*;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Objects;

import static org.ltc.hitalk.core.BaseApp.getAppContext;

/**
 *
 */
public
class PrologPreCompiler<T extends HtClause, P, Q> extends AbstractBaseMachine implements ICompiler <T, P, Q> {

    final protected PlPrologParser parser;
    final protected PrologDefaultBuiltIn defaultBuiltIn;

    /**
     * Holds the built in transformation.
     */
    protected final PrologBuiltInTransform <T, P, Q> builtInTransform;
    final protected IResolver <HtPredicate, HtClause> resolver;
    protected LogicCompilerObserver <P, Q> observer;

    /**
     * @param symbolTable
     * @param interner
     * @param builtInTransform
     * @param resolver
     */
    public PrologPreCompiler ( ISymbolTable <Integer, String, Object> symbolTable,
                               IVafInterner interner,
                               PrologDefaultBuiltIn defaultBuiltIn,
                               PrologBuiltInTransform <T, P, Q> builtInTransform,
                               IResolver <HtPredicate, HtClause> resolver,
                               PlPrologParser parser
    ) {
        super(symbolTable, interner);

        this.defaultBuiltIn = defaultBuiltIn;//instructionCompiler.getDefaultBuiltIn();
        this.builtInTransform = builtInTransform;
        this.resolver = resolver;
        this.parser = parser;
    }

    public PrologPreCompiler () {
        this(getAppContext().getSymbolTable(),
                getAppContext().getInterner(),
                getAppContext().getDefaultBuiltIn(),
                getAppContext().getBuiltInTransform(),
                getAppContext().getResolverPre(),
                getAppContext().getParser());
    }

    /**
     * @param string
     * @param flags
     * @throws Exception
     */
    @Override
    public void compileString ( String string, HtProperty... flags ) throws Exception {
        ICompiler.super.compileString(string, flags);
    }

    /**
     * @param tokenSource
     * @param flags
     * @throws IOException
     * @throws SourceCodeException
     */
    @Override
    public void compile ( PlTokenSource tokenSource, HtProperty... flags ) throws IOException, SourceCodeException, ParseException {
        getConsole().info("Compiling " + tokenSource.getPath() + "... ");
        parser.setTokenSource(tokenSource);
        while (true) {
            ITerm t = parser.next();
            if (t == null) {
                break;
            }
            T c = (T) parser.convert(t);//FIXME
            compile(c, flags);
        }
    }

    @Override
    public Logger getConsole () {
        return logger;
    }

    @Override
    public PlPrologParser getParser () {
        return parser;
    }

    public void compile ( T clause, HtProperty... flags ) throws SourceCodeException {
        logger.debug("Compiling " + clause);
        if (clause.getT().getHead() == null) {
            final IFunctor goal = (IFunctor) clause.getBody().get(0);
            if (checkEncodingDirective(goal)) {
                parser.getTokenSource().setEncodingPermitted(false);
            }
        }
    }

    private boolean checkEncodingDirective ( IFunctor goal ) {
        final FunctorName functorName = interner.getDeinternedFunctorName(goal.getName());
        return Objects.equals(functorName.getName(), PrologAtoms.ENCODING) && functorName.getArity() == 1;
    }

    public void compileDcgRule ( DcgRule rule ) {

    }

    /**
     * @param query
     */
    public void compileQuery ( Q query ) {

    }

    /**
     * @param resolver
     */
    public void setResolver ( IResolver <P, Q> resolver ) {

    }

    public void compile ( String fileName, HtProperty... flags ) throws IOException, SourceCodeException {

    }

    public void compile ( T clause ) {

    }

    /**
     * Compiles a sentence into a (presumably binary) form, that provides a Java interface into the compiled structure.
     *
     * @param sentence The sentence to compile.
     * @throws SourceCodeException If there is an error in the source to be compiled that prevents its compilation.
     */
    public void compile ( Sentence <T> sentence ) throws SourceCodeException {

    }

    public void setCompilerObserver ( LogicCompilerObserver <P, Q> observer ) {
        this.observer = observer;
    }

    public void endScope () throws SourceCodeException {

    }
}