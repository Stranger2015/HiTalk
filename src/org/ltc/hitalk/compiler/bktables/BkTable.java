package org.ltc.hitalk.compiler.bktables;

import org.ltc.hitalk.compiler.bktables.db.Record;
import org.ltc.hitalk.compiler.bktables.db.Recordset;
import org.ltc.hitalk.entities.HtEntityIdentifier;

/**
 *
 */
public
class BkTable<R extends Record> implements IRegistry <R> {
    /**
     * @param id
     * @return
     */
    public
    boolean isRegistered ( int id ) {
        return false;
    }

    /**
     * @param iIdentifiable
     * @return
     */
    @Override
    public
    R register ( R iIdentifiable ) {
        return null;
    }

    /**
     * @param id
     * @return
     */
    @Override
    public
    R getById ( int id ) {
        return null;
    }

    public
    Recordset <R> select ( BkTableKind kind, HtEntityIdentifier eid1, Object... obj ) {
//        Record schema =
        int idx = kind.ordinal(); // selectRelations ( BkTableKind idx,
        BookKeepingTables bkt = new BookKeepingTables();
        bkt.get();
//                        HtEntityIdentifier entity1,
//                        HtEntityIdentifier entity2,
//                        HtEntityKind entityKind,
//                        HtRelationKind relationKind ) {
//            Record[] table = tables[idx.ordinal()];
//            int n1 = entity1.getName();
//            int n2 = entity2.getName();
//            idx.getBkClass();

        return null;
    }
}
