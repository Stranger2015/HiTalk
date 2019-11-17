package org.ltc.hitalk.parser.jp.segfault.prolog.parser;

import com.thesett.aima.logic.fol.TermTraverser;
import com.thesett.aima.search.Operator;
import com.thesett.aima.search.util.backtracking.Reversable;
import com.thesett.common.parsing.SourceCodePosition;
import com.thesett.common.util.doublemaps.SymbolKey;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.term.HtBaseTerm;
import org.ltc.hitalk.term.ITerm;

import java.util.Iterator;

public class ErrorTerm extends HtBaseTerm implements ITerm {
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
    public ITerm getValue () {
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
    public ITerm queryConversion () {
        return null;
    }

    @Override
    public void setReversible ( Reversable reversible ) {

    }

    @Override
    public void setTermTraverser ( TermTraverser traverser ) {

    }

    @Override
    public Iterator <Operator <ITerm>> getChildren ( boolean reverse ) {
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
    public String toString ( IVafInterner interner, boolean printVarName, boolean printBindings ) {
        return getClass().getSimpleName();//todo
    }
}