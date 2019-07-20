package org.ltc.hitalk.compiler.bktables;


import com.thesett.aima.logic.fol.Functor;
import org.ltc.hitalk.compiler.bktables.db.Record;
import org.ltc.hitalk.compiler.bktables.db.Recordset;
import org.ltc.hitalk.entities.HtEntityIdentifier;

import java.util.function.BiConsumer;

/**
 * @param <R>
 */
public
class BookKeepingTables<R extends Record>
        implements IRegistry <R> {

    private final static int TAB_LENGTH = BkTableKind.USER_DEFINED_FLAGS.ordinal() + 1;
    /**
     *
     */
    private final Record[][] tables = new Record[TAB_LENGTH][];
    private final BiConsumer <Functor, R>[] actions = new BiConsumer[TAB_LENGTH];

    private IRegistry <R> registry = new BkTable <>();

    /**
     *
     */
    public
    BookKeepingTables () {

    }

    /**
     * @param kind
     * @return
     */
    public
    BiConsumer <Functor, R> getAction ( BkTableKind kind ) {
        return actions[kind.ordinal()];
    }

    /**
     * @param id
     * @return
     */
    @Override
    public
    boolean isRegistered ( int id ) {
        return registry.isRegistered(id);
    }

    /**
     * @param identifiable
     * @return
     */
    @Override
    public
    R register ( R identifiable ) {
        return registry.register(identifiable);
    }

    /**
     * @param id
     * @return
     */
    @Override
    public
    R getById ( int id ) {
        return registry.getById(id);
    }

    /**
     * @param kind
     * @param eid
     * @param obj
     * @return
     */
    @Override
    public
    Recordset <R> select ( BkTableKind kind, HtEntityIdentifier eid, Object... obj ) {
        return registry.select(kind, eid, obj);
    }
}


