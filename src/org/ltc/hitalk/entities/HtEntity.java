package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.FunctorName;

/**
 *
 */
public
class HtEntity implements IPropertyOwner <FunctorName> {

    /**
     *
     */
    protected final FunctorName name;
    protected final HtEntityKind entityKind;

    /**
     *
     */
    private HtProperty[] properties;

    /**
     * @param name
     * @param entityKind
     */
    protected
    HtEntity ( String name, HtEntityKind entityKind ) {
        this.name = new FunctorName(name, 0);
        this.entityKind = entityKind;
    }

    /**
     *
     */
    public
    HtEntityKind getEntityKind () {
        return entityKind;
    }

    /**
     * @return
     */
    @Override
    public
    int getPropLength () {
        HtEntityKind kind = entityKind;

        for (; ; kind) {

        }

        return 0;
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