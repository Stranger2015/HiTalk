package org.ltc.hitalk.entities;

import org.ltc.enumus.Hierarchy;

import java.util.function.Function;

/**
 *
 */
public abstract
class PropertyOwner<T extends Enum <T>> extends Hierarchy <T> implements IPropertyOwner {
    T[] props;

    /**
     * @param type
     * @param parentAccessor
     */
    public
    PropertyOwner ( Class <T> type, Function <T, T> parentAccessor ) {
        super(type, parentAccessor);
    }

    /**
     * @return
     */
    @Override
    public
    int getPropLength () {
        return props.length;
    }

//    /**
//     * @param props
//     */
//    public
//    PropertyOwner ( HiTalkFlag... props ) {
//        this.props = props;
//    }
//
//    public abstract
//    String get ( Loading basename );
}
