package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.Functor;
import org.ltc.hitalk.compiler.bktables.HiTalkFlag;
import org.ltc.hitalk.compiler.bktables.INameable;
import org.ltc.hitalk.entities.context.Context;

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
    HtEntity ( Functor functor, HtEntityKind kind, HiTalkFlag... props ) {
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

    @Override
    public
    String get ( Context.Kind.Loading basename ) {
        return null;
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