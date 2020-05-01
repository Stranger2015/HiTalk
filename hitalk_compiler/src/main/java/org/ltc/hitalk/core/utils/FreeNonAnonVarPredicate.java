package org.ltc.hitalk.core.utils;

import com.thesett.common.util.logic.UnaryPredicate;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ITerm;

public class FreeNonAnonVarPredicate<T extends ITerm<T>> implements UnaryPredicate<T> {

    /**
     * Evaluates a logical predicate.
     *
     * @param term The object to test for predicate membership.
     * @return <tt>true</tt> if the object is a member of the predicate, <tt>false</tt> otherwise.
     */
    public boolean evaluate(T term) {
        if (term instanceof HtVariable) {
            HtVariable<?> var = (HtVariable<?>) term;
            return !var.isBound() && !var.isAnonymous();
        }
        return false;
    }
}
