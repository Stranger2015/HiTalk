package org.ltc.hitalk.compiler.bktables;

import org.ltc.hitalk.compiler.bktables.db.Record;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @param <R>
 */
public
class BookKeepingTables<R extends Record, T extends BkTable <R>> extends LinkedList <T>/* IRegistry <T>*/ {

    private final static int TAB_LENGTH = BkTableKind.USER_DEFINED_FLAGS.ordinal() + 1;

    /**
     * @param kind
     * @return
     */
    public
    BkTable <R> getTable ( BkTableKind kind ) {
        int index = kind.ordinal();
        if (index >= tables.size()) {
            BkTable <R> t = new BkTable <>(this);
            tables.add(t);

            return t;
        }

        return tables.get(kind.ordinal());
    }

    /**
     *
     */
    private final List <BkTable <R>> tables;
//    private final BiConsumer <Functor, R>[] actions = new BiConsumer[TAB_LENGTH];

//    private IRegistry <R> registry = new BkTable <>(this);

    /**
     *
     */
    public
    BookKeepingTables () {
        tables = new ArrayList <>();
    }

//    /**
//     * @param id
//     * @return
//     */
////    @Override
//    public
//    boolean isRegistered ( int id ) {
//        return registry.isRegistered(id);
//    }

//    /**
//     * @param identifiable
//     * @return
//     */
////    @Override
//    public
//    int register ( T identifiable ) {
//        return 0;
//    }

    /**
     * @param identifiable
     * @return
     */
//    @Override
    public
    int lookup ( R identifiable ) {
        return 0;
    }

    //
    //    @Override
    public
    List <R> select ( BkTableKind kind ) {
        return getTable(kind).select(kind, null);
    }

//    /**
//     * @param rs
//     * @return
//     */
//    @Override
//    public
//    boolean add ( T rs ) {
//        return false;
//    }

    /**
     * @param tableKind
     * @param rec
     * @return
     */
//    @Override
    public
    R selectOne ( BkTableKind tableKind, R rec ) {
        return getTable(tableKind).selectOne(tableKind, rec);
    }

    /**
     * @param rs
     */
//    @Override
    public
    void save ( BkTableKind tableKind, R rs ) {
        getTable(tableKind).save(rs);
    }
}
