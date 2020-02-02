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

package org.ltc.hitalk.term;

import com.thesett.aima.logic.fol.TermTraverser;
import com.thesett.aima.search.GoalState;
import com.thesett.aima.search.Operator;
import com.thesett.aima.search.TraversableState;
import com.thesett.aima.search.util.backtracking.Reversable;
import com.thesett.common.util.doublemaps.SymbolKey;
import org.ltc.hitalk.parser.ISourceCodePosition;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * BaseTerm provides an abstract base implementation of {@link ITerm}. In particular it provides methods to make an
 * abstract syntax tree expressible as a state space with operations to navigate over it.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Express navigation over the term syntax tree as operators.
 * <tr><td> Provide default search goal that matches all nodes in the tree.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public abstract class HtBaseTerm extends TraversableState <ITerm> implements ITerm, GoalState, Cloneable {

    /* private static final Logger log = Logger.getLogger(BaseTerm.class.getName()); */

    /**
     * Used to hold the terms allocation cell reference, can be useful during compilation. This is initialized to -1 to
     * indicate an unassigned status.
     */
    protected int alloc = -1;

    /**
     * Holds the source code position that this term was parsed from.
     */
    protected ISourceCodePosition sourcePosition;

    /**
     * Holds the bracketing flag for this term.
     */
    protected boolean bracketed;

    /**
     * Holds a traverser to supply search operators over terms. When <tt>null</tt> this is used as the operator.
     */
    protected TermTraverser traverser;

    /**
     * Holds this terms unique symbol key.
     */
    protected SymbolKey symbolKey;

    /**
     * Holds a reversible operator to establish and restore state when traversing this term.
     */
    private Reversable reversible;

    /**
     * {@inheritDoc}
     */
    public boolean isGoal () {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public ITerm getChildStateForOperator ( Operator <ITerm> op ) {
        return op.getOp();
    }

    /**
     * {@inheritDoc}
     */
    public float costOf ( Operator op ) {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public void setReversible ( Reversable reversible ) {
        this.reversible = reversible;
    }

    /**
     * {@inheritDoc} If a {@link Reversable} has been set on this term it is applied, otherwise nothing is done.
     */
    public void applyOperator () {
        if (reversible != null) {
            reversible.applyOperator();
        }
    }

    /**
     * {@inheritDoc} If a {@link Reversable} has been set on this term it is undone, otherwise nothing is done.
     */
    public void undoOperator () {
        if (reversible != null) {
            reversible.undoOperator();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setTermTraverser ( TermTraverser traverser ) {
        this.traverser = traverser;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator <Operator <ITerm>> validOperators ( boolean reverse ) {
        return getChildren(reverse);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator <Operator <ITerm>> getChildren ( boolean reverse ) {
        // Return an empty iterator by default.
        return Collections.emptyIterator();
    }

    /**
     * {@inheritDoc}
     */
    public ITerm getOp () {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public SymbolKey getSymbolKey() {
        return symbolKey;
    }

    /**
     * {@inheritDoc}
     */
    public int getAllocation () {
        return alloc;
    }

    /**
     * {@inheritDoc}
     */
    public void setAllocation ( int alloc ) {
        this.alloc = alloc;
    }

    /**
     * {@inheritDoc}
     */
    public ISourceCodePosition getSourceCodePosition () {
        return sourcePosition;
    }

    /**
     * {@inheritDoc}
     */
    public void setSourceCodePosition ( ISourceCodePosition sourceCodePosition ) {
        this.sourcePosition = sourceCodePosition;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isBracketed () {
        return bracketed;
    }

    /**
     * {@inheritDoc}
     */
    public void setBracketed ( boolean bracketed ) {
        this.bracketed = bracketed;
    }

    /**
     * {@inheritDoc}
     */
    public ITerm queryConversion () {
        try {
            return (ITerm) clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Got CloneNotSupportedException but clone should be implemented on Terms.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void accept ( ITermVisitor visitor ) {
        visitor.visit(this);
    }

    /**
     * {@inheritDoc}
     */
    public List <ITerm> acceptTransformer ( ITermTransformer transformer ) {
        return transformer.transform(this);
    }

//    /**
//     * {@inheritDoc}
//     */
//    public String toString ( IVafInterner interner, boolean printVarName, boolean printBindings ) {
//        return toString();
//    }

    @Override
    public boolean isQuery () {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean structuralEquals ( ITerm term ) {
        return this.equals(term);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNumber () {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isFunctor () {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isVar () {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isConstant () {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCompound () {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAtom () {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isGround () {
        return false;
    }

    /**
     * @return
     */
    public boolean isJavaObject () {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public String toString() {
        return "";//oString(getAppContext().getInterner(), true, true);
    }

    @Override
    public void setSymbolKey(SymbolKey key) {

    }
}
