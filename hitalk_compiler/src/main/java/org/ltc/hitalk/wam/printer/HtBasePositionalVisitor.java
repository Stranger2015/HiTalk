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


import com.thesett.aima.logic.fol.IntegerType;
import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.LiteralType;
import org.ltc.hitalk.compiler.AbstractBaseMachine;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.compiler.HtPositionalTermTraverser;
import org.ltc.hitalk.wam.compiler.IFunctor;

import static org.ltc.hitalk.term.ListTerm.Kind;

/**
 * HtBasePositionalVisitor is an {@link IAllTermsVisitor} that is being driven by a {@link HtPositionalTermTraverser}.
 * It is used as a base class for implementing visitors that need to understand the positional context during visitation.
 * <p>
 * <p/>It uses positional context information from a {@link IPositionalTermTraverser}, to determine whether terms are
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
public abstract
class HtBasePositionalVisitor extends AbstractBaseMachine implements IAllTermsVisitor {

    /**
     * The positional context.
     */
    protected IPositionalTermTraverser traverser;

    /**
     * Creates a positional visitor.
     *
     * @param interner    The name interner.
     * @param symbolTable The compiler symbol table.
     */
    public HtBasePositionalVisitor ( ISymbolTable <Integer, String, Object> symbolTable,
                                     IVafInterner interner,
                                     IPositionalTermTraverser traverser ) {
        super(symbolTable, interner);
        this.traverser = traverser;
    }

    /**
     * {@inheritDoc}
     */
    public void visit ( ITerm term ) {
        if (traverser.isEnteringContext()) {
            enterTerm(term);
        } else if (traverser.isLeavingContext()) {
            leaveTerm(term);
            term.setTermTraverser(null);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param functor
     */
    public void visit(IFunctor functor) throws Exception {
        if (traverser.isEnteringContext()) {
            enterFunctor(functor);
        } else if (traverser.isLeavingContext()) {
            leaveFunctor(functor);
            functor.setTermTraverser(null);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    public void visit ( HtVariable variable ) {
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
     * @return
     */
    public void visit ( HtClause clause ) throws LinkageException {
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
    public void visit ( ListTerm dottedPair ) throws LinkageException {
        if (traverser.isEnteringContext()) {
            enterListTerm(dottedPair);
        } else if (traverser.isLeavingContext()) {
            leaveListTerm(dottedPair);
            dottedPair.setTermTraverser(null);
        }
    }

    /**
     * @param dottedPair
     */
    protected void enterListTerm ( ListTerm dottedPair ) throws LinkageException {
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

    protected void leaveListTerm ( ListTerm dottedPair ) {
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
    protected void enterTerm ( ITerm term ) {
    }

    /**
     * Called when a term is being left during the visitation.
     *
     * @param term The term being left.
     */
    protected void leaveTerm ( ITerm term ) {
    }

    /**
     * Called when a functor is entered during the visitation.
     *
     * @param functor The functor being entered.
     */
    protected void enterFunctor(IFunctor functor) throws Exception {

    }

    /**
     * Called when a functor is being left during the visitation.
     *
     * @param functor The functor being left.
     */
    protected void leaveFunctor ( IFunctor functor ) {
    }

    /**
     * Called when a variable is entered during the visitation.
     *
     * @param variable The variable being entered.
     */
    protected void enterVariable ( HtVariable variable ) {
    }

    /**
     * Called when a variable is being left during the visitation.
     *
     * @param variable The variable being left.
     */
    protected void leaveVariable ( HtVariable variable ) {

    }

    /**
     * Called when a predicate is entered during the visitation.
     *
     * @param predicate The predicate being entered.
     */
    protected void enterPredicate ( HtPredicate predicate ) {

    }

//====================================================

    /**
     * Called when a clause is entered during the visitation.
     *
     * @param clause The clause being entered.
     */
    protected void enterClause ( HtClause clause ) throws LinkageException {
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
