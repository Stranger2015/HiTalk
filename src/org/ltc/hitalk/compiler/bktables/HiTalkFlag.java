package org.ltc.hitalk.compiler.bktables;

import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.term.ListTerm;

/**
 *
 */
public
class HiTalkFlag extends HtProperty {

    /**
     * @param name
     * @param value
     */
    public
    HiTalkFlag ( int name, Term value ) {
        super(name, (ListTerm) value);//fixme
    }
}
