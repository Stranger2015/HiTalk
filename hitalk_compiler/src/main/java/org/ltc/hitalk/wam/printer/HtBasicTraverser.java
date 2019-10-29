package org.ltc.hitalk.wam.printer;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.FunctorTraverser;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.search.Operator;
import com.thesett.aima.search.util.backtracking.Reversable;
import com.thesett.common.util.StackQueue;
import org.ltc.hitalk.compiler.HtPredicateVisitor;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.entities.HtPredicateDefinition;
import org.ltc.hitalk.parser.HtClause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public abstract
class HtBasicTraverser implements HtPredicateTraverser,
        HtClauseTraverser,
        FunctorTraverser,
        HtClauseVisitor,
        HtPredicateVisitor {
    /** Used for debugging purposes. */
    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    /**
     * Flag used to indicate that clause heads should come before bodies in the left-to-right ordering of clauses.
     */
    protected boolean clauseHeadFirst;

    /**
     * Flag used to indicate that clause bodies should be taken in the intuitive left-to-right ordering.
     */
    protected boolean leftToRightClauseBodies;

    /**
     * Flag used to indicate that predicate bodies should be taken in the intuitive left-to-right ordering.
     */
    protected boolean leftToRightPredicateBodies;

    /**
     * Flag used to indicate that functor arguments should be taken in the intuitive left-to-right ordering.
     */
    protected boolean leftToRightFunctorArgs;

    /**
     * Creates a traverser that uses the normal intuitive left-to-right traversal orderings for clauses and functors.
     */
    public
    HtBasicTraverser () {
        clauseHeadFirst = true;
        leftToRightClauseBodies = true;
        leftToRightFunctorArgs = true;
        leftToRightPredicateBodies = true;
    }

    /**
     * Creates a traverser that uses the defined left-to-right traversal orderings for clauses and functors.
     *
     * @param clauseHeadFirst         <tt>true</tt> to use the normal ordering, <tt>false</tt> for the reverse.
     * @param leftToRightClauseBodies <tt>true</tt> to use the normal ordering, <tt>false</tt> for the reverse.
     * @param leftToRightFunctorArgs  <tt>true</tt> to use the normal ordering, <tt>false</tt> for the reverse.
     */
    public
    HtBasicTraverser ( boolean clauseHeadFirst, boolean leftToRightClauseBodies, boolean leftToRightFunctorArgs ) {
        this.clauseHeadFirst = clauseHeadFirst;
        this.leftToRightClauseBodies = leftToRightClauseBodies;
        this.leftToRightFunctorArgs = leftToRightFunctorArgs;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p/>Can be used to visit a predicate, to set up an initial context for predicate traversals.
     */
    public abstract
    void visit ( HtPredicate predicate );

    /**
     * {@inheritDoc}
     * <p>
     * <p/>Can be used to visit a clause, to set up an initial context for clause traversals.
     */
    public abstract
    void visit ( HtClause clause );

    /**
     * {@inheritDoc}
     */
    public
    Iterator <Operator <Term>> traverse ( HtPredicate predicate, boolean reverse ) {
        logger.debug("Traversing predicate " + predicate.toString());

        HtPredicateDefinition body = predicate.getDefinition();

        Queue <Operator <Term>> queue = (!reverse) ? new StackQueue <>() : new LinkedList <>();

        // For the predicate bodies.
        for (int i = leftToRightPredicateBodies ? 0 : (body.size() - 1);
             leftToRightPredicateBodies ? (i < body.size()) : (i >= 0);
             i = i + (leftToRightPredicateBodies ? 1 : -1)) {
            HtClause bodyClause = (HtClause) body.get(i);

            bodyClause.setReversable(createClauseOperator(bodyClause, i, body, predicate));
            bodyClause.setTermTraverser(this);
            queue.offer(bodyClause);
        }

        return queue.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public
    Iterator <Operator <Term>> traverse ( HtClause clause, boolean reverse ) {
        logger.debug("Traversing clause " + clause.toString());

        Functor head = clause.getHead();
        Functor[] body = clause.getBody();

        Queue <Operator <Term>> queue = (!reverse) ? new StackQueue <>() : new LinkedList <>();

        // For the head functor, set the top-level flag, set in head context.
        if (head != null) {
            head.setReversable(createHeadOperator(head, clause));
            head.setTermTraverser(this);
            queue.offer(head);
        }

        // For the body functors, set the top-level flag, clear in head context.
        if (body != null) {
            for (int i = leftToRightClauseBodies ? 0 : (body.length - 1);
                 leftToRightClauseBodies ? (i < body.length) : (i >= 0); i = i + (leftToRightClauseBodies ? 1 : -1)) {
                Functor bodyFunctor = body[i];

                bodyFunctor.setReversable(createBodyOperator(bodyFunctor, i, body, clause));
                bodyFunctor.setTermTraverser(this);
                queue.offer(bodyFunctor);
            }
        }

        return queue.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public
    Iterator <Operator <Term>> traverse ( Functor functor, boolean reverse ) {
        /*log.fine("Traversing functor " + functor.toString());*/

        Queue <Operator <Term>> queue = (!reverse) ? new StackQueue <>() : new LinkedList <>();

        Term[] arguments = functor.getArguments();

        // For a top-level functor clear top-level flag, so that child functors are not taken as top-level.
        if (arguments != null) {
            for (int i = leftToRightFunctorArgs ? 0 : (arguments.length - 1);
                 leftToRightFunctorArgs ? (i < arguments.length) : (i >= 0);
                 i = i + (leftToRightFunctorArgs ? 1 : -1)) {
                Term argument = arguments[i];
                argument.setReversable(createTermOperator(argument, i, functor));
                argument.setTermTraverser(this);
                queue.offer(argument);
            }
        }

        return queue.iterator();
    }

    /**
     * When traversing the head of a clause, creates a reversible operator to use to transition into the head.
     *
     * @param head   The head to transition into.
     * @param clause The containing clause.
     * @return A reversable operator.
     */
    protected abstract
    HtBasicTraverser.StackableOperator createHeadOperator ( Functor head, HtClause clause );

    /**
     * When traversing the body functors of a clause, creates a reversible operator to use to transition into each body
     * functor.
     *
     * @param bodyFunctor The body functor to transition into.
     * @param pos         The position of the body functor within the body.
     * @param body        The containing body.
     * @param clause      The containing clause.
     * @return A reversable operator.
     */
    protected abstract StackableOperator createBodyOperator ( Functor bodyFunctor, int pos, Functor[] body,
                                                              HtClause clause );

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
    protected abstract StackableOperator createClauseOperator ( HtClause bodyClause, int pos, HtPredicateDefinition body,
                                                                HtPredicate predicate );

    /**
     * When traversing the argument terms of a functor, creates a reversible operator to use to transition into each
     * argument term.
     *
     * @param argument The argument term to transition into.
     * @param pos      The position of the argument within the functor.
     * @param functor  The containing functor.
     * @return A reversable operator.
     */
    protected abstract
    StackableOperator createTermOperator ( Term argument, int pos, Functor functor );

    /**
     * StackableOperator is a {@link Reversable} operator that can also delegates to another stackable operation by
     * default. This allows stack of operators to be built and applied or undone on every state transition.
     *
     * <pre><p/><table id="crc"><caption>CRC Card</caption>
     * <tr><th> Responsibilities <th> Collaborations
     * <tr><td> Delegate to a stackable operation.
     * </table></pre>
     */
    protected
    class StackableOperator implements Reversable {
        /**
         * The optional stackable delegate operation.
         */
        protected StackableOperator delegate;

        /**
         * Creates an reversible operator with a delegate.
         *
         * @param delegate An optional delegate to chain onto, may be <tt>null</tt>.
         */
        public
        StackableOperator ( StackableOperator delegate ) {
            this.delegate = delegate;
        }

        /**
         * {@inheritDoc}
         */
        public
        void applyOperator () {
            if (delegate != null) {
                delegate.applyOperator();
            }
        }

        /**
         * {@inheritDoc}
         */
        public
        void undoOperator () {
            if (delegate != null) {
                delegate.undoOperator();
            }
        }
    }
}
