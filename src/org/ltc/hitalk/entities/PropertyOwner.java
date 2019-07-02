package org.ltc.hitalk.entities;

import org.ltc.hitalk.compiler.bktables.HiTalkFlag;

import static org.ltc.hitalk.entities.context.Context.Kind.Loading;

/**
 *
 */
public abstract
class PropertyOwner implements IPropertyOwner {
    HiTalkFlag[] props;

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
    PropertyOwner ( HiTalkFlag... props ) {
        this.props = props;
    }

    public abstract
    String get ( Loading basename );
}
