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
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.parser.*;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.io.HiTalkStream;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.ltc.hitalk.term.HlOpSymbol.Associativity;

/**
 * ResolutionEngine combines together a logic {@link Parser}, a {@link IVafInterner} that acts as a symbol
 * table, a {@link ICompiler} and a {@link IResolver}, into a single unit.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * </table></pre>
 *
 * @author Rupert Smith
 */
public
class HtResolutionEngine<T extends HtClause, P, Q> extends InteractiveParser
        implements IVafInterner, ICompiler <T, P, Q>, IResolver <P, Q> {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    /**
     * Holds the parser.
     */
    protected PlPrologParser parser;

    /**
     * Holds the variable and functor symbol table.
     */
    protected IVafInterner interner;

    /**
     * Holds the compiler.
     */
    protected ICompiler <T, P, Q> compiler;

    /**
     * Holds the observer for compiler outputs.
     */
    protected LogicCompilerObserver <P, Q> observer = new ChainedCompilerObserver <>();

    protected Q currentQuery;
    protected final List <Set <HtVariable>> vars = new ArrayList <>();
    protected IResolver <P, Q> resolver;

    /**
     * Creates a prolog parser using the specified interner.
     *
     * @param parser
     * @param interner The functor and variable name interner.
     */
    public HtResolutionEngine ( PlPrologParser parser,
                                IVafInterner interner,
                                ICompiler <T, P, Q> compiler ) {
        super(parser);

        this.interner = interner;
        this.compiler = compiler;
        this.resolver = this;
        this.compiler.setCompilerObserver(observer);
    }

    /**
     * Resets the engine to its default state. This will typically load any
     * bootstrapping libraries of built-ins that
     * the engine requires, but otherwise set its domain to empty.
     */
    public void reset () {

    }

    /**
     * Provides the resolution engines interner.
     *
     * @return The resolution engines interner.
     */
    public IVafInterner getInterner () {
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
            ISentence <ITerm> sentence = getParser().parse();

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
    public String printSolution ( Iterable <HtVariable> solution ) {
        StringBuilder result = new StringBuilder();

        for (HtVariable var : solution) {
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
    public String printSolution ( Map <String, HtVariable> variables ) {

        return variables.values().stream().map(variable ->
                printVariableBinding(variable) + "\n").collect(Collectors.joining());
    }

    /**
     * Prints a variable binding in the form 'Var = value'.
     *
     * @param var The variable to print.
     * @return The variable binding in the form 'Var = value'.
     */
    public String printVariableBinding ( ITerm var ) {
        return var.toString(getInterner(), true, false) + " = " + var.getValue().toString(getInterner(), false, true);
    }

    /**
     * Transforms an iterator over sets of variable bindings, resulting from a query, to an iterator over a map from the
     * string name of variables to their bindings, for the same sequence of query solutions.
     *
     * @param solutions The resolution solutions to convert to map form.
     * @return An iterator over a map from the string name of variables to their bindings, for the solutions.
     */
    public Iterable <Map <String, HtVariable>> expandResultSetToMap ( Iterator <Set <HtVariable>> solutions ) {
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
    public String getVariableName ( HtVariable variable ) {
        return interner.getVariableName(variable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FunctorName getDeinternedFunctorName ( int name ) {
        return interner.getDeinternedFunctorName(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFunctorName ( int name ) {
        return interner.getFunctorName(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getFunctorArity ( int name ) {
        return interner.getFunctorArity(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FunctorName getFunctorFunctorName ( IFunctor functor ) {
        return interner.getFunctorFunctorName(functor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFunctorName ( IFunctor functor ) {
        return interner.getFunctorName(functor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getFunctorArity ( IFunctor functor ) {
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
    @Override
    public ISentence <ITerm> parse () throws SourceCodeException, IOException, ParseException {
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
//        resolver.setQuery(query);
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
        //resolver.addToDomain(term);
    }

    /**
     * {@inheritDoc}
     */
    public Set <HtVariable> resolve () {
        // Check that a query has been set to resolve.
        if (currentQuery == null) {
            throw new IllegalStateException("No query set to resolve.");
        }

        // Execute the byte code, starting from the first functor of the query.
        return executeAndExtractBindings(currentQuery);
    }

    private Set <HtVariable> executeAndExtractBindings ( Q query ) {
        return null;
    }//todo

    /**
     * {@inheritDoc}
     */
    public Iterator <Set <HtVariable>> iterator () {
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
        compiler.compile(sentence.getT());
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

    private Map <String, HtVariable> apply ( Set <HtVariable> variables ) {
        Map <String, HtVariable> results = new HashMap <>();

        for (HtVariable var : variables) {
            String varName = getInterner().getVariableName(var.getName());
            results.put(varName, var);
        }

        return results;
    }

    @Override
    public void compile ( PlTokenSource tokenSource, HtProperty... flags ) throws IOException, SourceCodeException, ParseException {
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
        compiler.compile(clause, flags);
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
    public void setResolver ( IResolver <P, Q> resolver ) {
        this.resolver = resolver;
    }

    @Override
    public void compile ( String fileName, HtProperty... flags ) throws IOException, SourceCodeException, ParseException {
        compiler.compile(fileName, flags);
    }

    public void compile ( T clause ) throws SourceCodeException {
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