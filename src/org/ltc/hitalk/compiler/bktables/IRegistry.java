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
    R lookup ( R identifiable ) {
        if (!isRegistered(identifiable.getId())) {
            return register(identifiable);
        }
        return identifiable.newInstance();
    }

    /**
     * @param id
     * @return
     */
    R getById ( int id );

    /**
     * @param kind
     * @param pattern
     * @return
     */
    Recordset <R> select ( BkTableKind kind, R pattern );

    Recordset <R> select ( BkTableKind kind );

    void add ( R r );
}