package org.ltc.hitalk.wam.compiler;


import com.thesett.aima.logic.fol.IntegerType;
import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.LiteralType;
import com.thesett.aima.logic.fol.Term;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.entities.HtPredicateDefinition;
import org.ltc.hitalk.entities.ISubroutine;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.printer.HtBasePositionalVisitor;
import org.ltc.hitalk.wam.printer.IPositionalTermTraverser;
import org.ltc.hitalk.wam.printer.IPositionalTermVisitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.ltc.hitalk.term.ListTerm.Kind.TRUE;

/**
 *
 */
public class PrologPositionalTransformVisitor extends HtBasePositionalVisitor
        implements IPositionalTermVisitor {

    /**
     * Creates a positional visitor.
     *
     * @param symbolTable The compiler symbol table.
     * @param interner    The name interner.
     */
    public PrologPositionalTransformVisitor ( SymbolTable <Integer, String, Object> symbolTable,
                                              IVafInterner interner,
                                              IPositionalTermTraverser positionalTermTraverser ) {
        super(symbolTable, interner, positionalTermTraverser);
    }

    /**
     * @param positionalTraverser
     */
    @Override
    public void setPositionalTraverser ( HtPositionalTermTraverser positionalTraverser ) {
        this.positionalTraverser = positionalTraverser;
    }

    protected void enterTerm ( Term term ) {
        super.enterTerm(term);
    }

    protected void leaveTerm ( Term term ) {
        super.leaveTerm(term);
    }

    protected void enterFunctor ( HtFunctor functor ) throws LinkageException {
        super.enterFunctor(functor);
    }

    protected void leaveFunctor ( HtFunctor functor ) {
        super.leaveFunctor(functor);
    }

    protected void enterVariable ( HtVariable variable ) {
        super.enterVariable(variable);
    }

    protected void leaveVariable ( HtVariable variable ) {
        super.leaveVariable(variable);
    }

    private void chb ( IFunctor[] body, Map <HtFunctor, List <PiCalls>> hb ) {
        chb(body, hb);
    }

    protected void enterPredicate ( HtPredicate predicate ) {
        HtPredicateDefinition <ISubroutine, HtPredicate, HtClause> def = predicate.getDefinition();
        final List <HtClause> clauses;
        int bound = def.size();
        clauses = IntStream.range(0, bound).mapToObj(i -> (HtClause) def.get(i))
                .filter(clause -> !def.isBuiltIn()).collect(Collectors.toList());
        Map <IFunctor, List <PiCalls>> hb = new HashMap <>();
        for (HtClause clause : clauses) {
            IFunctor[] body = clause.getBody();
            chb(Objects.requireNonNull(asDottedPair(body)), hb);
        }
    }

    public void setPositionalTraverser ( IPositionalTermTraverser positionalTraverser ) {

    }

    public void visit ( Term term ) {

    }

    public void visit ( HtVariable variable ) {

    }

    private static class BinFunctor {
        ListTerm.Kind kind;
        BinFunctor left;
        BinFunctor right;

        /**
         * @return
         */
        public ListTerm.Kind getKind () {
            return kind;
        }

        public BinFunctor getLeft () {
            return left;
        }

        public BinFunctor getRight () {
            return right;
        }
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
            final ListTerm dp = (ListTerm) body.get(i);
            switch (dp.getKind()) {
                case NOT:
//                    final IFunctor g1 = (IFunctor);

//                    chb0((IFunctor) dp.getHeads()[i], hb);
                case AND:
                case OR:
//                    chb0(j + 1, body, hb);//FALLING DOWN
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

    private void chb0 ( ListTerm.Kind kind, IFunctor g1, Map <IFunctor, List <PiCalls>> hb ) {

    }

    protected void leavePredicate ( HtPredicate predicate ) {
        super.leavePredicate(predicate);
    }

    protected void enterClause ( HtClause clause ) throws LinkageException {
        super.enterClause(clause);
    }

    protected void leaveClause ( HtClause clause ) {
        super.leaveClause(clause);
    }

    protected void enterIntLiteral ( IntegerType literal ) {
        super.enterIntLiteral(literal);
    }

    protected void leaveIntLiteral ( IntegerType literal ) {
        super.leaveIntLiteral(literal);
    }

    protected void enterLiteral ( LiteralType literal ) {
        super.enterLiteral(literal);
    }

    protected void leaveLiteral ( LiteralType literal ) {
        super.leaveLiteral(literal);
    }
}
