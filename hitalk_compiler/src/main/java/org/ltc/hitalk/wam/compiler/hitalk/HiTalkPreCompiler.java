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


import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.search.util.backtracking.DepthFirstBacktrackingSearch;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtSourceCodeException;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.HtMethod;
import org.ltc.hitalk.wam.compiler.prolog.ICompilerObserver;
import org.ltc.hitalk.wam.compiler.prolog.PrologBuiltInTransform;
import org.ltc.hitalk.wam.compiler.prolog.PrologDefaultBuiltIn;
import org.ltc.hitalk.wam.compiler.prolog.PrologPreCompiler;

import java.util.List;

import static org.ltc.hitalk.wam.compiler.HtTermWalkers.positionalWalker;

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
class HiTalkPreCompiler<T extends HtMethod, P, Q> extends PrologPreCompiler <T, P, Q> implements IPreCompiler {
    /**
     * Holds the compiler output observer.
     */
    protected ICompilerObserver <P, Q> observer;

    /**
     * Creates a new PreCompiler.
     *
     * @param symbolTable      The symbol table.
     * @param interner         The machine to translate functor and variable names.
     * @param builtInTransform
     * @param defaultBuiltIn   The default built in, for standard compilation and interners and symbol tables.
     * @param parser
     * @param resolver
     */
    public HiTalkPreCompiler(ISymbolTable<Integer, String, Object> symbolTable,
                             IVafInterner interner,
                             PrologBuiltInTransform<T, P, Q> builtInTransform,
                             PrologDefaultBuiltIn defaultBuiltIn,
                             IResolver<HtPredicate, HtClause> resolver,
                             PlPrologParser parser) {
        super(symbolTable, interner, defaultBuiltIn, builtInTransform, resolver, parser);
    }

    /**
     * {@inheritDoc}
     *
     * @param sentence
     */
    public abstract void compile ( Sentence <T> sentence ) throws HtSourceCodeException;


    /**
     * @param clauses
     */
    protected abstract void saveResult ( List <T> clauses );

    /**
     * @param t
     * @return
     */
    protected abstract List <T> preprocess ( T t );


    /**
     * Runs a symbol key traverser over the clause to be compiled, to ensure that all of its terms and sub-terms have
     * their symbol keys initialised.
     *
     * @param clause The clause to initialise the symbol keys of.
     */
    private void initializeSymbolTable ( ITerm clause ) {
        // Run the symbol key traverser over the clause, to ensure that all terms have their symbol keys correctly
        // set up.
        HtSymbolKeyTraverser symbolKeyTraverser = new HtSymbolKeyTraverser(interner, symbolTable, null);
        symbolKeyTraverser.setContextChangeVisitor(symbolKeyTraverser);

        HtTermWalker symWalker = new HtTermWalker(new DepthFirstBacktrackingSearch <>(), symbolKeyTraverser, symbolKeyTraverser);
        symWalker.walk(clause);
    }

    /**
     * Finds and marks all functors within the clause that are considered to be top-level.
     *
     * @param clause The clause to top-level check.
     */
    private void topLevelCheck ( ITerm clause ) {
        HtTermWalker walk = positionalWalker(new HiTalkTopLevelCheckVisitor(symbolTable, interner, null));
        walk.walk(clause);
    }

    public ICompilerObserver <P, Q> getObserver () {
        return observer;
    }
}

