package org.ltc.hitalk.wam.compiler.builtins;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.common.util.doublemaps.SymbolKey;
import org.ltc.hitalk.wam.compiler.HiTalkBuiltIn;

public abstract
class HiTalkBuiltInFunctor extends Functor implements HiTalkBuiltIn {
    /**
     * Holds the functor that this is a built in for.
     */
    protected Functor functor;

    /**
     * Creates a built-in for the specified functor.
     *
     * @param functor The functor to create a built-in for.
     */
    public
    HiTalkBuiltInFunctor ( Functor functor ) {
        super(functor.getName(), functor.getArguments());
        this.functor = functor;
    }

    /**
     * Provides the functor that this is a built-in for.
     *
     * @return The functor that this is a built-in for.
     */
    public
    Functor getFunctor () {
        return functor;
    }

    /**
     * {@inheritDoc}
     */
    public
    void setSymbolKey ( SymbolKey key ) {
        functor.setSymbolKey(key);
    }

    /**
     * {@inheritDoc}
     */
    public
    SymbolKey getSymbolKey () {
        return functor.getSymbolKey();
    }
}

