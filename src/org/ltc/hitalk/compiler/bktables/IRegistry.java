package org.ltc.hitalk.compiler.bktables;

import org.ltc.hitalk.compiler.bktables.db.Record;
import org.ltc.hitalk.compiler.bktables.db.Recordset;

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
    Recordset <R> select ( BkTableKind kind, R pattern );

    /**
     * @param kind
     * @return
     */
    Recordset <R> select ( BkTableKind kind );

    /**
     * @param r
     */
    void add ( R r );

    R selectOne ( BkTableKind loadedEntities, R bkLoadedEntities );
}