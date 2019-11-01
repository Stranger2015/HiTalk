package org.ltc.hitalk.interpreter;

import com.thesett.aima.logic.fol.*;
import com.thesett.common.parsing.SourceCodeException;
import jline.ConsoleReader;
import org.ltc.hitalk.compiler.bktables.IConfig;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.IParser;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.term.io.TermIO;
import org.ltc.hitalk.wam.compiler.Language;
import org.ltc.hitalk.wam.compiler.prolog.ChainedCompilerObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Set;

public
class PrologInterpreter<T extends HtClause, P, Q> implements IInterpreter <T, P, Q>, IParser {
    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    protected final PlPrologParser parser;
    protected IConfig config;
    protected ConsoleReader reader;
    protected final ICompiler <T, P, Q> compiler;
    protected LogicCompilerObserver <P, Q> observer;
    protected Mode mode;
    protected HtResolutionEngine <T, P, Q> engine;
    protected int currentPredicateName;


    /**
     * @param parser
     * @param compiler
     */
    public PrologInterpreter ( PlPrologParser parser, ICompiler <T, P, Q> compiler, Resolver <P, Q> resolver ) {
        this.parser = parser;
        this.compiler = compiler;
        engine = new HtResolutionEngine <>(parser,
                TermIO.instance().getInterner(),
                compiler, resolver
        );
    }

    /**
     * @return
     */
    @Override
    public Mode getMode () {
        return mode;
    }

    /**
     * @return
     */
    public Logger getConsole () {
        return logger;
    }

    /**
     * @return
     */
    @Override
    public PlPrologParser getParser () {
        return parser;
    }

    /**
     * @return
     */
    @Override
    public Language language () {
        return parser.language();
    }

    @Override
    public void initializeBuiltIns () {
        parser.initializeBuiltIns();
    }

    @Override
    public Sentence <HtClause> parseClause () {
        return parser.parseClause();
    }

    @Override
    public HtClause convert ( Term t ) {
        return null;
    }

    /**
     * @param clause
     * @throws SourceCodeException
     */
    @Override
    public void evaluate ( T clause ) throws SourceCodeException {
        if (clause.isQuery()) {
            engine.endScope();
            engine.compileClause(clause);
//            evaluateQuery();
        } else {
            // Check if the program clause is new, or a continuation of the current predicate.
            int name = clause.getHead().getName();

            if (/*currentPredicateName == null ||*/ currentPredicateName != name) {
                engine.endScope();
                currentPredicateName = name;
            }

            addProgramClause(clause);
        }
    }

    /**
     * @param clause
     */
    private void addProgramClause ( HtClause clause ) {
        //todo
    }

    /**
     * @return
     */
    @Override
    public ConsoleReader getConsoleReader () {
        return reader;
    }

    /**
     * @param reader
     */
    @Override
    public void setConsoleReader ( ConsoleReader reader ) {
        this.reader = reader;
    }

    /**
     * @return
     */
    @Override
    public String getQueryPrompt () {
        return "?- ";
    }

    /**
     * @return
     */
    @Override
    public ConsoleReader initializeCommandLineReader () {
        return reader;
    }

    /**
     * Adds the specified construction to the domain of resolution searched by this resolver.
     *
     * @param term The term to add to the domain.
     * @throws LinkageException If the term to add to the domain, cannot be added to it, because it depends on the
     *                          existance of other clauses which are not in the domain. Implementations may elect to
     *                          raise this as an error at the time the clauses are added to the domain, or during
     *                          resolution, or simply to fail to find a resolution.
     */
    @Override
    public void addToDomain ( P term ) throws LinkageException {

    }

    /**
     * Sets the query to resolve.
     *
     * @param query The query to resolve.
     * @throws LinkageException If the query to add run over the domain, cannot be applied to it, because it depends on
     *                          the existance of clauses which are not in the domain. Implementations may elect to raise
     *                          this as an error at the time the query is created, or during resolution, or simply to
     *                          fail to find a resolution.
     */
    @Override
    public void setQuery ( Q query ) throws LinkageException {

    }

    /**
     * Resolves a query over a logical domain, or knowledge base and a query. The domain and query to resolve over must
     * be established by prior to invoking this method. There may be more than one set of bindings that make the query
     * provable over the domain, in which case subsequent calls to this method will return successive bindings until no
     * more can be found. If no proof can be found, this method will return <tt>null</tt>.
     *
     * @return A list of variable bindings, if the query can be satisfied, or <tt>null</tt> otherwise.
     */
    @Override
    public Set <Variable> resolve () {
        return null;
    }//todo

    /**
     * Resets the resolver. This should clear any start and goal states, and leave the resolver in a state in which it
     * is ready to be run.
     */
    @Override
    public void reset () {

    }

    /**
     * Provides an iterator that generates all solutions on demand as a sequence of variable bindings.
     *
     * @return An iterator that generates all solutions on demand as a sequence of variable bindings.
     */
    @Override
    public Iterator <Set <Variable>> iterator () {
        return null;
    }//todo

    /**
     * Establishes an observer on the compiled forms that the compiler outputs.
     *
     * @param observer The compiler output observer.
     */
    public void setCompilerObserver ( ChainedCompilerObserver observer ) {
        this.observer = observer;
    }

    /**
     * Signal the end of a compilation scope, to trigger completion of the compilation of its contents.
     *
     * @throws SourceCodeException If there is an error in the source to be compiled that prevents its compilation.
     */
    public void endScope () throws SourceCodeException {

    }

    /**
     * @return
     */
    @Override
    public IConfig getConfig () {
        return config;
    }

    /**
     * @param config
     */
    @Override
    public void setConfig ( IConfig config ) {
        this.config = config;
    }
}