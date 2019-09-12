package org.ltc.hitalk.compiler.bktables;

import org.ltc.hitalk.compiler.bktables.db.Record;

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
        return getById(id) != -1;
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
        return -1;
    }

    @Override
    public
    List <R> select ( BkTableKind kind ) {
        return select(kind, null);
    }

    /**
     * @param r
     */
    @Override
    public
    void add ( R r ) {
        if (!isRegistered(r.getId())) {
            register(r);
        }
    }

    @Override
    public
    void save ( R r ) {

    }

    /**
     * @param kind
     * @param r
     * @return
     */
    @Override
    public
    List <R> select ( BkTableKind kind, R r ) {
        BookKeepingTables bkt = new BookKeepingTables();
        List <R> rs = this.select(kind);
        List <R> list = new ArrayList <>();

        for (R record : rs) {
            if (r == null || r.equals(record)) {
                list.add(record);
            }
        }

        return list;
    }
}
