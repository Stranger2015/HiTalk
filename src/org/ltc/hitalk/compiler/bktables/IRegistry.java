package org.ltc.hitalk.compiler.bktables;

import org.ltc.hitalk.compiler.bktables.db.Record;

import java.util.List;

/**
 *
 */
public
interface IRegistry<R extends Record> {
    /**
     * @param id
     * @return
     */
    boolean isRegistered ( int id );

    /**
     * @param identifiable
     * @return
     */
    int register ( R identifiable );

    /**
     * @param identifiable
     * @return
     */
    default
    int lookup ( R identifiable ) {
        if (!isRegistered(identifiable.getId())) {
            return register(identifiable);
        }
        return getById(identifiable.getId());
    }

    /**
     * @param id
     * @return
     */
    int getById ( int id );

    /**
     * @param kind
     * @param pattern
     * @return
     */
    List <R> select ( BkTableKind kind, R pattern );

    /**
     * @param kind
     * @return
     */
    List <R> select ( BkTableKind kind );

    /**
     * @param r
     */
    void add ( R r );

    /**
     * @param tableKind
     * @param r
     * @return
     */
    default
    R selectOne ( BkTableKind tableKind, R r ) {
        return select(tableKind, r).get(0);
    }

    /**
     * @param r
     */
    void save ( R r );

    /**
     * @param list
     */
    default
    void save ( List <R> list ) {
        for (R r : list) {
            save(r);
        }
    }

}