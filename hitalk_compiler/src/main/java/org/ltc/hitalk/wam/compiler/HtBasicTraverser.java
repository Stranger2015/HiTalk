package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.search.Operator;
import com.thesett.aima.search.util.backtracking.Reversable;
import com.thesett.common.util.StackQueue;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.entities.ISubroutine;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.printer.IClauseTraverser;
import org.ltc.hitalk.wam.printer.IClauseVisitor;
import org.ltc.hitalk.wam.printer.IFunctorTraverser;
import org.ltc.hitalk.wam.printer.IPredicateTraverser;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * BasicTraverser provides methods to produce reversible {@link Operator}s to transition over the structure of a
 * {@link Term}. Structurally terms are built up as clauses, and functors, and default methods are provided to iterate
 * over these constructions, with flags to set to determines whether heads or bodies of clauses are explored first, and
 * whether the arguments of functors are explored left-to-right or right-to-left.
 * <p>
 * <p/>A BasicTraverser can be extended to form a concrete traverser over terms by providing implementations of the
 * methods to create operators for traversing into clause heads or bodies, and the arguments of functors. Any optional
 * visit method to visit the root of a clause being traversed may be also be implemented, usually to initialize the root
 * context of the traversal.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Establish an initial positional context upon visiting a clause.
 * <tr><td> Provide traversal operators over clauses.
 * <tr><td> Provide traversal operators over functors.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public abstract class HtBasicTraverser implements
        IPredicateTraverser,
        IClauseTraverser,
        IFunctorTraverser,
        IClauseVisitor {

    /** Used for debugging purposes. */
    /* private static final Logger log = Logger.getLogger(BasicTraverser.class.getName()); */

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
    public HtBasicTraverser () {
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
    public HtBasicTraverser ( boolean clauseHeadFirst, boolean leftToRightClauseBodies, boolean leftToRightFunctorArgs ) {
        this.clauseHeadFirst = clauseHeadFirst;
        this.leftToRightClauseBodies = leftToRightClauseBodies;
        this.leftToRightFunctorArgs = leftToRightFunctorArgs;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p/>Can be used to visit a predicate, to set up an initial context for predicate traversals.
     */
    public abstract void visit ( HtPredicate predicate );

    /**
     * {@inheritDoc}
     * <p>
     * <p/>Can be used to visit a clause, to set up an initial context for clause traversals.
     */
    public abstract void visit ( HtClause clause ) throws LinkageException;

    /**
     * {@inheritDoc}
     */
    public Iterator <Operator <ITerm>> traverse ( HtPredicate predicate, boolean reverse ) {
        /*log.fine("Traversing predicate " + predicate.toString());*/

        ISubroutine[] body = predicate.getDefinition().getBody();

        Queue <Operator <ITerm>> queue = (!reverse) ? new StackQueue <>() : new LinkedList <>();

        // For the predicate bodies.
        for (int i = leftToRightPredicateBodies ? 0 : (body.length - 1);
             leftToRightPredicateBodies ? (i < body.length) : (i >= 0);
             i += (leftToRightPredicateBodies ? 1 : -1)) {
            HtClause bodyClause = (HtClause) body[i];

            bodyClause.setReversable(createClauseOperator(bodyClause, i, (HtClause[]) body, predicate));
            bodyClause.setTermTraverser(this);
            queue.offer((ITerm) bodyClause);//fixme
        }

        return queue.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public Iterator <Operator <ITerm>> traverse ( HtClause clause, boolean reverse ) {
        /*log.fine("Traversing clause " + clause.toString());*/

        IFunctor head = clause.getHead();
        IFunctor[] body = clause.getBody();

        Queue <Operator <ITerm>> queue = (!reverse) ? new StackQueue <>() : new LinkedList <>();

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
                IFunctor bodyFunctor = body[i];

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
    public Iterator <Operator <ITerm>> traverse ( IFunctor functor, boolean reverse ) {
        /*log.fine("Traversing functor " + functor.toString());*/

        Queue <Operator <ITerm>> queue = (!reverse) ? new StackQueue <>() : new LinkedList <>();

        ITerm[] arguments = functor.getArguments();

        // For a top-level functor clear top-level flag, so that child functors are not taken as top-level.
        for (int i = leftToRightFunctorArgs ? 0 : (arguments.length - 1);
             leftToRightFunctorArgs ? (i < arguments.length) : (i >= 0);
             i += (leftToRightFunctorArgs ? 1 : -1)) {
            ITerm argument = arguments[i];
            argument.setReversable(createTermOperator(argument, i, functor));
            argument.setTermTraverser(this);
            queue.offer(argument);
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
    protected abstract StackableOperator createHeadOperator ( IFunctor head, HtClause clause );

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
    protected abstract StackableOperator createBodyOperator ( IFunctor bodyFunctor,
                                                              int pos,
                                                              IFunctor[] body,
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
    protected abstract StackableOperator createClauseOperator ( HtClause bodyClause, int pos,
                                                                HtClause[] body,
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
    protected abstract StackableOperator createTermOperator ( ITerm argument, int pos, IFunctor functor );

    /**
     * StackableOperator is a {@link Reversable} operator that can also delegates to another stackable operation by
     * default. This allows stack of operators to be built and applied or undone on every state transition.
     *
     * <pre><p/><table id="crc"><caption>CRC Card</caption>
     * <tr><th> Responsibilities <th> Collaborations
     * <tr><td> Delegate to a stackable operation.
     * </table></pre>
     */
    protected class StackableOperator implements Reversable {
        /**
         * The optional stackable delegate operation.
         */
        StackableOperator delegate;

        /**
         * Creates an reversible operator with a delegate.
         *
         * @param delegate An optional delegate to chain onto, may be <tt>null</tt>.
         */
        public StackableOperator ( StackableOperator delegate ) {
            this.delegate = delegate;
        }

        /**
         * {@inheritDoc}
         */
        public void applyOperator () {
            if (delegate != null) {
                delegate.applyOperator();
            }
        }

        /**
         * {@inheritDoc}
         */
        public void undoOperator () {
            if (delegate != null) {
                delegate.undoOperator();
            }
        }
    }
}