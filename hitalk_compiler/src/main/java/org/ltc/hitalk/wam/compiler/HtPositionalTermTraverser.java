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
package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.TermVisitor;
import com.thesett.aima.logic.fol.compiler.PositionalContext;
import com.thesett.aima.search.Operator;
import com.thesett.common.util.StackQueue;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.entities.HtPredicateDefinition;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.printer.IBasicTraverser;
import org.ltc.hitalk.wam.printer.IPositionalTermTraverser;

import java.util.Iterator;

/**
 * PositionalTermTraverserImpl provides contextual traversal of a term with additional information about the current
 * position of the traversal, as defined in {@link HtPositionalTermTraverser}; it provides a set of flags and properties
 * as a term tree is walked over, to indicate some positional properties of the current term within the tree.
 * <p>
 * twice,
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Establish an initial positional context upon visiting a clause.
 * <tr><td> Report whether the current term is a functor at the top-level within a clause.
 * <tr><td> Report whether the current term is within a clause head or body.
 * <tr><td> Report whether a top-level functor in a clause body is the last one in the body.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public
class HtPositionalTermTraverser extends HtBasicTraverser
        implements IPositionalTermTraverser {
    /**
     * Flag used to indicate that a term context is being entered.
     */
    protected boolean enteringContext;

    /**
     * Flag used to indicate that a term context is being left.
     */
    protected boolean leavingContext;

    /**
     * Holds an optional visitor to notify on context changes.
     */
    protected TermVisitor contextChangeVisitor;

    /**
     * Holds the context stack for the traversal.
     */
    protected StackQueue <HtPositionalContextOperator> contextStack = new StackQueue <>();

    /**
     * Inidicates that the initial context has been established at the start of a traversal.
     */
    private boolean initialContextCreated;

    /**
     * Creates a traverser that uses the normal intuitive left-to-right traversal orderings for clauses and functors.
     */
    public HtPositionalTermTraverser () {
        clauseHeadFirst = true;
        leftToRightClauseBodies = true;
        leftToRightFunctorArgs = true;
    }

    /**
     * Creates a traverser that uses the defubes left-to-right traversal orderings for clauses and functors.
     *
     * @param clauseHeadFirst         <tt>true</tt> to use the normal ordering, <tt>false</tt> for the reverse.
     * @param leftToRightClauseBodies <tt>true</tt> to use the normal ordering, <tt>false</tt> for the reverse.
     * @param leftToRightFunctorArgs  <tt>true</tt> to use the normal ordering, <tt>false</tt> for the reverse.
     */
    public HtPositionalTermTraverser ( boolean clauseHeadFirst, boolean leftToRightClauseBodies,
                                       boolean leftToRightFunctorArgs ) {
        this.clauseHeadFirst = clauseHeadFirst;
        this.leftToRightClauseBodies = leftToRightClauseBodies;
        this.leftToRightFunctorArgs = leftToRightFunctorArgs;
    }

    /**
     * {@inheritDoc} Visits a predicate, to set up an initial context for clause traversals.
     */
    public
    void visit ( HtPredicate predicate ) {
        // Set up the initial context, if this is the top-level of the traversal.
        createInitialContext(predicate);
    }

    /**
     * {@inheritDoc} Visits a clause, to set up an initial context for clause traversals.
     */
    public
    void visit ( HtClause clause ) {
        // Set up the initial context, if this is the top-level of the traversal.
        createInitialContext(clause);
    }

    /**
     * {@inheritDoc}
     */
    public
    void visit ( Term term ) {
    }

    /**
     * {@inheritDoc}
     */
    public
    boolean isTopLevel () {
        HtPositionalTermTraverser.HtPositionalContextOperator position = contextStack.peek();

        return (position != null) && position.isTopLevel();
    }

    /**
     * {@inheritDoc}
     */
    public
    boolean isInHead () {
        HtPositionalTermTraverser.HtPositionalContextOperator position = contextStack.peek();

        return (position != null) && position.isInHead();
    }

    /**
     * {@inheritDoc}
     */
    public
    boolean isLastBodyFunctor () {
        HtPositionalTermTraverser.HtPositionalContextOperator position = contextStack.peek();

        return (position != null) && position.isLastBodyFunctor();
    }

    /**
     * {@inheritDoc}
     */
    public
    Term getTerm () {
        HtPositionalContextOperator position = contextStack.peek();

        return (position != null) ? position.getTerm() : null;
    }

    /**
     * {@inheritDoc}
     */
    public
    int getPosition () {
        HtPositionalContextOperator position = contextStack.peek();

        return (position != null) ? position.getPosition() : -1;
    }

    /**
     * {@inheritDoc}
     */
    public
    boolean isEnteringContext () {
        return enteringContext;
    }

    /**
     * {@inheritDoc}
     */
    public
    boolean isLeavingContext () {
        return leavingContext;
    }

    /**
     * {@inheritDoc}
     */
    public
    boolean isContextChange () {
        return enteringContext || leavingContext;
    }

    /**
     * {@inheritDoc}
     */
    public
    PositionalContext getParentContext () {
        HtPositionalContextOperator position = contextStack.peek();

        return (position != null) ? position.getParentContext() : null;
    }

    /**
     * {@inheritDoc}
     */
    public
    void setContextChangeVisitor ( TermVisitor contextChangeVisitor ) {
        this.contextChangeVisitor = contextChangeVisitor;
    }

    /**
     * Prints the position of this traverser, mainly for debugging purposes.
     *
     * @return The position of this traverser, mainly for debugging purposes.
     */
    public
    String toString () {
        return "BasicTraverser: [ topLevel = " + isTopLevel() + ", inHead = " + isInHead() + ", lastBodyfunctor = " +
                isLastBodyFunctor() + ", enteringContext = " + enteringContext + ", leavingContext = " + leavingContext +
                " ]";
    }

    /**
     * {@inheritDoc}
     */
    protected IBasicTraverser.StackableOperator createHeadOperator ( Functor head, HtClause clause ) {
        return new HtPositionalTermTraverser.HtPositionalContextOperator(head, -1, true, true, false, null, contextStack.peek());
    }

    /**
     * {@inheritDoc}
     */
    protected
    StackableOperator createBodyOperator ( Functor bodyFunctor, int pos, Functor[] body, HtClause clause ) {
        return new HtPositionalTermTraverser.HtPositionalContextOperator(bodyFunctor, pos, true, false, pos == (body.length - 1), null,
                contextStack.peek());
    }

    /**
     * When traversing the body clauses of a predicate, creates a reversible operator to use to transition into each
     * body clause.
     *
     * @param bodyClause The body clause to transition into.
     * @param pos        The position of the body clause within the body.
     * @param body       The containing body.
     * @param predicate  The containing predicate.
     * @return A reversable operator.
     */
    @Override
    protected
    StackableOperator createClauseOperator ( HtClause bodyClause, int pos, HtPredicateDefinition body, HtPredicate predicate ) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    protected
    StackableOperator createTermOperator ( Term argument, int pos, Functor functor ) {
        return new HtPositionalTermTraverser.HtPositionalContextOperator(argument, pos, false, null, false, null, contextStack.peek());
    }

    /**
     * {@inheritDoc}
     */
    protected
    StackableOperator createClauseOperator ( HtClause bodyClause, int pos, HtClause[] body, HtPredicate predicate ) {
        return new HtPositionalTermTraverser.HtPositionalContextOperator(bodyClause, pos, false, false, false, null, contextStack.peek());
    }

    /**
     * Sets up the initial context once, at the start of a traversal.
     *
     * @param term The term to establish the initial positional traversal context for.
     */
    private
    void createInitialContext ( Term term ) {
        if (!initialContextCreated) {
            HtPositionalTermTraverser.HtPositionalContextOperator initialContext =
                    new HtPositionalTermTraverser.HtPositionalContextOperator(term, -1, false, false, false, null, contextStack.peek());
            contextStack.offer(initialContext);
            term.setReversable(initialContext);
            initialContextCreated = true;
        }
    }

    /**
     * Visits a clause.
     *
     * @param clause  The clause to visit.
     * @param reverse <tt>true</tt> if the child operators should be presented in reverse order to what is deemed to be
     *                a natural, left-to-right ordering.
     * @return An iterator over operators producing the traveresed elements of the clause.
     */
    @Override
    public
    Iterator <Operator <Term>> traverse ( HtClause clause, boolean reverse ) {
        return super.traverse(clause, reverse);
    }

    /**
     * HtPositionalContextOperator is a {@link StackableOperator} that passes on state changes when the term it is
     * attached to is traversed into. Although it implements {@link StackableOperator} it does not undo state changes
     * when back-tracking over the term, as it is assumed that successive terms will overwrite state as they need to.
     *
     * <pre><p/><table id="crc"><caption>CRC Card</caption>
     * <tr><th> Responsibilities <th> Collaborations
     * <tr><td> Optionally set the top-level, in-head, or last functor flags on the traverser.
     *     <td> {@link HtPositionalTermTraverser}.
     * </table></pre>
     */
    private
    class HtPositionalContextOperator extends StackableOperator implements PositionalContext {
        /**
         * Holds the term that this is the context operator for.
         */
        Term term;

        /**
         * Holds the 'position' within the parent term.
         */
        Integer position;

        /**
         * The state of the top-level flag to establish.
         */
        Boolean topLevel;

        /**
         * The state of the in-head flag to establish.
         */
        Boolean inHead;

        /**
         * The state of the last functor flag to establish.
         */
        Boolean lastBodyFunctor;

        /**
         * The parent context if any.
         */
        HtPositionalContextOperator parent;

        /**
         * Creates a context establishing operation.
         *
         * @param term            The term that this is the context operator for.
         * @param position        The 'position' within the parent term.
         * @param topLevel        <tt>true</tt> to set flag, <tt>false</tt> to clear, <tt>null</tt> to leave alone.
         * @param inHead          <tt>true</tt> to set flag, <tt>false</tt> to clear, <tt>null</tt> to leave alone.
         * @param lastBodyFunctor <tt>true</tt> to set flag, <tt>false</tt> to clear, <tt>null</tt> to leave alone.
         * @param delegate        A stackable operator to chain onto this one.
         */
        private
        HtPositionalContextOperator ( Term term, Integer position, Boolean topLevel, Boolean inHead,
                                      Boolean lastBodyFunctor, StackableOperator delegate, HtPositionalTermTraverser.HtPositionalContextOperator parent ) {
            super(delegate);

            this.term = term;
            this.position = position;
            this.topLevel = topLevel;
            this.inHead = inHead;
            this.lastBodyFunctor = lastBodyFunctor;
            this.parent = parent;
        }

        /**
         * {@inheritDoc}
         */
        public
        void applyOperator () {
            HtPositionalTermTraverser.HtPositionalContextOperator previousContext = contextStack.peek();

            if (previousContext == null) {
                previousContext = new HtPositionalTermTraverser.HtPositionalContextOperator(null, -1, false, false, false, null, previousContext);
            }

            topLevel = (topLevel == null) ? previousContext.topLevel : topLevel;
            inHead = (inHead == null) ? previousContext.inHead : inHead;
            lastBodyFunctor = (lastBodyFunctor == null) ? previousContext.lastBodyFunctor : lastBodyFunctor;
            position = (position == null) ? previousContext.position : position;

            contextStack.offer(this);

            if (HtPositionalTermTraverser.this.contextChangeVisitor != null) {
                HtPositionalTermTraverser.this.enteringContext = true;
                term.accept(HtPositionalTermTraverser.this.contextChangeVisitor);
                HtPositionalTermTraverser.this.enteringContext = false;
            }

            super.applyOperator();
        }

        /**
         * {@inheritDoc}
         */
        public
        void undoOperator () {
            super.undoOperator();

            if (HtPositionalTermTraverser.this.contextChangeVisitor != null) {
                HtPositionalTermTraverser.this.leavingContext = true;
                term.accept(HtPositionalTermTraverser.this.contextChangeVisitor);
                HtPositionalTermTraverser.this.leavingContext = false;
            }

            contextStack.poll();
        }

        /**
         * {@inheritDoc}
         */
        public
        boolean isTopLevel () {
            return topLevel;
        }

        /**
         * {@inheritDoc}
         */
        public
        boolean isInHead () {
            return inHead;
        }

        /**
         * {@inheritDoc}
         */
        public
        boolean isLastBodyFunctor () {
            return lastBodyFunctor;
        }

        /**
         * {@inheritDoc}
         */
        public
        Term getTerm () {
            return term;
        }

        /**
         * {@inheritDoc}
         */
        public
        int getPosition () {
            return position;
        }

        /**
         * {@inheritDoc}
         */
        public
        PositionalContext getParentContext () {
            return parent;
        }
    }
}
