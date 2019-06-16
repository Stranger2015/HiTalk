package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Predicate;
import com.thesett.aima.logic.fol.Term;

public
class HtPredicateProperty extends HtProperty {

    Predicate <?> predicate;

    public
    HtPredicateProperty ( HiTalkType type, String name, Term value ) {
        super(type, name, value);
    }
}
