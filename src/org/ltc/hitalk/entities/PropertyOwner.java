package org.ltc.hitalk.entities;

/**
 *
 */
public abstract
class PropertyOwner<T extends Enum <T>>/* extends Hierarchy <T> */ implements IPropertyOwner {
    T[] props;

//    /**
//     * @param type
//     * @param parentAccessor
//     */
//    public
//    PropertyOwner ( Class <T> type, Function <T, T> parentAccessor ) {
//        super(type, parentAccessor);
//    }

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
//
//    public abstract
//    String get ( Loading basename );
}
