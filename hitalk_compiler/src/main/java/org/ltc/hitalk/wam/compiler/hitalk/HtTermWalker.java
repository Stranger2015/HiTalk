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

import com.thesett.aima.logic.fol.TermTraverser;
import com.thesett.aima.logic.fol.TermVisitor;
import com.thesett.aima.search.QueueBasedSearchMethod;
import com.thesett.aima.search.util.Searches;
import com.thesett.common.util.logic.UnaryPredicate;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ITermVisitor;

import java.util.Iterator;

public class HtTermWalker {
    /**
     * Holds the search that order the walk.
     */
    private final QueueBasedSearchMethod <ITerm, ITerm> search;

    /**
     * Holds the traverser to expand selected nodes with context.
     */
    private final TermTraverser traverser;

    /**
     * Holds the visitor to apply to every goal node discovered by the search.
     */
    private final ITermVisitor visitor;

    /**
     * Holds an optional goal predicate.
     */
    private UnaryPredicate <ITerm> goalPredicate;

    /**
     * Creates a term walker using the specified search, traverser and visitor.
     *
     * @param search    The search to order the walk by.
     * @param traverser The traverser to expand nodes and supply context with.
     * @param visitor   The visitor to apply to every goal node encountered.
     */
    public HtTermWalker ( QueueBasedSearchMethod <ITerm, ITerm> search, TermTraverser traverser, ITermVisitor visitor ) {
        this.search = search;
        this.traverser = traverser;
        this.visitor = visitor;
    }

    /**
     * Sets up a goal predicate to be applied on the underlying queue based search method.
     *
     * @param goalPredicate The predicate that evaluates search states to check if they are goals.
     */
    public void setGoalPredicate ( UnaryPredicate <ITerm> goalPredicate ) {
        this.goalPredicate = goalPredicate;
    }

    /**
     * Walks over the supplied term.
     *
     * @param term The term to walk over.
     */
    public void walk ( ITerm term ) {
        // Set up the traverser on the term to walk over.
        term.setTermTraverser(traverser);

        // Create a fresh search starting from the term.
        search.reset();

        if (goalPredicate != null) {
            search.setGoalPredicate(goalPredicate);
        }

        search.addStartState(term);

        Iterator <ITerm> treeWalker = Searches.allSolutions(search);

        // If the traverser is a term visitor, allow it to visit the top-level term in the walk to establish
        // an initial context.
        if (traverser instanceof TermVisitor) {
            term.accept((ITermVisitor) traverser);
        }

        // Visit every goal node discovered in the walk over the term.
        while (treeWalker.hasNext()) {
            ITerm nextTerm = treeWalker.next();
            nextTerm.accept(visitor);
        }

        // Remote the traverser on the term to walk over.
        term.setTermTraverser(null);
    }
}
