package org.ltc.hitalk.entities;

/**
 *
 */
public abstract
class PropertyOwner implements IPropertyOwner {
    HtProperty[] props;

    /**
     * @return
     */
    @Override
    public
    int getPropLength () {
        return props.length;
    }

    /**
     * @param props
     */
    public
    PropertyOwner ( HtProperty... props ) {
        this.props = props;
    }
}
