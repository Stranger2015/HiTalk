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
package org.ltc.hitalk.wam.compiler.hitalk;

import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.Sink;
import com.thesett.common.util.Source;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.PlToken;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.compiler.HtMethod;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.ltc.hitalk.wam.compiler.PrologInterpreterVisitor;
import org.ltc.hitalk.wam.printer.IAllTermsVisitor;
import org.ltc.hitalk.wam.printer.IPositionalTermTraverser;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Set;

import static org.ltc.hitalk.interpreter.IInterpreter.SEMICOLON;

/**
 *
 */
public class HiTalkInterpreterVisitor<T extends HtMethod, P, Q> extends PrologInterpreterVisitor <T, P, Q>
        implements IAllTermsVisitor {


    protected int currentPredicateName;

    /**
     * Creates a positional visitor.
     *
     * @param symbolTable         The compiler symbol table.
     * @param interner            The name interner.
     * @param positionalTraverser
     */
    public HiTalkInterpreterVisitor ( ISymbolTable <Integer, String, Object> symbolTable,
                                      IVafInterner interner,
                                      IResolver <P, Q> resolver,
                                      IPositionalTermTraverser positionalTraverser,
                                      HiTalkInterpreter <T, P, Q> engine
    ) throws IOException {
        super(symbolTable, interner, resolver, positionalTraverser, engine);
    }

    /**
     * @param predicate The predicate being entered.
     */
    @Override
    protected void enterPredicate ( HtPredicate predicate ) {
        super.enterPredicate(predicate);
    }

    /**
     * @param predicate
     */
    @Override
    protected void leavePredicate ( HtPredicate predicate ) {
        super.leavePredicate(predicate);
    }

    /**
     * @param lt
     * @throws LinkageException
     */
    @Override
    protected void enterListTerm ( ListTerm lt ) throws LinkageException {
        super.enterListTerm(lt);
    }

    /**
     * @param listTerm
     */
    @Override
    protected void leaveListTerm ( ListTerm listTerm ) {
        super.leaveListTerm(listTerm);
    }

    /**
     * @param functor The functor being entered.
     * @throws LinkageException
     */
    @Override
    protected void enterFunctor ( IFunctor functor ) throws LinkageException {
        super.enterFunctor(functor);
    }

    /**
     * @param functor The functor being left.
     */
    @Override
    protected void leaveFunctor ( IFunctor functor ) {
        super.leaveFunctor(functor);
    }

    /**
     * @param variable The variable being entered.
     */
    @Override
    protected void enterVariable ( HtVariable variable ) {
        super.enterVariable(variable);
    }

    /**
     * @param variable The variable being left.
     */
    @Override
    protected void leaveVariable ( HtVariable variable ) {
        super.leaveVariable(variable);
    }

    protected void enterClause ( HtClause clause ) throws LinkageException {
        super.enterClause(clause);
    }

    /**
     * @param clause The clause being left.
     */
    @Override
    protected void leaveClause ( HtClause clause ) {
        super.leaveClause(clause);
    }

    /**
     * Evaluates a query against the resolver or adds a clause to the resolvers domain.
     *
     * @param sentence The clausal sentence to run as a query or as a clause to add to the domain.
     * @throws SourceCodeException If the query or domain clause fails to compile or link into the resolver.
     */
    private void evaluate ( Sentence <T> sentence ) throws SourceCodeException {
        HtMethod clause = sentence.getT();

        if (clause.isQuery()) {
            engine.endScope();
            engine.getCompiler().compile(sentence);
            evaluateQuery();
        } else {
            // Check if the program clause is new, or a continuation of the current predicate.
            int name = clause.getHead().getName();

            if (/*(currentPredicateName == null) || */currentPredicateName != name) {
                engine.endScope();
                currentPredicateName = name;
            }

            addProgramClause(sentence);
        }
    }

    /**
     * Evaluates a query. In the case of queries, the interner is used to recover textual names for the resulting
     * variable bindings. The user is queried through the parser to if more than one solution is required.
     */
    private void evaluateQuery () {
        logger.debug("Read query from input.");
        // Create an iterator to generate all solutions on demand with. Iteration will stop if the request to
        // the parser for the more ';' token fails.
        Iterator <Set <HtVariable>> i = engine.iterator();

        if (!i.hasNext()) {
            System.out.println("false. ");

            return;
        }

        for (; i.hasNext(); ) {
            Set <HtVariable> solution = i.next();

            if (solution.isEmpty()) {
                System.out.print("true");
            } else {
                for (Iterator <HtVariable> j = solution.iterator(); j.hasNext(); ) {
                    HtVariable nextVar = j.next();

                    String varName = engine.getInterner().getVariableName(nextVar.getName());

                    System.out.print(varName + " = " + nextVar.getValue().toString(engine.getInterner(), true, false));

                    if (j.hasNext()) {
                        System.out.println();
                    }
                }
            }

            // Finish automatically if there are no more solutions.
            if (!i.hasNext()) {
                System.out.println(".");

                break;
            }

            // Check if the user wants more solutions.
            try {
                int key = engine.getConsoleReader().readVirtualKey();

                if (key == SEMICOLON) {
                    System.out.println(" ;");
                } else {
                    System.out.println();
                    break;
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Adds a program clause to the domain. Multiple program clauses making up a predicate are compiled as a unit, and
     * not individually. For this reason, Prolog expects clauses for the same predicate to appear together in source
     * code. When a clause with a name and arity not seen before is encountered, a new compiler scope is entered into,
     * and this compiler scope is closed at the EOF of the current input stream, or when another clause with a different
     * name and arity is seen.
     *
     * @param sentence The clause to add to the domain.
     */
    private void addProgramClause ( Sentence <T> sentence ) throws SourceCodeException {
        logger.debug("Read program clause from input.");

        engine.getCompiler().compile(sentence);
    }

    /**
     * Used to buffer tokens.
     */
    private static class TokenBuffer implements Source <PlToken>, Sink <PlToken> {
        Deque <PlToken> tokens = new ArrayDeque <>();

        public boolean offer ( PlToken o ) {
            return tokens.offer(o);
        }

        public PlToken poll () {
            return tokens.poll();
        }

        public PlToken peek () {
            return tokens.peek();
        }

        public void clear () {
            tokens.clear();
        }
    }

    /**
     * OffsettingTokenSource is a token source that automatically adds in line offsets to all tokens, to assist the
     * parser when operating interactively line-at-a-time.
     */
    private static class OffsettingTokenSource implements Source <PlToken> {
        /**
         * Holds the underlying token source.
         */
        private final Source <PlToken> source;

        /**
         * Holds the current line offset to add to all tokens.
         */
        private final int lineOffset;

        /**
         * Wraps another token source.
         *
         * @param source The token source to wrap.
         */
        private OffsettingTokenSource ( Source <PlToken> source, int lineOffset ) {
            this.source = source;
            this.lineOffset = lineOffset;
        }

        /**
         * {@inheritDoc}
         */
        public PlToken poll () {
            return addOffset(copyToken(source.poll()));
        }

        /**
         * {@inheritDoc}
         */
        public PlToken peek () {
            return addOffset(copyToken(source.peek()));
        }

        private PlToken addOffset ( PlToken token ) {
            token.beginLine += lineOffset;
            token.endLine += lineOffset;

            return token;
        }

        private PlToken copyToken ( PlToken token ) {
            PlToken newToken = new PlToken(token.kind);

//            newToken.kind = token.kind;
            newToken.beginLine = token.beginLine;
            newToken.beginColumn = token.beginColumn;
            newToken.endLine = token.endLine;
            newToken.endColumn = token.endColumn;
            newToken.image = token.image;
            newToken.next = token.next;
            newToken.specialToken = token.specialToken;

            return newToken;
        }
    }
}
