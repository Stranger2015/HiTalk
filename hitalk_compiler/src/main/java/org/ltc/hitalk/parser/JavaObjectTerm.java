package org.ltc.hitalk.parser;

import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.term.HtBaseTerm;

/**
 *
 */
public class JavaObjectTerm extends HtBaseTerm {
    /**
     *
     */
    public JavaObjectTerm () {
    }

    /**
     * @return
     */
    @Override
    public boolean isHiLog () {
        return false;
    }

    /**
     * @return
     */
    public boolean isJavaObject () {
        return true;
    }

    /**
     * Frees all assigned variables in the term, leaving them unnassigned.
     */
    public void free () {

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
        return toString();
    }
}
