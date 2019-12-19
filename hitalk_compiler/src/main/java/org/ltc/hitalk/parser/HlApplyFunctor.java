package org.ltc.hitalk.parser;

import org.ltc.hitalk.wam.compiler.HtFunctor;

/**
 *
 */
public class HlApplyFunctor extends HtFunctor {
    /**
     * @param hilogApply
     * @param arity
     * @param arityDelta
     */
    public HlApplyFunctor ( int hilogApply, int arity, int arityDelta ) {
        super(hilogApply, arity, arityDelta);
    }
}
