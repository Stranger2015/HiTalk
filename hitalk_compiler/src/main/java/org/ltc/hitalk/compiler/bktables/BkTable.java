package org.ltc.hitalk.compiler.bktables;

import org.ltc.hitalk.compiler.bktables.db.Record;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public
class BkTable<R extends Record> extends ArrayList <R> {//implements IRegistry <R>  {
    private final BookKeepingTables bkt;

    public
    BkTable ( BookKeepingTables bkt ) {
        this.bkt = bkt;
    }

    /**
     * @param id
     * @return
     */
    public
    boolean isRegistered ( int id ) {
        return false; //getById(id) != -1;
    }

//    /**
//     * @param identifiable
//     * @return
//     */
////    @Override
//    public
//    int register ( R identifiable ) {
//        add(identifiable);
//        return identifiable.getId();
//    }

//    /**
//     * @param id
//     * @return
//     */
//    @Override
//    public
//    R getByIdf ( int id ) {
//        return get(id);
//    }

    /**
     * @param r
     * @return
     */
    @Override
    public
    boolean add ( R r ) {
        if (!contains(r.getId())) {//todo
            //register(r);
        }
        return false;
    }

    //    @Override
    public
    void save ( R r ) {

    }

    /**
     * @param kind
     * @param r
     * @return
     */
//    @Override
    public
    List <R> select ( BkTableKind kind, R r ) {
//        BookKeepingTables bkt = new BookKeepingTables();
//        List <R> rs = this.select(kind);
        List <R> list = new ArrayList <>();

        for (R record : this) {
            if (r == null || r.equals(record)) {
                list.add(record);
            }
        }

        return list;
    }

    public
    BookKeepingTables getBkt () {
        return bkt;
    }

    public
    R selectOne ( BkTableKind tableKind, R rec ) {
        return null;
    }
}
