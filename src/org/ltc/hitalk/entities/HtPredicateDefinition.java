package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.Functor;

import java.util.function.Consumer;

/**
 *
 */
public
class HtPredicateDefinition extends PropertyOwner <HtProperty> {

//    private final static int PROPS_LENGTH = 7;

    /**
     * @param props
     */
    public
    HtPredicateDefinition ( Consumer <Functor> consumer, HtProperty... props ) {
        super(props);
    }
}
