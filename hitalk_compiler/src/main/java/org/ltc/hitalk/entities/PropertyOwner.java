package org.ltc.hitalk.entities;

/**
 *
 */
public abstract
class PropertyOwner<T extends IProperty> implements IPropertyOwner <I> {
    private T[] props;

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
    @SafeVarargs
    public
    PropertyOwner ( T... props ) {
        this.props = props;
    }
}
