package org.ltc.hitalk.compiler.bktables;

import org.ltc.hitalk.compiler.bktables.db.Record;
import org.ltc.hitalk.compiler.bktables.db.Recordset;
import org.ltc.hitalk.entities.HtEntityIdentifier;

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
    R register ( R identifiable );

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
     * @param eid
     * @param obj
     * @return
     */
    Recordset <R> select ( BkTableKind kind, HtEntityIdentifier eid, Object... obj );
}