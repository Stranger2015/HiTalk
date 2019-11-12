package org.ltc.hitalk.term;

import com.thesett.aima.logic.fol.FloatLiteral;

public class FloatTerm extends FloatLiteral implements ITerm {
    /**
     * Creates a new number with the specified value.
     *
     * @param value The value of the number.
     */
    public FloatTerm ( float value ) {
        super(value);
    }

    @Override
    public ITerm getValue () {
        return null;
    }

    @Override
    public void free () {

    }
}
