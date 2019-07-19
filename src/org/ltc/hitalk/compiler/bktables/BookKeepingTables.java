package org.ltc.hitalk.compiler.bktables;


import com.thesett.aima.logic.fol.Functor;
import org.ltc.hitalk.compiler.bktables.db.DbSchema;

import java.util.function.BiConsumer;

/**
 * @param <T>
 */
public
class BookKeepingTables<T extends INameable <Functor>>
        extends BkTable
        implements IRegistry {

    private final static int TAB_LENGTH = BkTableKind.USER_DEFINED_FLAGS.ordinal() + 1;
    private final DbSchema[][] tables = new DbSchema[TAB_LENGTH][];
    private final BiConsumer <Functor, T>[] actions = new BiConsumer[TAB_LENGTH];

    private IRegistry registry = this;

    /**
     *
     */
    public
    BookKeepingTables () {

    }

//    /**
//     * @return
//     */
//    public
//    DbSchema[][] getTables () {
//        return tables;
//    }

    /**
     * @param identifier
     * @return
     */
    public
    BiConsumer <Functor, T> getAction ( BkTableKind identifier ) {
        return actions[identifier.ordinal()];
    }


    /**
     * @param clazz
     * @return
     */
    @Override
    public
    boolean isRegistered ( Class <? extends IIdentifiable> clazz ) {
        return false;
    }

    /**
     * @param iIdentifiable
     * @return
     */
    @Override
    public
    IIdentifiable register ( IIdentifiable iIdentifiable ) {
        return null;
    }

    /**
     * @param id
     * @return
     */
    @Override
    public
    IIdentifiable getById ( int id ) {
        return registry.getById(id);
    }
}


