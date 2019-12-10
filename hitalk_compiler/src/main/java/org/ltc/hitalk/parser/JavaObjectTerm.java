package org.ltc.hitalk.parser;

import org.ltc.hitalk.term.HtBaseTerm;

public class JavaObjectTerm extends HtBaseTerm {
    public JavaObjectTerm () {
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
