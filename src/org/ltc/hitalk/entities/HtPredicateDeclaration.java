package org.ltc.hitalk.entities;

/**
 *
 */
class HtPredicateDeclaration implements IPropertyOwner <HtPredicateIndicator> {
    //  private final static int PROPS_LENGTH = 16;
    //   private final HtProperty[] props;


    /**
     *
     */
    HtPredicateDeclaration ( HtProperty... props ) {
        this.props = props;
    }

    @Override
    public
    int getPropLength () {
        return 0;
    }
}