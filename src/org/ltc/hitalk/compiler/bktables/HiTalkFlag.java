package org.ltc.hitalk.compiler.bktables;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.Term;

/**
 *
 */
public
class HiTalkFlag extends Functor {

    /**
     * @param name
     * @param value
     */
    public
    HiTalkFlag ( int name, Term value ) {
        super(name, new Term[]{value});
    }
}
