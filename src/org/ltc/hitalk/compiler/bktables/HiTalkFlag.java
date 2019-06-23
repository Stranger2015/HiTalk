package org.ltc.hitalk.compiler.bktables;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.Term;

/**
 *
 */
public
class HiTalkFlag extends Functor {
    /**
     * @param ffn
     * @param ffv
     */
    public
    HiTalkFlag ( int ffn, int ffv ) {
        super(ffn, new Term[]{new Functor(ffv, new Term[0])});
    }
}
