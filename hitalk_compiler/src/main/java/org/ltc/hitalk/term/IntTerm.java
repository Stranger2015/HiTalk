package org.ltc.hitalk.term;

import com.thesett.aima.logic.fol.IntLiteral;
import com.thesett.aima.logic.fol.Term;

/**
 *
 */
public class IntTerm extends IntLiteral implements Term {
    /**
     * Creates a new number with the specified value.
     *
     * @param value The value of the number.
     */
    public IntTerm ( int value ) {
        super(value);
    }

    @Override
    public Term getValue () {
        return null;
    }

    @Override
    public void free () {

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
}
