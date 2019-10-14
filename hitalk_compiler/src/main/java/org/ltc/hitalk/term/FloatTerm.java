package org.ltc.hitalk.term;

import com.thesett.aima.logic.fol.FloatLiteral;
import com.thesett.aima.logic.fol.Term;

public class FloatTerm extends FloatLiteral implements Term {
    /**
     * Creates a new number with the specified value.
     *
     * @param value The value of the number.
     */
    public FloatTerm ( float value ) {
        super(value);
    }

    @Override
    public Term getValue () {
        return null;
    }

    @Override
    public void free () {

    }
}
