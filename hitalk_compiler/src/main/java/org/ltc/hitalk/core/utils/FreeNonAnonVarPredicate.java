package org.ltc.hitalk.core.utils;

import com.thesett.common.util.logic.UnaryPredicate;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ITerm;

public class FreeNonAnonVarPredicate implements UnaryPredicate <ITerm> {

    /**
     * Determine whether a term is a free variable.
     *
     * @param term The object to test for predicate membership.
     * @return <tt>true</tt> if the term is a free variable, <tt>false</tt> otherwise.
     */
    public boolean evaluate ( ITerm term ) {
        if (term.isVar() && (term instanceof HtVariable)) {
            HtVariable var = (HtVariable) term;

            return !var.isBound() && !var.isAnonymous();
        }

        return false;
    }
}
