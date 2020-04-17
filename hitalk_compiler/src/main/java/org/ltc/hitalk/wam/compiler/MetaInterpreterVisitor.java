package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.LinkageException;
import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.core.BaseApp;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.printer.HtBasePositionalVisitor;
import org.ltc.hitalk.wam.printer.IPositionalTermTraverser;
import org.ltc.hitalk.wam.printer.IPositionalTermVisitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;
import static org.ltc.hitalk.core.utils.TermUtilities.unifyB;

/**
 *
 */
public abstract class MetaInterpreterVisitor<T extends HtClause, P, Q, PC, QC> extends HtBasePositionalVisitor
        implements IPositionalTermVisitor {

    protected final IResolver <P, Q> resolver;
    protected IPositionalTermTraverser positionalTraverser;
    protected final List <T> clauses = new ArrayList <>();
    protected final Set <HtVariable> bindings = new HashSet <>();

    /**
     * Creates a positional visitor.
     *
     * @param symbolTable The compiler symbol table.
     * @param interner    The name interner.
     */
    protected MetaInterpreterVisitor ( ISymbolTable <Integer, String, Object> symbolTable,
                                       IVafInterner interner,
                                       IResolver <P, Q> resolver,
                                       IPositionalTermTraverser traverser ) {
        super(symbolTable, interner, traverser);
        this.resolver = resolver;
    }

    /**
     * @param positionalTraverser
     */
    public void setPositionalTraverser ( IPositionalTermTraverser positionalTraverser ) {
        this.positionalTraverser = positionalTraverser;
    }

    /**
     * @param predicate The predicate being entered.
     */
    @Override
    protected void enterPredicate ( HtPredicate predicate ) {
        //todo op stack ==> term.accept(this);
        int bound = predicate.size();
        IntStream.range(0, bound).mapToObj(predicate::get).forEachOrdered(clause ->
                clause.accept(this));

    }

    /**
     * @param predicate
     */
    @Override
    protected void leavePredicate ( HtPredicate predicate ) {

    }

    /**
     * body may be encoded AS DOTTED PAIR (first goal) OR AS IS
     *
     * @param clause The clause being entered.
     */
    @Override
    protected void enterClause ( HtClause clause ) throws LinkageException {
        IntStream.range(0, clause.bodyLength()).mapToObj(clause::getGoal).forEachOrdered(goal ->
                goal.accept(this));
    }

    /**
     * @param clause The clause being left.
     */
    @Override
    protected void leaveClause ( HtClause clause ) {

    }

    /**
     * @throws LinkageException
     */
    @Override
    protected void enterListTerm(ListTerm listTerm) throws LinkageException {
        for (int i = 0, headsSize = listTerm.size(); i <= headsSize; i++) {
            switch (listTerm.getKind()) {
                case NOT:
                    listTerm.getHead(i).accept(this);
                    break;
                case AND://fall down
                case OR:
                case IF:
                    listTerm.getHead(i).accept(this);
                    listTerm.getHead(i + 1).accept(this);
                    break;
                case GOAL:
                    IFunctor goal = (IFunctor) listTerm.getHead(i);
                    if (goal.isDefined()) {
                        final Set<PiCalls<?>> hbSet = new HashSet<>();
                        lookupGoal((PiCalls) goal, hbSet);
                    }
                    break;
                case TRUE:
                    break;
                case INLINE_GOAL:
                    break;
                case OTHER:
                    break;
                default:
                    throw new ExecutionError(PERMISSION_ERROR, null);
            }
        }
    }

    public void lookupGoal ( PiCalls <?> goal, Set <PiCalls <?>> hbSet ) throws LinkageException {
        ITermFactory factory = BaseApp.getAppContext().getTermFactory();
        goal.setMgCalls(factory.newVariable("Calls"));
        final HtFunctor eqf = new HtFunctor(interner.internFunctorName("=", 2), new ListTerm(0));// fixme
//        final HtClause<IFunctor> query = new HtClause<>(null, new ListTerm( LIST,eqf));?
        eqf.setArgument(0, goal);
        boolean unifies = true;
        for (PiCalls <?> hbEl : hbSet) {
            eqf.setArgument(1, hbEl);//fixme
            unifies = unifyB(eqf.getArgument(0), eqf.getArgument(1));
            if (unifies) {
//                this.bindings.addAll(bindings);
                return;
            }
        }

        hbSet.add(goal);//if not found just add
    }

    @Override
    protected void leaveListTerm ( ListTerm listTerm ) {

    }

    @Override
    protected void enterFunctor ( IFunctor functor ) throws LinkageException {
        if (!functor.isBracketed() && functor.isListTerm()) {
            enterListTerm((ListTerm) functor);
        } else {
            enterGoal(functor);//fixme redundant
        }
    }

    /**
     * @param functor The functor being left.
     */
    @Override
    protected void leaveFunctor ( IFunctor functor ) {

    }

    /**
     * @param functor
     */
    protected void enterGoal ( IFunctor functor ) {

    }

    /**
     * @param functor
     */
    protected void leaveGoal ( IFunctor functor ) {

    }

    /**
     * @param sym
     * @param args
     */
    protected boolean filterGoal ( IFunctor sym, ITerm[] args ) {
//        lookupGoal(sym, args);
        return true;
    }// ->	% partially instantiated (HiLog) call
}