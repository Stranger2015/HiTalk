package org.ltc.hitalk.wam.printer;

import com.thesett.aima.logic.fol.AllTermsVisitor;
import com.thesett.aima.logic.fol.LinkageException;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.IntegerLiteral;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.compiler.IFunctor;

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

/**
 * DelegatingAllTermsVisitor is an {@link AllTermsVisitor} that accepts an optional delegate. All visit operations by
 * default defer onto the delegate.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Visit a term.
 * <tr><td> Visit a clause.
 * <tr><td> Visit a functor.
 * <tr><td> Visit a variable.
 * <tr><td> Visit a literal.
 * <tr><td> Visit an integer literal.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public
class HtDelegatingAllTermsVisitor implements IAllTermsVisitor {
    /**
     * The option Teal delegate.
     */
    protected IAllTermsVisitor delegate;

    /**
     * Creates an AllTermsVisitor that by default delegates all visit operations to the specified delegate.
     *
     * @param delegate The delegate, may be <tt>null</tt> if none is to be used.
     */
    public HtDelegatingAllTermsVisitor ( IAllTermsVisitor delegate ) {
        this.delegate = delegate;
    }

    /**
     * {@inheritDoc}
     */
    public void visit ( HtVariable variable ) {
        if (delegate != null) {
            //variable.accept(delegate);
            delegate.visit(variable);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void visit ( IntegerLiteral literal ) {
        if (delegate != null) {
            //literal.accept(delegate);
            delegate.visit(literal);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void visit ( HtLiteralType literal ) {
        if (delegate != null) {
            //literal.accept(delegate);
            delegate.visit(literal);
        }
    }

    /**
     * Visits a clause.
     *
     * @param clause The clause to visit.
     */
    @Override
    public void visit ( HtClause clause ) throws LinkageException {
        if (delegate != null) {
            //literal.accept(delegate);
            delegate.visit(clause);
        }
    }

    /**
     * Visits a predicate.
     *
     * @param predicate The predicate to visit.
     */
    @Override
    public void visit ( HtPredicate predicate ) {
        if (delegate != null) {
            //literal.accept(delegate);
            delegate.visit(predicate);
        }
    }

    @Override
    public void visit ( IFunctor functor ) throws LinkageException {
        if (delegate != null) {
            //literal.accept(delegate);
            delegate.visit(functor);
        }
    }

    @Override
    public void visit ( ListTerm listTerm ) throws LinkageException {
        if (delegate != null) {
            //literal.accept(delegate);
            delegate.visit(listTerm);
        }
    }

    @Override
    public void visit ( ITerm term ) {
        if (delegate != null) {
            //literal.accept(delegate);
            delegate.visit(term);
        }
    }
}
