package org.ltc.hitalk.term;

import com.thesett.aima.search.Operator;
import org.ltc.hitalk.NumberTerm;
import org.ltc.hitalk.compiler.IVafInterner;

import java.util.List;

/**
 *
 */
public class IntTerm extends NumberTerm implements ITerm {
    /**
     * Creates a new number with the specified value.
     *
     * @param value The value of the number.
     */
    public IntTerm(int value) {
        super(value);
    }

    /**
     *
     */
    @Override
    public void free() {

    }

    @Override
    public void accept(ITermVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public List<ITerm> acceptTransformer(ITermTransformer transformer) {
        return transformer.transform(this);
    }

    public String toString(IVafInterner interner, boolean printVarName, boolean printBindings) {
        return toString();
    }

    public boolean structuralEquals(ITerm term) {
        return false;
    }

    @Override
    public boolean isNumber () {
        return true;
    }

    @Override
    public boolean isConstant () {
        return true;
    }

    @Override
    public boolean isGround () {
        return true;
    }

    /**
     * @return
     */
    public boolean isJavaObject () {
        return false;
    }

    public ITerm getChildStateForOperator ( Operator <ITerm> op ) {
        return null;
    }//fixme
}
