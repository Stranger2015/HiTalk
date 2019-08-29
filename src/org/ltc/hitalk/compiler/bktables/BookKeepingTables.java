package org.ltc.hitalk.compiler.bktables;

import org.ltc.hitalk.compiler.bktables.db.Record;

import java.util.ArrayList;
import java.util.List;

/**
 * @param <R>
 */
public
class BookKeepingTables<R extends Record> implements IRegistry <R> {

    private final static int TAB_LENGTH = BkTableKind.USER_DEFINED_FLAGS.ordinal() + 1;

    /**
     * @param kind
     * @return
     */
    public
    BkTable <R> getTable ( BkTableKind kind ) {
        return tables.get(kind.ordinal());
    }

    /**
     *
     */
    private final List <BkTable <R>> tables;
//    private final BiConsumer <Functor, R>[] actions = new BiConsumer[TAB_LENGTH];

    private IRegistry <R> registry = new BkTable <R>();

    /**
     *
     */
    public
    BookKeepingTables () {
        tables = new ArrayList <>();
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
    List <R> select ( BkTableKind kind, R pattern ) {
        return getTable(kind).select(kind, pattern);
    }

    @Override
    public
    List <R> select ( BkTableKind kind ) {
        return getTable(kind).select(kind);
    }

    @Override
    public
    void add ( BkTableKind kind, R r ) {
        getTable(kind).add(kind, r);
    }

    @Override
    public
    void save ( R r ) {

    }
}


