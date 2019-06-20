package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.FunctorName;

/**
 *
 */
public
class HtPredicateDefinition implements IPropertyOwner <FunctorName> {

    private final static int PROPS_LENGTH = 7;
    private final HtProperty[] props;

    /**
     * @param props
     */
    public
    HtPredicateDefinition ( HtProperty[] props ) {
        this.props = props;
    }

    /**
     * @return
     */
    @Override
    public
    int getPropLength () {
        return PROPS_LENGTH;
    }
}
