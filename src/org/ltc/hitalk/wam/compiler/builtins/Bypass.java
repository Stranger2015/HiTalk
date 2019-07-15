package org.ltc.hitalk.wam.compiler.builtins;

import com.thesett.aima.logic.fol.Functor;
import org.ltc.hitalk.wam.compiler.HiTalkDefaultBuiltIn;

public
class Bypass extends HiTalkBaseBuiltIn {
    /**
     * Creates the base built in on the specified functor.
     *
     * @param functor        The functor to create a built-in for.
     * @param defaultBuiltIn The default built in, for standard compilation and interners and symbol tables.
     */
    protected
    Bypass ( Functor functor, HiTalkDefaultBuiltIn defaultBuiltIn ) {
        super(functor, defaultBuiltIn);
    }
}
