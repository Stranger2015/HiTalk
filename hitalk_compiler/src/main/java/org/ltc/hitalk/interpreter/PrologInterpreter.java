package org.ltc.hitalk.interpreter;


import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.Variable;
import com.thesett.common.parsing.SourceCodeException;
import jline.ConsoleReader;
import org.ltc.hitalk.compiler.bktables.IConfig;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.IParser;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.Language;
import org.ltc.hitalk.wam.compiler.prolog.ChainedCompilerObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

public
class PrologInterpreter<P, Q> implements IInterpreter <P, Q>, IParser {
    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    protected final PlPrologParser parser;
    protected IConfig config;
    protected ConsoleReader reader;
    private ChainedCompilerObserver observer;

    public PrologInterpreter ( PlPrologParser parser ) {
        this.parser = parser;
    }

    /**
     * @return
     */
    @Override
    public Mode getMode () {
        return null;
    }

//    @Override
//    public void compile ( PlTokenSource tokenSource, HtProperty... flags ) throws IOException, SourceCodeException {
//
//    }

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

//    /**
//     * @param clause
//     * @param flags
//     * @throws SourceCodeException
//     */
//    @Override
//    public
//    void compile ( HtClause clause, HtProperty... flags ) throws SourceCodeException {

//    }

//    /**
//     * @param rule
//     */
//    @Override
//    public
//    void compileDcgRule ( DcgRule rule ) throws SourceCodeException {
//
//    }

//    @Override
//    public void compileQuery ( HtClause query ) throws SourceCodeException {
//
//    }

//    /**
//     * @param clause
//     */
//    @Override
//    public
//    void compileClause ( HtClause clause ) {

//    }
//
//    /**
//     * @param resolver
//     */
//    @Override
//    public
//    void setResolver ( Resolver <HtClause, Q> resolver ) {
//
//    }

//    @Override
//    public void compile ( String fileName, HtProperty... flags ) {
//
//    }
//

    /**
     * @param clause
     * @throws SourceCodeException
     */
    @Override
    public void evaluate ( HtClause clause ) throws SourceCodeException {

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
        return null;
    }

    /**
     * @return
     * @throws IOException
     */
    @Override
    public ConsoleReader initializeCommandLineReader () throws IOException {
        return null;
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
//    @Override
    public void setCompilerObserver ( ChainedCompilerObserver observer ) {
        this.observer = observer;
    }

//    /**
//     * Compiles a sentence into a (presumably binary) form, that provides a Java interface into the compiled structure.
//     *
//     * @param sentence The sentence to compile.
//     * @throws SourceCodeException If there is an error in the source to be compiled that prevents its compilation.
//     */
//    @Override
//    public
//    void compile ( Sentence sentence ) throws SourceCodeException {
//
//    }

    /**
     * Signal the end of a compilation scope, to trigger completion of the compilation of its contents.
     *
     * @throws SourceCodeException If there is an error in the source to be compiled that prevents its compilation.
     */
//    @Override
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

    }

//    /**
//     * @param parser
//     */
//    public void setParser ( PlPrologParser parser ) {
//        this.parser = parser;
//    }
}
