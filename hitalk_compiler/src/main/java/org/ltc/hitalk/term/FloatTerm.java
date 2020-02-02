package org.ltc.hitalk.term;

import com.thesett.aima.search.util.backtracking.Reversable;
import org.ltc.hitalk.NumberTerm;
import org.ltc.hitalk.compiler.IVafInterner;

import java.util.List;

/**
 *
 */
public class FloatTerm extends NumberTerm implements ITerm {

    /**
     * Creates a new number with the specified value.
     *
     * @param fvalue The value of the number.
     */
    public FloatTerm(float fvalue) {
        super(Float.floatToIntBits(fvalue));
    }

    /**
     * @return
     */
    public boolean isList () {
        return false;
    }

    @Override
    public boolean isHiLog () {
        return false;
    }

    @Override
    public FloatTerm getValue () {
        return this;
    }

    @Override
    public void free () {

    }

    /**
     * Allows a reversable operator to be set upon the term, so that context can be established or cleared as terms are
     * traversed.
     *
     * @param reversible The reversable operator to use on the term.
     */
    public void setReversible ( Reversable reversible ) {

    }

    /**
     * Accepts a term visitor.
     *
     * @param visitor The term visitor to accept.
     */
//    public void accept ( ITermVisitor visitor ) {
//
//    }

    /**
     * Applies a term to term transformation function over the term tree, recursively from this point downwards. This is
     * a general recursive mapping function over term trees, and is intended to be usefull for transforming term trees
     * for compilation, optimization or other transformational activities.
     *
     * @param transformer The transformation function to apply.
     * @return The transformed term tree.
     */
    @Override
    public List<ITerm> acceptTransformer(ITermTransformer transformer) {
        return null;
    }

    /**
     * Pretty prints a term relative to the symbol namings provided by the specified interner.
     *
     * @param interner      The interner use to provide symbol names.
     * @param printVarName  <tt>true</tt> if the names of bound variables should be printed, <tt>false</tt> if just the
     *                      binding without the variable name should be printed.
     * @param printBindings <tt>true</tt> if variable binding values should be printed, <tt>false</tt> if just the
     *                      variables name without any binding should be printed.
     * @return A pretty printed string containing the term.
     */
    public String toString ( IVafInterner interner, boolean printVarName, boolean printBindings ) {
        return null;
    }

    /**
     * Compares this term for structural equality with another. Two terms are structurally equal if they are the same
     * functor with the same arguments, or are the same unbound variable, or the bound values of the left or right
     * variable operands are structurally equal. Structural equality is a stronger equality than unification and unlike
     * unification it does not produce any variable bindings. Two unified terms will always be structurally equal.
     *
     * @param term The term to compare with this one for structural equality.
     * @return <tt>true</tt> if the two terms are structurally eqaul, <tt>false</tt> otherwise.
     */
    public boolean structuralEquals ( ITerm term ) {
        return false;
    }

}
