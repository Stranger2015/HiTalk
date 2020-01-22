package org.ltc.hitalk.wam.compiler.builtins;

import org.ltc.hitalk.wam.compiler.HtFunctor;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.ltc.hitalk.wam.compiler.prolog.IPrologBuiltIn;

/**
 *
 */
public abstract
class PrologBuiltInFunctor extends HtFunctor implements IPrologBuiltIn {
    /**
     * Holds the functor that this is a built in for.
     */
    protected IFunctor functor;

    /**
     * Creates a built-in for the specified functor.
     *
     * @param functor The functor to create a built-in for.
     */
    public PrologBuiltInFunctor ( IFunctor functor ) {
        super(functor.getName(), functor.getArguments());
        this.functor = functor;
    }

    /**
     * Provides the functor that this is a built-in for.
     *
     * @return The functor that this is a built-in for.
     */
    public IFunctor getFunctor () {
        return functor;
    }

    /**
     * {@inheritDoc}
     */
    public void setString(String key) {
        functor.setString(key);
    }

    /**
     * {@inheritDoc}
     */
    public String getString() {
        return functor.getString();
    }
}

