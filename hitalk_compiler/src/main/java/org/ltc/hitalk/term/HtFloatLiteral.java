package org.ltc.hitalk.term;

public abstract class HtFloatLiteral extends IntTerm {
    /**
     * Creates a new number with the specified value.
     *
     * @param value The value of the number.
     */
    public HtFloatLiteral ( float value ) {
        super(Float.floatToIntBits(value));
    }

    public boolean isHiLog () {
        return false;
    }

    /**
     * Frees all assigned variables in the term, leaving them unnassigned.
     */
    public void free () {

    }
}
