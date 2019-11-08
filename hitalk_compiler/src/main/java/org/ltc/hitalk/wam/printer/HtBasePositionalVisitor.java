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
package org.ltc.hitalk.wam.printer;


import com.thesett.aima.logic.fol.*;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.PackedDottedPair;
import org.ltc.hitalk.wam.compiler.HtFunctor;
import org.ltc.hitalk.wam.compiler.HtPositionalTermTraverserImpl;

import static org.ltc.hitalk.term.PackedDottedPair.Kind;

/**
 * HtBasePositionalVisitor is an {@link HtAllTermsVisitor} that is being driven by a {@link HtPositionalTermTraverser}.
 * It is used as a base class for implementing visitors that need to understand the positional context during visitation.
 * <p>
 * <p/>It uses positional context information from a {@link HtPositionalTermTraverser}, to determine whether terms are
 * being entered or left, and splits these down into calls on appropriate enter/leave methods. Default no-op
 * implementations of these methods are supplied by this base class and may be extended by specific implementations as
 * needed to figure out the positional context during visitation.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Provide default enter/leave methods for every part of a term.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public
class HtBasePositionalVisitor implements HtAllTermsVisitor {

//    List <PiCall> piCalls = new ArrayList <>();

    /**
     * The name interner.
     */
    protected VariableAndFunctorInterner interner;

    /**
     * The symbol table.
     */
    protected SymbolTable <Integer, String, Object> symbolTable;

    /**
     * The positional context.
     */
    protected HtPositionalTermTraverser traverser;

    /**
     * Creates a positional visitor.
     *
     * @param interner    The name interner.
     * @param symbolTable The compiler symbol table.
     */
    public HtBasePositionalVisitor ( SymbolTable <Integer, String, Object> symbolTable,
                                     VariableAndFunctorInterner interner ) {

        this.symbolTable = symbolTable;
        this.interner = interner;
        this.traverser = new HtPositionalTermTraverserImpl();
    }

    /**
     * {@inheritDoc}
     */
    public void visit ( Term term ) {
        if (traverser.isEnteringContext()) {
            enterTerm(term);
        } else if (traverser.isLeavingContext()) {
            leaveTerm(term);
            term.setTermTraverser(null);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void visit ( HtFunctor functor ) {
        if (traverser.isEnteringContext()) {
            enterFunctor(functor);
        } else if (traverser.isLeavingContext()) {
            leaveFunctor(functor);
            functor.setTermTraverser(null);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void visit ( Variable variable ) {
        if (traverser.isEnteringContext()) {
            enterVariable(variable);
        } else if (traverser.isLeavingContext()) {
            leaveVariable(variable);
            variable.setTermTraverser(null);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void visit ( HtPredicate predicate ) {
        if (traverser.isEnteringContext()) {
            enterPredicate(predicate);
        } else if (traverser.isLeavingContext()) {
            leavePredicate(predicate);
            predicate.setTermTraverser(null);
        }
    }

    protected void leavePredicate ( HtPredicate predicate ) {

    }

    /**
     * {@inheritDoc}
     */
    public void visit ( HtClause clause ) {
        if (traverser.isEnteringContext()) {
            enterClause(clause);
        } else if (traverser.isLeavingContext()) {
            leaveClause(clause);
            clause.setTermTraverser(null);
        }
    }


    /**
     * @param dottedPair
     */
    @Override
    public void visit ( PackedDottedPair dottedPair ) {
        if (traverser.isEnteringContext()) {
            enterDottedPair(dottedPair);
        } else if (traverser.isLeavingContext()) {
            leaveDottedPair(dottedPair);
            dottedPair.setTermTraverser(null);
        }
    }


    /**
     * @param dottedPair
     */
    protected void enterDottedPair ( PackedDottedPair dottedPair ) {
        final Kind kind = dottedPair.getKind();
        switch (kind) {
            case NIL:
                break;
            case TRUE:
                break;
            case LIST:
                break;
            case BYPASS:
                break;
            case NOT:
            case AND:
            case OR:
                break;
            case GOAL:
                break;
            case INLINE_GOAL:
                break;
            case OTHER:
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + kind);
        }
    }

    protected void leaveDottedPair ( PackedDottedPair dottedPair ) {


    }

    /**
     * {@inheritDoc}
     */
    public void visit ( IntegerType literal ) {
        if (traverser.isEnteringContext()) {
            enterIntLiteral(literal);
        } else if (traverser.isLeavingContext()) {
            leaveIntLiteral(literal);
            literal.setTermTraverser(null);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void visit ( LiteralType literal ) {
        if (traverser.isEnteringContext()) {
            enterLiteral(literal);
        } else if (traverser.isLeavingContext()) {
            leaveLiteral(literal);
            literal.setTermTraverser(null);
        }
    }

    /**
     * Called when a term is entered during the visitation.
     *
     * @param term The term being entered.
     */
    protected void enterTerm ( Term term ) {
    }

    /**
     * Called when a term is being left during the visitation.
     *
     * @param term The term being left.
     */
    protected void leaveTerm ( Term term ) {
    }

    /**
     * Called when a functor is entered during the visitation.
     *
     * @param functor The functor being entered.
     */
    protected void enterFunctor ( HtFunctor functor ) {

    }

    /**
     * Called when a functor is being left during the visitation.
     *
     * @param functor The functor being left.
     */
    protected void leaveFunctor ( HtFunctor functor ) {
    }

    /**
     * Called when a variable is entered during the visitation.
     *
     * @param variable The variable being entered.
     */
    protected void enterVariable ( Variable variable ) {
    }

    /**
     * Called when a variable is being left during the visitation.
     *
     * @param variable The variable being left.
     */
    protected void leaveVariable ( Variable variable ) {

    }

    /**
     * Called when a predicate is entered during the visitation.
     *
     * @param predicate The predicate being entered.
     */
    protected void enterPredicate ( HtPredicate predicate ) {

    }

//    private Collection <? extends PiCall> collectPiCalls ( HtPredicateDefinition <ISubroutine, HtPredicate, HtClause> pd ) {
//        piCalls = new ArrayList <>();
//        for (int i = 0; i < pd.size(); i++) {
//            ISubroutine sub = pd.get(i);
//            if (pd.isBuiltIn()) {
//                continue;
//            }
//            HtFunctor head = sub.getHead();
//            HtFunctor[] body = sub.getBody();
//            int name = -1;
//            Term[] args = EMPTY_TERM_ARRAY;
//            piCalls.add(new PiCall(name, args));
//        }
//
//        return null;
//    }

//====================================================

    /**
     * Called when a clause is entered during the visitation.
     *
     * @param clause The clause being entered.
     */
    protected void enterClause ( HtClause clause ) {
    }

    /**
     * Called when a clause is being left during the visitation.
     *
     * @param clause The clause being left.
     */
    protected void leaveClause ( HtClause clause ) {
    }

    /**
     * Called when a integer literal is entered during the visitation.
     *
     * @param literal The integer literal being entered.
     */
    protected void enterIntLiteral ( IntegerType literal ) {
    }

    /**
     * Called when a integer literal is being left during the visitation.
     *
     * @param literal The integer literal being left.
     */
    protected void leaveIntLiteral ( IntegerType literal ) {
    }

    /**
     * Called when a literal is entered during the visitation.
     *
     * @param literal The literal being entered.
     */
    protected void enterLiteral ( LiteralType literal ) {
    }

    /**
     * Called when a literal is being left during the visitation.
     *
     * @param literal The literal being left.
     */
    protected void leaveLiteral ( LiteralType literal ) {
    }

}
