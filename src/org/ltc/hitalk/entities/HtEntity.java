package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.Functor;
import org.ltc.hitalk.compiler.bktables.INameable;

/**
 *
 */
public
class HtEntity extends PropertyOwner implements INameable <Functor> {

    /**
     *
     */
    protected final HtEntityIdentifier identifier;

    /**
     *
     * @param kind
     */
    protected
    HtEntity ( Functor functor, HtEntityKind kind, HtProperty... props ) {
        this.identifier = new HtEntityIdentifier(functor, kind);
    }

    /**
     * @return
     */
    @Override
    public
    Functor getName () {
        return identifier;
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