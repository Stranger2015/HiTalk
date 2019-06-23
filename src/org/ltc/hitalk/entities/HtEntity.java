package org.ltc.hitalk.entities;

import org.ltc.hitalk.compiler.bktables.INameable;

/**
 *
 */
public
class HtEntity extends PropertyOwner implements INameable <HtEntityIdentifier> {

    /**
     *
     */
    protected final HtEntityIdentifier identifier;

    /**
     *
     * @param identifier
     */
    protected
    HtEntity ( HtEntityIdentifier identifier ) {
        this.identifier = identifier;
    }


//    /**
//     * @return
//     */
//    @Override
//    public
//    int getPropLength () {
//        HtEntityKind kind = entityKind;
//
//        for (int length = kind.getPropsLength(); kind != null; kind = kind.getParent()) {
//
//        }
//
//        return 0;
//    }

    /**
     * @return
     */
    @Override
    public
    HtEntityIdentifier getName () {
        return identifier;
    }

//    /**
//     *
//     */
//    private
//    void initProperties () {
//        switch (entityKind) {
//            case ENTITY:
//
//                break;
//            case OBJECT:
//                //                getProperty();
//                break;
//            case CATEGORY:
//
//                break;
//            case OBJECT_OR_CATEGORY:
//                break;
//            case PROTOCOL:
//
//                break;
//            case MODULE:
//
//                break;
//
//            default:
//                throw new IllegalStateException("Unexpected value: " + entityKind);
//        }
}