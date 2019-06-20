package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.FunctorName;
import org.ltc.enumus.Hierarchy;
import org.ltc.hitalk.compiler.bktables.INameable;

import java.util.function.Function;

/**
 *
 */
public
class HtPredicateIndicator extends FunctorName implements INameable <String> {

    private boolean bool;
    private Functor functor;

    /**
     * Creates a functor name with the specified name and arity.
     *
     * @param name  The name of the functor.
     * @param arity The arity of the functor.
     */
    public
    HtPredicateIndicator ( String name, int arity ) {
        super(name, arity);
    }

    /**
     *
     */
    public static
    class HtProperties extends Hierarchy {
        /**
         * @param type
         * @param parentAccessor
         */
        public
        HtProperties ( Class type, Function parentAccessor ) {
            super(type, parentAccessor);
        }
    }
}
