package org.ltc.hitalk.parser.jp.segfault.prolog.parser;

import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.TermTransformer;
import com.thesett.aima.logic.fol.TermTraverser;
import com.thesett.aima.logic.fol.TermVisitor;
import com.thesett.aima.search.Operator;
import com.thesett.aima.search.Successor;
import com.thesett.aima.search.Traversable;
import com.thesett.aima.search.util.backtracking.Reversable;
import com.thesett.aima.state.ComponentType;
import com.thesett.common.parsing.SourceCodePosition;
import com.thesett.common.util.doublemaps.SymbolKey;
import com.thesett.common.util.logic.UnaryPredicate;

import java.util.Iterator;

public class ErrorTerm implements Term {
    @Override
    public boolean isNumber () {
        return false;
    }

    @Override
    public boolean isFunctor () {
        return false;
    }

    @Override
    public boolean isVar () {
        return false;
    }

    @Override
    public boolean isConstant () {
        return false;
    }

    @Override
    public boolean isCompound () {
        return false;
    }

    @Override
    public boolean isAtom () {
        return false;
    }

    @Override
    public boolean isGround () {
        return false;
    }

    @Override
    public Term getValue () {
        return null;
    }

    @Override
    public int getAllocation () {
        return 0;
    }

    @Override
    public void setAllocation ( int alloc ) {

    }

    @Override
    public void setSymbolKey ( SymbolKey key ) {

    }

    @Override
    public SymbolKey getSymbolKey () {
        return null;
    }

    @Override
    public void free () {

    }

    @Override
    public Term queryConversion () {
        return null;
    }

    @Override
    public void setReversable ( Reversable reversable ) {

    }

    @Override
    public void setTermTraverser ( TermTraverser traverser ) {

    }

    @Override
    public Iterator <Operator <Term>> getChildren ( boolean reverse ) {
        return null;
    }

    @Override
    public SourceCodePosition getSourceCodePosition () {
        return null;
    }

    @Override
    public void setSourceCodePosition ( SourceCodePosition sourceCodePosition ) {

    }

    @Override
    public boolean isBracketed () {
        return false;
    }

    @Override
    public void setBracketed ( boolean bracketed ) {

    }

    @Override
    public void accept ( TermVisitor visitor ) {

    }

    @Override
    public Term acceptTransformer ( TermTransformer transformer ) {
        return null;
    }

    @Override
    public String toString ( IVafInterner interner, boolean printVarName, boolean printBindings ) {
        return getClass().getSimpleName();
    }

    @Override
    public boolean structuralEquals ( Term term ) {
        return false;
    }

    @Override
    public Term getOp () {
        return null;
    }

    @Override
    public Traversable <Term> getChildStateForOperator ( Operator <Term> op ) {
        return null;
    }

    @Override
    public float costOf ( Operator op ) {
        return 0;
    }

    @Override
    public Iterator <Operator <Term>> validOperators ( boolean reverse ) {
        return null;
    }

    @Override
    public Iterator <Successor <Term>> successors ( boolean reverse ) {
        return null;
    }

    @Override
    public UnaryPredicate getDefaultGoalPredicate () {
        return null;
    }

    @Override
    public void applyOperator () {

    }

    @Override
    public void undoOperator () {

    }

    @Override
    public Object getProperty ( String property ) {
        return null;
    }

    @Override
    public void setProperty ( String name, Object value ) {

    }

    @Override
    public boolean hasProperty ( String property ) {
        return false;
    }

    @Override
    public ComponentType getComponentType () {
        return null;
    }
}
