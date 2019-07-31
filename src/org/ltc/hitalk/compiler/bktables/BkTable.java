package org.ltc.hitalk.compiler.bktables;

import org.ltc.hitalk.compiler.bktables.db.Record;
import org.ltc.hitalk.compiler.bktables.db.Recordset;

import java.util.ArrayList;
import java.util.List;

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
        return getById(id) != null;
    }

    /**
     * @param identifiable
     * @return
     */
    @Override
    public
    int register ( R identifiable ) {
        add(identifiable);
        return identifiable.getId();
    }

    /**
     * @param id
     * @return
     */
    @Override
    public
    int getById ( int id ) {
        return null;
    }

    @Override
    public
    Recordset <R> select ( BkTableKind kind ) {
        return select(kind, );
    }

    @Override
    public
    void add ( R r ) {
        if (isRegistered(r)) {

        }
    }

    /**
     * @param kind
     * @param r
     * @return
     */
    @Override
    public
    Recordset <R> select ( BkTableKind kind, R r ) {
        BookKeepingTables bkt = new BookKeepingTables();
        List <Record> rs = bkt.getTable(kind);
        List <Record> result = new ArrayList <>();

        for (int i = 0; i < rs.size(); i++) {
            Record record = rs.get(i);
            if (r.equals(record)) {
                result.add(r);
            }

        }//                        HtEntityIdentifier entity1,
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
