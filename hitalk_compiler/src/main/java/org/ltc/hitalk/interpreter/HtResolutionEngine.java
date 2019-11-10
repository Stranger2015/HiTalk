/*
 * Copyright The Sett Ltd, 2005 to 2014.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ltc.hitalk.interpreter;

import com.thesett.aima.attribute.impl.IdAttribute;
import com.thesett.aima.logic.fol.*;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.Filterator;
import com.thesett.common.util.Source;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.ParseException;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlToken;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlTokenSource;
import org.ltc.hitalk.term.HlOpSymbol.Associativity;
import org.ltc.hitalk.term.io.HiTalkStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * ResolutionEngine combines together a logic {@link Parser}, a {@link VariableAndFunctorInterner} that acts as a symbol
 * table, a {@link LogicCompiler} and a {@link Resolver}, into a single unit.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * </table></pre>
 *
 * @author Rupert Smith
 */
public
class HtResolutionEngine<T extends HtClause, P, Q> extends InteractiveParser
        implements VariableAndFunctorInterner, ICompiler <T, P, Q>, Resolver <P, Q> {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    /**
     * Holds the parser.
     */
    protected PlPrologParser parser;

    /**
     * Holds the variable and functor symbol table.
     */
    protected VariableAndFunctorInterner interner;

    /**
     * Holds the compiler.
     */
    protected ICompiler <T, P, Q> compiler;
    protected Resolver <P, Q> resolver;

    /**
     * Holds the observer for compiler outputs.
     */
    protected LogicCompilerObserver <P, Q> observer = new ChainedCompilerObserver <>();

    protected Q currentQuery;
    protected final List <Set <Variable>> vars = new ArrayList <>();

    /**
     * Creates a prolog parser using the specified interner.
     *
     * @param parser
     * @param interner The functor and variable name interner.
     */
    public HtResolutionEngine ( PlPrologParser parser,
                                VariableAndFunctorInterner interner,
                                ICompiler <T, P, Q> compiler,
            /*Resolver <P, Q> resolver*/ ) {
        super(parser);

        this.interner = interner;
        this.compiler = compiler;
        this.resolver = this;//fixme
        this.compiler.setCompilerObserver(observer);
    }

    /**
     * Resets the engine to its default state. This will typically load any bootstrapping libraries of built-ins that
     * the engine requires, but otherwise set its domain to empty.
     */
    public void reset () {
        resolver.reset();//fixme
    }

    /**
     * Provides the resolution engines interner.
     *
     * @return The resolution engines interner.
     */
    public VariableAndFunctorInterner getInterner () {
        return interner;
    }

    /**
     * Provides the resolution engines compiler.
     *
     * @return The resolution engines compiler.
     */
    public ICompiler <T, P, Q> getCompiler () {
        return compiler;
    }

    /**
     * Consults an input stream, reading first order logic clauses from it, and inserting them into the resolvers
     * knowledge base.
     *
     * @param stream The input stream to consult.
     * @throws SourceCodeException If any code read from the input stream fails to parse, compile or link.
     */
    public void consultInputStream ( HiTalkStream stream ) throws SourceCodeException, IOException, ParseException {
        // Create a token source to read from the specified input stream.
        PlTokenSource tokenSource = stream.getTokenSource();
        getParser().setTokenSource(tokenSource);

        // Consult the type checking rules and add them to the knowledge base.
        while (true) {
            Sentence <Term> sentence = getParser().parse();

            if (sentence == null) {
                break;
            }

//            getCompiler().compile(sentence.getT());
        }
    }

    /**
     * Prints all of the logic variables in the results of a query.
     *
     * @param solution An iterable over the variables in the solution.
     * @return All the variables printed as a string, one per line.
     */
    public String printSolution ( Iterable <Variable> solution ) {
        StringBuilder result = new StringBuilder();

        for (Variable var : solution) {
            result.append(printVariableBinding(var)).append("\n");
        }

        return result.toString();
    }

    /**
     * Prints all of the logic variables in the results of a query.
     *
     * @param variables An iterable over the variables in the solution.
     * @return All the variables printed as a string, one per line.
     */
    public String printSolution ( Map <String, Variable> variables ) {
        StringBuilder result = new StringBuilder();

        for (Map.Entry <String, Variable> entry : variables.entrySet()) {
            result.append(printVariableBinding(entry.getValue())).append("\n");
        }

        return result.toString();
    }

    /**
     * Prints a variable binding in the form 'Var = value'.
     *
     * @param var The variable to print.
     * @return The variable binding in the form 'Var = value'.
     */
    public String printVariableBinding ( Term var ) {
        return var.toString(getInterner(), true, false) + " = " + var.getValue().toString(getInterner(), false, true);
    }

    /**
     * Transforms an iterator over sets of variable bindings, resulting from a query, to an iterator over a map from the
     * string name of variables to their bindings, for the same sequence of query solutions.
     *
     * @param solutions The resolution solutions to convert to map form.
     * @return An iterator over a map from the string name of variables to their bindings, for the solutions.
     */
    public Iterable <Map <String, Variable>> expandResultSetToMap ( Iterator <Set <Variable>> solutions ) {
        return new Filterator <>(solutions, this::apply);
    }

    /**
     * {@inheritDoc}
     */
    public IdAttribute.IdAttributeFactory <String> getVariableInterner () {
        return interner.getVariableInterner();
    }

    /**
     * {@inheritDoc}
     */
    public IdAttribute.IdAttributeFactory <FunctorName> getFunctorInterner () {
        return interner.getFunctorInterner();
    }

    /**
     * {@inheritDoc}
     */
    public int internFunctorName ( String name, int numArgs ) {
        return interner.internFunctorName(name, numArgs);
    }

    /**
     * {@inheritDoc}
     */
    public int internFunctorName ( FunctorName name ) {
        return interner.internFunctorName(name);
    }

    /**
     * {@inheritDoc}
     */
    public int internVariableName ( String name ) {
        return interner.internVariableName(name);
    }

    /**
     * {@inheritDoc}
     */
    public String getVariableName ( int name ) {
        return interner.getVariableName(name);
    }

    /**
     * {@inheritDoc}
     */
    public String getVariableName ( Variable variable ) {
        return interner.getVariableName(variable);
    }

    /**
     * {@inheritDoc}
     */
    public FunctorName getDeinternedFunctorName ( int name ) {
        return interner.getDeinternedFunctorName(name);
    }

    /**
     * {@inheritDoc}
     */
    public String getFunctorName ( int name ) {
        return interner.getFunctorName(name);
    }

    /**
     * {@inheritDoc}
     */
    public int getFunctorArity ( int name ) {
        return interner.getFunctorArity(name);
    }

    /**
     * {@inheritDoc}
     */
    public FunctorName getFunctorFunctorName ( Functor functor ) {
        return interner.getFunctorFunctorName(functor);
    }

    /**
     * {@inheritDoc}
     */
    public String getFunctorName ( Functor functor ) {
        return interner.getFunctorName(functor);
    }

    /**
     * {@inheritDoc}
     */
    public int getFunctorArity ( Functor functor ) {
        return interner.getFunctorArity(functor);
    }

    /**
     * {@inheritDoc}
     */
    public void setTokenSource ( Source <PlToken> tokenSource ) {
        parser.setTokenSource(tokenSource);
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    public Sentence <Term> parse () throws SourceCodeException, IOException, ParseException {
        return parser.parse();
    }

    /**
     * {@inheritDoc}
     */
    public void setOperator ( String operatorName, int priority, Associativity associativity ) {
        parser.setOperator(operatorName, priority, associativity);
    }

    /**
     * {@inheritDoc}
     */
    public void setQuery ( Q query ) throws LinkageException {
//        currentQuery = query;
        resolver.setQuery(query);
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
    public void addToDomain ( P term ) throws LinkageException {
        resolver.addToDomain(term);
    }

    /**
     * {@inheritDoc}
     */
    public Set <Variable> resolve () {
        // Check that a query has been set to resolve.
        if (currentQuery == null) {
            throw new IllegalStateException("No query set to resolve.");
        }

        // Execute the byte code, starting from the first functor of the query.
        return executeAndExtractBindings(currentQuery);
    }

    private Set <Variable> executeAndExtractBindings ( Q query ) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator <Set <Variable>> iterator () {
        return vars.iterator();
    }

//    /**
//     * {@inheritDoc}
//     *
//     * @param observer
//     */
//    public void setCompilerObserver ( ChainedCompilerObserver observer ) {
//        chainedObserver.setCompilerObserver(observer);
//    }

    public void compile ( Sentence <T> sentence ) throws SourceCodeException {
        compiler.compile(sentence);
    }

    public void setCompilerObserver ( LogicCompilerObserver <P, Q> observer ) {
        compiler.setCompilerObserver(observer);
    }

    /**
     * {@inheritDoc}
     */
    public void endScope () throws SourceCodeException {
        compiler.endScope();
    }

    private Map <String, Variable> apply ( Set <Variable> variables ) {
        Map <String, Variable> results = new HashMap <>();

        for (Variable var : variables) {
            String varName = getInterner().getVariableName(var.getName());
            results.put(varName, var);
        }

        return results;
    }

    @Override
    public void compile ( PlTokenSource tokenSource, HtProperty... flags ) throws IOException, SourceCodeException {
        compiler.compile(tokenSource, flags);
    }

    /**
     * @return
     */
    @Override
    public Logger getConsole () {
        return logger;
    }

    /**
     * @param clause
     * @param flags
     * @throws SourceCodeException
     */
    @Override
    public void compile ( T clause, HtProperty... flags ) throws SourceCodeException {

    }

    /**
     * @param rule
     */
    @Override
    public void compileDcgRule ( DcgRule rule ) throws SourceCodeException {//fixme
        compiler.compileDcgRule(rule);
    }

    /**
     * @param query
     */
    @Override
    public void compileQuery ( Q query ) throws SourceCodeException {
        compiler.compileQuery(query);
    }

    @Override
    public void setResolver ( Resolver <P, Q> resolver ) {
        this.resolver = resolver;
    }

    @Override
    public void compile ( String fileName, HtProperty... flags ) throws IOException, SourceCodeException {
        compiler.compile(fileName, flags);
    }

    public void compile ( T clause ) {
        compiler.compile(clause);
    }

    /**
     * ChainedCompilerObserver implements the compiler observer for this resolution engine. Compiled programs are added
     * to the resolvers domain. Compiled queries are executed.
     * <p>
     * <p/>If a chained observer is set up, all compiler outputs are forwarded onto it.
     */
    public static
    class ChainedCompilerObserver<P, Q> implements LogicCompilerObserver <P, Q> {

        /**
         * Accepts notification of the completion of the compilation of a sentence into a (binary) form.
         *
         * @param sentence The compiled form of the sentence.
         * @throws SourceCodeException If there is an error in the compiled code that prevents its further processing.
         */
        @Override
        public void onCompilation ( Sentence <P> sentence ) throws SourceCodeException {
            //todo
        }

        /**
         * {@inheritDoc}
         *
         * @param sentence
         */
        public void onQueryCompilation ( Sentence <Q> sentence ) throws SourceCodeException {
//todo
        }
    }
}