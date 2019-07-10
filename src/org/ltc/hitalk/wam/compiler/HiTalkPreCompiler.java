package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.*;
import com.thesett.aima.logic.fol.bytecode.BaseMachine;
import com.thesett.aima.logic.fol.compiler.SymbolKeyTraverser;
import com.thesett.aima.logic.fol.compiler.TermWalker;
import com.thesett.aima.logic.fol.isoprologparser.TokenSource;
import com.thesett.aima.logic.fol.wam.TermWalkers;
import com.thesett.aima.search.util.backtracking.DepthFirstBacktrackingSearch;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.entities.context.LoadContext;

import java.util.List;
import java.util.Objects;

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

/**
 * PreCompiler transforms clauses for compilation, substituting built-ins for any built-ins in the source expressions to
 * compile.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Perform the built-ins transformation.
 * </table></pre>
 *
 * @author Rupert Smith
 */
abstract public
class HiTalkPreCompiler<T extends Clause> extends BaseMachine implements LogicCompiler <T, Clause, Clause> {

    //Used for debugging.
    /* private static final Logger log = Logger.getLogger(PreCompiler.class.getName()); */

    /**
     * Holds the default built in, for standard compilation and interners and symbol tables.
     */
    private final HiTalkDefaultBuiltIn defaultBuiltIn;

    /**
     * Holds the built in transformation.
     */
    private final HiTalkBuiltInTransform builtInTransform;

    /**
     * Holds the compiler output observer.
     */
    private LogicCompilerObserver <Clause, Clause> observer;

    /**
     * Creates a new PreCompiler.
     *
     * @param symbolTable    The symbol table.
     * @param interner       The machine to translate functor and variable names.
     * @param defaultBuiltIn The default built in, for standard compilation and interners and symbol tables.
     */
    public
    HiTalkPreCompiler ( SymbolTable <Integer, String, Object> symbolTable,
                        VariableAndFunctorInterner interner,
                        HiTalkDefaultBuiltIn defaultBuiltIn ) {
        super(symbolTable, interner);

        this.defaultBuiltIn = defaultBuiltIn;
        builtInTransform = new HiTalkBuiltInTransform(defaultBuiltIn);
    }

    /**
     * {@inheritDoc}
     */
    public
    void compile ( Sentence <T> sentence ) throws SourceCodeException {
        List <T> clauses = preprocess(sentence.getT());
//        Clause clause = null;

//        for (Term term : terms) {
//            clause = TermUtils.convertToClause(term, interner);
//            clauses.add(clause);
//        }

        for (
                Clause clause : clauses) {
            substituteBuiltIns(clause);
            initializeSymbolTable(clause);
            topLevelCheck(clause);

            if (observer != null) {
                final Clause finalClause = clause;
                if (Objects.requireNonNull(clause).isQuery()) {

                    observer.onQueryCompilation(() -> finalClause);
                }
                else {
                    observer.onCompilation(() -> finalClause);
                }
            }
        }
        saveResult(clauses);
    }

    /**
     * @param clauses
     */
    protected abstract
    void saveResult ( List <T> clauses );

    /**
     * @param term
     * @return
     */
    protected abstract
    List <T> preprocess ( T term );

    /**
     * {@inheritDoc}
     */
    public
    void setCompilerObserver ( LogicCompilerObserver <Clause, Clause> observer ) {
        this.observer = observer;
    }

    /**
     * {@inheritDoc}
     */
    public
    void endScope () throws SourceCodeException {

    }

    /**
     * Substitutes built-ins within a clause, with their built-in definitions.
     *
     * @param clause The clause to transform.
     */
    private
    void substituteBuiltIns ( Term clause ) {
        TermWalker walk = TermWalkers.positionalWalker(new HiTalkBuiltInTransformVisitor(interner, symbolTable, null, builtInTransform));
        walk.walk(clause);
    }

    /**
     * Runs a symbol key traverser over the clause to be compiled, to ensure that all of its terms and sub-terms have
     * their symbol keys initialised.
     *
     * @param clause The clause to initialise the symbol keys of.
     */
    private
    void initializeSymbolTable ( Term clause ) {
        // Run the symbol key traverser over the clause, to ensure that all terms have their symbol keys correctly
        // set up.
        SymbolKeyTraverser symbolKeyTraverser = new SymbolKeyTraverser(interner, symbolTable, null);
        symbolKeyTraverser.setContextChangeVisitor(symbolKeyTraverser);

        TermWalker symWalker = new TermWalker(new DepthFirstBacktrackingSearch <>(), symbolKeyTraverser, symbolKeyTraverser);
        symWalker.walk(clause);
    }

    /**
     * Finds and marks all functors within the clause that are considered to be top-level.
     *
     * @param clause The clause to top-level check.
     */
    private
    void topLevelCheck ( Term clause ) {
        TermWalker walk = TermWalkers.positionalWalker(new HiTalkTopLevelCheckVisitor(interner, symbolTable, null));
        walk.walk(clause);
    }

    /**
     * @param tokenSource
     * @param loadContext
     * @return
     */
    public
    Term[] compile ( TokenSource tokenSource, LoadContext loadContext ) {


        return new Term[0];
    }
}

