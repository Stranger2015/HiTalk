package org.ltc.hitalk.compiler.bktables;

import org.ltc.hitalk.compiler.bktables.db.Recordset;
import org.ltc.hitalk.entities.HtEntityIdentifier;

/**
 *
 */
public
class BkTable implements IRegistry {
    /**
     * @param clazz
     * @return
     */
    @Override
    public
    boolean isRegistered ( Class <? extends IIdentifiable> clazz ) {
        return false;
    }

    /**
     * @param iIdentifiable
     * @return
     */
    @Override
    public
    IIdentifiable register ( IIdentifiable iIdentifiable ) {
        return null;
    }

    /**
     * @param id
     * @return
     */
    @Override
    public
    IIdentifiable getById ( int id ) {
        return null;
    }

    public
    Recordset select ( BkTableKind kind, HtEntityIdentifier eid1, Object... ue3cxt ) {
//        DbSchema schema =
        int idx = kind.  // selectRelations ( BkTableKind idx,
//                        HtEntityIdentifier entity1,
//                        HtEntityIdentifier entity2,
//                        HtEntityKind entityKind,
//                        HtRelationKind relationKind ) {
//            DbSchema[] table = tables[idx.ordinal()];
//            int n1 = entity1.getName();
//            int n2 = entity2.getName();
//            idx.getBkClass();

        return null;
    }
}
