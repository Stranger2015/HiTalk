package org.ltc.hitalk.compiler.bktables;


import org.ltc.hitalk.compiler.bktables.db.Record;
import org.ltc.hitalk.compiler.bktables.db.Recordset;

import java.util.ArrayList;
import java.util.List;

/**
 * @param <R>
 */
public
class BookKeepingTables<R extends Record>
        implements IRegistry <R> {

    private final static int TAB_LENGTH = BkTableKind.USER_DEFINED_FLAGS.ordinal() + 1;

    /**
     * @param kind
     * @return
     */
    public
    List <Record> getTable ( BkTableKind kind ) {
        return tables.get(kind.ordinal());
    }

    /**
     *
     */
    private final List <List <Record>> tables = new ArrayList <>();
//    private final BiConsumer <Functor, R>[] actions = new BiConsumer[TAB_LENGTH];

    private IRegistry <R> registry = new BkTable <>();

    /**
     *
     */
    public
    BookKeepingTables () {

    }
//
//    /**
//     * @param kind
//     * @return
//     */
//    public
//    BiConsumer <Functor, R> getAction ( BkTableKind kind ) {
//        return actions[kind.ordinal()];
//    }

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
    int register ( R identifiable ) {
        return registry.register(identifiable);
    }

    /**
     * @param id
     * @return
     */
    @Override
    public
    int getById ( int id ) {
        return registry.getById(id);
    }

    /**
     * @param kind
     * @param pattern
     * @return
     */
    @Override
    public
    Recordset <R> select ( BkTableKind kind, R pattern ) {
        return registry.select(kind, pattern);
    }

    @Override
    public
    Recordset <R> select ( BkTableKind kind ) {
        return registry.select(kind);
    }

    @Override
    public
    void add ( R r ) {
        registry.add(r);
    }
}


