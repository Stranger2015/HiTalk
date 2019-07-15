package org.ltc.hitalk.compiler.bktables;

import com.thesett.aima.logic.fol.Functor;
import org.ltc.hitalk.entities.HtEntityIdentifier;
import org.ltc.hitalk.entities.HtEntityKind;
import org.ltc.hitalk.entities.HtRelation;
import org.ltc.hitalk.entities.HtRelationKind;

import java.util.function.BiConsumer;

/**
 * @param <T>
 */
public
class BookKeepingTables<T extends INameable <Functor>> implements IRegistry {

    private final static int TAB_LENGTH = BkTableKind.USER_DEFINED_FLAGS.ordinal() + 1;
    private final byte[][] tables = new byte[TAB_LENGTH][];
    private final BiConsumer <Functor, T>[] actions = new BiConsumer[TAB_LENGTH];

    private IRegistry registry;

    /**
     *
     */
    public
    BookKeepingTables () {

    }

    /**
     * @return
     */
    public
    byte[][] getTables () {
        return tables;
    }

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

    /**
     *
     */
    HtRelation[] selectRelations ( BkTableKind idx,
                                   HtEntityIdentifier entity1,
                                   HtEntityIdentifier entity2,
                                   HtEntityKind entityKind,
                                   HtRelationKind relationKind ) {
        byte[] table = tables[idx.ordinal()];
        int n1 = entity1.getName();
        int n2 = entity2.getName();
        idx.getBkClass();

        return null;

    }
}


