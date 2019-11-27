package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.Resolver;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.entities.HtPredicateDefinition;
import org.ltc.hitalk.entities.ISubroutine;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.IntTerm;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.printer.HtLiteralType;
import org.ltc.hitalk.wam.printer.IPositionalTermTraverser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.ltc.hitalk.term.ListTerm.Kind.TRUE;

/**
 *
 */
public class PiCallsCollectorVisitor extends MetaInterpreterVisitor {

    /**
     * Creates a positional visitor.
     * @param symbolTable The compiler symbol table.
     * @param interner    The name interner.
     * @param resolver
     */
    protected PiCallsCollectorVisitor ( SymbolTable <Integer, String, Object> symbolTable,
                                        IVafInterner interner,
                                        Resolver <HtPredicate, HtClause> resolver,
                                        IPositionalTermTraverser traverser ) {
        super(symbolTable, interner, resolver, traverser);
    }

    /**
     * @param predicate The predicate being entered.
     */
    @Override
    protected void enterPredicate ( HtPredicate predicate ) {
        HtPredicateDefinition <ISubroutine, HtPredicate, HtClause> def = predicate.getDefinition();
        final List <HtClause> clauses;
        int bound = def.size();
        clauses = IntStream.range(0, bound).mapToObj(i -> (HtClause) def.get(i))
                .filter(clause -> def.isBuiltIn()).collect(Collectors.toList());
        Map <IFunctor, List <PiCalls>> hb = new HashMap <>();
        for (HtClause clause : clauses) {
            ListTerm body = clause.getBodyAsListTerm();
            chb(body, hb);
        }
    }

    @Override
    protected void leavePredicate ( HtPredicate predicate ) {
        super.leavePredicate(predicate);
    }

    private ListTerm asDottedPair ( IFunctor[] body ) {
        switch (body.length) {
            case 0:
                ListTerm.Kind kind = TRUE;
                break;
            case 1:

                break;
            default:

                break;
        }

        return null;
    }

    private void chb ( ListTerm body, Map <IFunctor, List <PiCalls>> hb ) {
        for (int i = 0, bodyLength = body.size(); i < bodyLength; i++) {
            final ListTerm goal = (ListTerm) body.get(i);
            switch (goal.getKind()) {
                case NOT:
//                    final IFunctor g1 = (IFunctor);
//                    HtBasePositionalVisitor metaint = new HtBasePositionalVisitor()
//                    chb0((IFunctor) goal.get(i), hb);
                case AND:
                case OR:
//                    chb0(j + 1, body, hb);//FALLING DOWN
                case IF:

                case GOAL:

                    break;
                case INLINE_GOAL:
                    break;
                case OTHER:
                    break;
                default:
                    throw new ExecutionError(ExecutionError.Kind.PERMISSION_ERROR, null);
            }
        }
    }

    protected void enterClause ( HtClause clause ) throws LinkageException {
        super.enterClause(clause);
    }

    @Override
    protected void leaveClause ( HtClause clause ) {
        super.leaveClause(clause);
    }

    @Override
    protected void enterListTerm ( ListTerm listTerm ) throws LinkageException {
        super.enterListTerm(listTerm);
    }

    protected void leaveListTerm ( ListTerm listTerm ) {
        super.leaveListTerm(listTerm);
    }

    protected void enterFunctor ( IFunctor functor ) throws LinkageException {
        super.enterFunctor(functor);
    }

    @Override
    protected void leaveFunctor ( IFunctor functor ) {
        super.leaveFunctor(functor);
    }


    protected void enterGoal ( IFunctor functor ) {
        super.enterGoal(functor);
    }

    protected boolean filterGoal ( IFunctor symbol, ITerm[] args ) {
        return super.filterGoal(symbol, args);
    }

    public void visit ( IntTerm term ) {

    }

    public void visit ( HtLiteralType term ) {

    }
}
