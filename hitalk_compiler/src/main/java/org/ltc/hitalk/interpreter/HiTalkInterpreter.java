package org.ltc.hitalk.interpreter;

import com.thesett.aima.logic.fol.*;
import com.thesett.common.parsing.SourceCodeException;
import jline.ConsoleReader;
import org.ltc.hitalk.compiler.bktables.Flag;
import org.ltc.hitalk.compiler.bktables.IConfig;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.term.io.TermIO;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

public
class HiTalkInterpreter<T extends HtClause, P, Q> implements IInterpreter <T, P, Q> {
    /**
     * @return
     */
    @Override
    public
    Mode getMode () {
        return null;
    }

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
    HtPrologParser <T> getParser () {
        return null;
    }

    /**
     * @param clause
     * @param flags
     * @throws SourceCodeException
     */
    @Override
    public
    void compile ( T clause, Flag... flags ) throws SourceCodeException {

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
    void compileQuery ( Q query ) throws SourceCodeException {

    }

    /**
     * @param clause
     */
    @Override
    public
    void compileClause ( T clause ) {

    }

    /**
     * @param resolver
     */
    @Override
    public
    void setResolver ( Resolver <T, Q> resolver ) {

    }

    /**
     * @param clause
     * @throws SourceCodeException
     */
    @Override
    public
    void evaluate ( T clause ) throws SourceCodeException {

    }

    /**
     * @return
     */
    @Override
    public
    ConsoleReader getConsoleReader () {
        return null;
    }

    /**
     * @param reader
     */
    @Override
    public
    void setConsoleReader ( ConsoleReader reader ) {

    }

    /**
     * @return
     */
    @Override
    public
    String getQueryPrompt () {
        return null;
    }

    /**
     * @return
     * @throws IOException
     */
    @Override
    public
    ConsoleReader initializeCommandLineReader () throws IOException {
        return null;
    }

    /**
     * @return
     */
    @Override
    public
    TermIO getTermIO () {
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
    public
    void addToDomain ( P term ) throws LinkageException {

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
    public
    void setQuery ( Q query ) throws LinkageException {

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
    public
    Set <Variable> resolve () {
        return null;
    }

    /**
     * Resets the resolver. This should clear any start and goal states, and leave the resolver in a state in which it
     * is ready to be run.
     */
    @Override
    public
    void reset () {

    }

    /**
     * Provides an iterator that generates all solutions on demand as a sequence of variable bindings.
     *
     * @return An iterator that generates all solutions on demand as a sequence of variable bindings.
     */
    @Override
    public
    Iterator <Set <Variable>> iterator () {
        return null;
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
    void setCompilerObserver ( LogicCompilerObserver <P, Q> observer ) {

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

    /**
     * @return
     */
    @Override
    public
    IConfig getConfig () {
        return null;
    }

    /**
     * @param config
     */
    @Override
    public
    void setConfig ( IConfig config ) {

    }
}
