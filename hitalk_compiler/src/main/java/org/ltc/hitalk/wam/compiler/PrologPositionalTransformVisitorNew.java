package org.ltc.hitalk.wam.compiler;


import com.thesett.aima.logic.fol.*;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.entities.HtPredicateDefinition;
import org.ltc.hitalk.entities.ISubroutine;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.PackedDottedPair;
import org.ltc.hitalk.wam.printer.HtBasePositionalVisitor;
import org.ltc.hitalk.wam.printer.HtPositionalTermTraverser;
import org.ltc.hitalk.wam.printer.HtPositionalTermVisitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.ltc.hitalk.term.PackedDottedPair.Kind.TRUE;

@Deprecated
public class PrologPositionalTransformVisitorNew extends HtBasePositionalVisitor
        implements HtPositionalTermVisitor {

    protected HtPositionalTermTraverser positionalTraverser;

    /**
     * Creates a positional visitor.
     *
     * @param symbolTable The compiler symbol table.
     * @param interner    The name interner.
     */
    public PrologPositionalTransformVisitorNew ( SymbolTable <Integer, String, Object> symbolTable,
                                                 IVafInterner interner ) {
        super(symbolTable, interner);
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

    protected void enterVariable ( Variable variable ) {
        super.enterVariable(variable);
    }

    protected void leaveVariable ( Variable variable ) {
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

    private static class BinFunctor {
        PackedDottedPair.Kind kind;
        BinFunctor left;
        BinFunctor right;

        /**
         * @return
         */
        public PackedDottedPair.Kind getKind () {
            return kind;
        }

        public BinFunctor getLeft () {
            return left;
        }

        public BinFunctor getRight () {
            return right;
        }
    }

    private PackedDottedPair asDottedPair ( IFunctor[] body ) {
        switch (body.length) {
            case 0:
                PackedDottedPair.Kind kind = TRUE;
                break;
            case 1:

                break;
            default:

                break;
        }

        return null;
    }

    private void chb ( PackedDottedPair body, Map <IFunctor, List <PiCalls>> hb ) {
        for (int i = 0, bodyLength = body.size(); i < bodyLength; i++) {
            final PackedDottedPair goal = (PackedDottedPair) body.get(i);
            switch (goal.getKind()) {
                case NOT:
//                    final IFunctor g1 = (IFunctor);
//                    HtBasePositionalVisitor metaint = new HtBasePositionalVisitor()
//                    chb0((IFunctor) goal.get(i), hb);
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

//    private void chb0 ( PackedDottedPair.Kind kind, IFunctor g1, Map <IFunctor, List <PiCalls>> hb ) {
//        chb(;
//    }

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
