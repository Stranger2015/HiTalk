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
import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.Filterator;
import com.thesett.common.util.Source;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.wam.compiler.HtTokenSource;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * ResolutionEngine combines together a logic {@link Parser}, a {@link VariableAndFunctorInterner} that acts as a symbol
 * table, a {@link LogicCompiler} and a {@link Resolver}, into a single unit.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * </table></pre>
 *
 * @param <T> The compiled program type that the compiler produces.
 * @param <Q> The compiled query type that the compiler produces.
 * @author Rupert Smith
 */
public abstract
class HtResolutionEngine<T, Q> extends InteractiveParser
        implements VariableAndFunctorInterner,
                   LogicCompiler <HtClause, T, Q>, Resolver <T, Q> {
    /**
     * Holds the parser.
     */
    protected HtPrologParser parser;

    /**
     * Holds the variable and functor symbol table.
     */
    protected VariableAndFunctorInterner interner;

    /**
     * Holds the compiler.
     */
    protected LogicCompiler <HtClause, T, Q> compiler;

//    /**
//     * Holds the resolver.
//     */
//    protected Resolver <T, Q> resolver;

    /**
     * Holds the observer for compiler outputs.
     */
    protected HtResolutionEngine <T, Q>.ChainedCompilerObserver chainedObserver =
            new ChainedCompilerObserver();

    protected Q currentQuery;


    /**
     * Creates a prolog parser using the specified interner.
     *
     * @param parser
     * @param interner The functor and variable name interner.
     */
    public
    HtResolutionEngine ( HtPrologParser parser,
                         VariableAndFunctorInterner interner,
                         LogicCompiler <HtClause, T, Q> compiler ) {
        super(parser, interner);
        this.compiler = compiler;
//        this.resolver = resolver;
        compiler.setCompilerObserver(chainedObserver);
    }

    /**
     * Resets the engine to its default state. This will typically load any bootstrapping libraries of built-ins that
     * the engine requires, but otherwise set its domain to empty.
     */
    public abstract
    void reset ();

    /**
     * Provides the resolution engines interner.
     *
     * @return The resolution engines interner.
     */
    public
    VariableAndFunctorInterner getInterner () {
        return interner;
    }

    /**
     * Provides the resolution engines compiler.
     *
     * @return The resolution engines compiler.
     */
    public
    LogicCompiler <HtClause, T, Q> getCompiler () {
        return compiler;
    }
//
//    /**
//     * Provides the resolution engines resolver.
//     *
//     * @return The resolution engines resolver.
//     */
//    public
//    Resolver <T, Q> getResolver () {
//        return resolver;
//    }

    /**
     * Consults an input stream, reading first order logic clauses from it, and inserting them into the resolvers
     * knowledge base.
     *
     * @param stream The input stream to consult.
     * @throws SourceCodeException If any code read from the input stream fails to parse, compile or link.
     */
    public
    void consultInputStream ( InputStream stream ) throws SourceCodeException {
        // Create a token source to read from the specified input stream.
        Source <Token> tokenSource = HtTokenSource.getTokenSourceForInputStream(stream);
        getParser().setTokenSource(tokenSource);

        // Consult the type checking rules and add them to the knowledge base.
        while (true) {
            Sentence <HtClause> sentence = getParser().parse();

            if (sentence == null) {
                break;
            }

            getCompiler().compile(sentence);
        }
    }

    /**
     * Prints all of the logic variables in the results of a query.
     *
     * @param solution An iterable over the variables in the solution.
     * @return All the variables printed as a string, one per line.
     */
    public
    String printSolution ( Iterable <Variable> solution ) {
        String result = "";

        for (Variable var : solution) {
            result += printVariableBinding(var) + "\n";
        }

        return result;
    }

    /**
     * Prints all of the logic variables in the results of a query.
     *
     * @param variables An iterable over the variables in the solution.
     * @return All the variables printed as a string, one per line.
     */
    public
    String printSolution ( Map <String, Variable> variables ) {
        String result = "";

        for (Map.Entry <String, Variable> entry : variables.entrySet()) {
            result += printVariableBinding(entry.getValue()) + "\n";
        }

        return result;
    }

    /**
     * Prints a variable binding in the form 'Var = value'.
     *
     * @param var The variable to print.
     * @return The variable binding in the form 'Var = value'.
     */
    public
    String printVariableBinding ( Term var ) {
        return var.toString(getInterner(), true, false) + " = " + var.getValue().toString(getInterner(), false, true);
    }

    /**
     * Transforms an iterator over sets of variable bindings, resulting from a query, to an iterator over a map from the
     * string name of variables to their bindings, for the same sequence of query solutions.
     *
     * @param solutions The resolution solutions to convert to map form.
     * @return An iterator over a map from the string name of variables to their bindings, for the solutions.
     */
    public
    Iterable <Map <String, Variable>> expandResultSetToMap ( Iterator <Set <Variable>> solutions ) {
        return new Filterator <>(solutions, this::apply);
    }

    /**
     * {@inheritDoc}
     */
    public
    IdAttribute.IdAttributeFactory <String> getVariableInterner () {
        return interner.getVariableInterner();
    }

    /**
     * {@inheritDoc}
     */
    public
    IdAttribute.IdAttributeFactory <FunctorName> getFunctorInterner () {
        return interner.getFunctorInterner();
    }

    /**
     * {@inheritDoc}
     */
    public
    int internFunctorName ( String name, int numArgs ) {
        return interner.internFunctorName(name, numArgs);
    }

    /**
     * {@inheritDoc}
     */
    public
    int internFunctorName ( FunctorName name ) {
        return interner.internFunctorName(name);
    }

    /**
     * {@inheritDoc}
     */
    public
    int internVariableName ( String name ) {
        return interner.internVariableName(name);
    }

    /**
     * {@inheritDoc}
     */
    public
    String getVariableName ( int name ) {
        return interner.getVariableName(name);
    }

    /**
     * {@inheritDoc}
     */
    public
    String getVariableName ( Variable variable ) {
        return interner.getVariableName(variable);
    }

    /**
     * {@inheritDoc}
     */
    public
    FunctorName getDeinternedFunctorName ( int name ) {
        return interner.getDeinternedFunctorName(name);
    }

    /**
     * {@inheritDoc}
     */
    public
    String getFunctorName ( int name ) {
        return interner.getFunctorName(name);
    }

    /**
     * {@inheritDoc}
     */
    public
    int getFunctorArity ( int name ) {
        return interner.getFunctorArity(name);
    }

    /**
     * {@inheritDoc}
     */
    public
    FunctorName getFunctorFunctorName ( Functor functor ) {
        return interner.getFunctorFunctorName(functor);
    }

    /**
     * {@inheritDoc}
     */
    public
    String getFunctorName ( Functor functor ) {
        return interner.getFunctorName(functor);
    }

    /**
     * {@inheritDoc}
     */
    public
    int getFunctorArity ( Functor functor ) {
        return interner.getFunctorArity(functor);
    }

    /**
     * {@inheritDoc}
     */
    public
    void setTokenSource ( Source <Token> tokenSource ) {
        parser.setTokenSource(tokenSource);
    }

    /**
     * {@inheritDoc}
     */
    public
    Sentence <HtClause> parse () throws SourceCodeException {
        return parser.parse();
    }

    /**
     * {@inheritDoc}
     */
    public
    void setOperator ( String operatorName, int priority, OpSymbol.Associativity associativity ) {
        parser.setOperator(operatorName, priority, associativity);
    }

    /**
     * {@inheritDoc}
     */
    public
    void compile ( Sentence <HtClause> sentence ) throws SourceCodeException {
        compiler.compile(sentence);
    }

    /**
     * {@inheritDoc}
     */
    public
    void addToDomain ( T term ) throws LinkageException {
//        resolver.addToDomain(term);

    }


    /**
     * {@inheritDoc}
     */
    public
    void setQuery ( Q query ) throws LinkageException {
        currentQuery = query;
    }

    /**
     * {@inheritDoc}
     */
    public
    Set <Variable> resolve () {
        // Check that a query has been set to resolve.
        if (currentQuery == null) {
            throw new IllegalStateException("No query set to resolve.");
        }

        // Execute the byte code, starting from the first functor of the query.
        return executeAndExtractBindings(currentQuery);
    }


    /**
     * {@inheritDoc}
     */
    public
    Iterator <Set <Variable>> iterator () {
        return resolver.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public
    void setCompilerObserver ( LogicCompilerObserver <T, Q> observer ) {
        chainedObserver.setCompilerObserver(observer);
    }

    /**
     * {@inheritDoc}
     */
    public
    void endScope () throws SourceCodeException {
        compiler.endScope();
    }

    private
    Map <String, Variable> apply ( Set <Variable> variables ) {
        Map <String, Variable> results = new HashMap <>();

        for (Variable var : variables) {
            String varName = getInterner().getVariableName(var.getName());
            results.put(varName, var);
        }

        return results;
    }

    /**
     * ChainedCompilerObserver implements the compiler observer for this resolution engine. Compiled programs are added
     * to the resolvers domain. Compiled queries are executed.
     * <p>
     * <p/>If a chained observer is set up, all compiler outputs are forwarded onto it.
     */
    private
    class ChainedCompilerObserver implements LogicCompilerObserver <T, Q> {
        /**
         * Holds the chained observer for compiler outputs.
         */
        private LogicCompilerObserver <T, Q> observer;

        /**
         * Sets the chained observer for compiler outputs.
         *
         * @param observer The chained observer.
         */
        public
        void setCompilerObserver ( LogicCompilerObserver <T, Q> observer ) {
            this.observer = observer;
        }

        /**
         * {@inheritDoc}
         */
        public
        void onCompilation ( Sentence <T> sentence ) throws SourceCodeException {
            if (observer != null) {
                observer.onCompilation(sentence);
            }

            getResolver().addToDomain(sentence.getT());
        }

        /**
         * {@inheritDoc}
         */
        public
        void onQueryCompilation ( Sentence <Q> sentence ) throws SourceCodeException {
            if (observer != null) {
                observer.onQueryCompilation(sentence);
            }

            getResolver().setQuery(sentence.getT());
        }
    }
}

