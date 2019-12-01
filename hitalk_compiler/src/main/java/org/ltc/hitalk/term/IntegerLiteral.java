package org.ltc.hitalk.term;


import com.thesett.aima.logic.fol.NumericType;
import org.ltc.hitalk.compiler.IVafInterner;

/**
 *
 */
public class IntegerLiteral extends HtBaseTerm {

    /**
     * Holds the value of the number.
     */
    protected final int value;

    /**
     * Creates a new number with the specified value.
     *
     * @param value The value of the number.
     */
    public IntegerLiteral ( int value ) {
        this.value = value;
    }

    /**
     * Determines whether a number is of basic type int.
     *
     * @return <tt>true</tt> if a number is an int, <tt>false</tt> otherwise.
     */
    public boolean isInt () {
        return true;
    }

    /**
     * Gets the value of the number converted to an int.
     *
     * @return The value of the number as int.
     */
    public int intValue () {
        return value;
    }

    /**
     * Gets the value of the number converted to a long.
     *
     * @return The value of the number as long.
     */
    public long longValue () {
        return value;
    }

    /**
     * Gets the value of the number converted to a float.
     *
     * @return The value of the number as float.
     */
    public float floatValue () {
        return (float) value;
    }

    /**
     * Gets the value of the number converted to a double.
     *
     * @return The value of the number as double.
     */
    public double doubleValue () {
        return value;
    }

    /**
     * Determines if this number is equal to another.
     *
     * @param comparator The object to compare to.
     * @return <tt>true</tt> if the comparator is a number equal in value to this one, <tt>false</tt> otherwise.
     */
    public boolean equals ( Object comparator ) {
        if (this == comparator) {
            return true;
        }

        if ((comparator == null) || !(comparator instanceof NumericType)) {
            return false;
        }

        NumericType comparatorNumber = (NumericType) comparator;

        return value == comparatorNumber.intValue();
    }

    /**
     * Computes a hash code based on the value of this number.
     *
     * @return A hash code based on the value of this number.
     */
    public int hashCode () {
        return value;
    }

    /**
     * Pretty prints the value of this number, mostly for debugging purposes.
     *
     * @return The value of this number as a string.
     */
    public String toString () {
        return "IntLiteral: [ value = " + value + "]";
    }

    public boolean isHiLog () {
        return false;
    }

    public void free () {

    }

    /**
     * {@inheritDoc}
     */
    public String toString ( IVafInterner interner, boolean printVarName, boolean printBindings ) {
        return Integer.toString(value);
    }
}

