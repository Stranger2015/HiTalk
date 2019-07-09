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

    //    private final Map <Functor,T>[] tables = new HashMap[TAB_LENGTH];
    private final byte[][] tables = new byte[TAB_LENGTH][];
    private final BiConsumer <Functor, T>[] actions = new BiConsumer[TAB_LENGTH];

    private IRegistry registry;

    enum BkTableKind {
        BEFORE_EVENTS,//<HtEvent>
        AFTER_EVENTS,

        LOADED_ENTITIES(DbSchema.class),
        ENTITY_RELATIONS(BkRelations.class),
        ENTITY_PROPERTIES,
        PREDICATE_PROPERTIES,

        LOADED_FILES,

        RUNTIME_FLAGS,
        STATIC_BINDING_CACHES,
        DYNAMIC_BINDING_LOOKUP_CACHES_1,
        //dynamic binding lookup caches for messages and super calls
        SEND_TO_OBJ(DYNAMIC_BINDING_LOOKUP_CACHES_1),
        SEND_TO_OBJ_NE(DYNAMIC_BINDING_LOOKUP_CACHES_1),
        SEND_TO_SELF(DYNAMIC_BINDING_LOOKUP_CACHES_1),
        OBJ_SUPERCALL(DYNAMIC_BINDING_LOOKUP_CACHES_1),
        CTG_SUPERCALL(DYNAMIC_BINDING_LOOKUP_CACHES_1),
        //
//        //Send_to_obj_(Obj, Pred, ExCtx)
//        :- dynamic(Send_to_obj_'/3).
//        //Send_to_obj_ne_(Obj, Pred, ExCtx)
//        :- dynamic(Send_to_obj_ne_'/3).
//        //Send_toSelf_(Obj, Pred, ExCtx)
//        :- dynamic(Send_toSelf_'/3).
//        //objSuperCall_(Super, Pred, ExCtx)
//        :- dynamic(objSuperCall_'/3).
//        //CtgSuperCall_(Ctg, Pred, ExCtx)
//        :- dynamic(CtgSuperCall_'/3).
//
        DYNAMIC_BINDING_LOOKUP_CACHES_2,

        LIBRARY_PATHS,
        EXTENSION_POINTS_FOR_LOGTALK_MAKE,
        GOAL_TERM_EXPANSION_DEFAULT_HOOKS,
        TERM_EXPANSION_DEFAULT_HOOKS(GOAL_TERM_EXPANSION_DEFAULT_HOOKS),
        GOAL_EXPANSION_DEFAULT_HOOKS(GOAL_TERM_EXPANSION_DEFAULT_HOOKS),

        ENGINES,
        COUNTERS,
        DEBUG_HOOK_PREDS,
        INTERNAL_INIT_FLAGS,
        USER_DEFINED_FLAGS,
        ;

        private final BkTableKind parent;
        private Class <? extends DbSchema> clazz = DbSchema.class;
        private Class <? extends DbSchema> bkClass;
//        private final int dimension;
//        private final int[] sizes;

        /**
         *
         */
        private
        BkTableKind () {
            this(null, DbSchema.class);
        }

//        /**
//         * @param parent
//         * @param dimension
//         */
//        private
//        BkTableKind ( BkTableKind parent ) {
//
//            this.parent = parent;
//        }

//        BkTableKind ( BkTableKind parent, int... size ) {
//            this(parent, 1, size);
//
//        }

//        BkTableKind ( int dimension, int... size ) {
//            this(null, dimension, size);
//        }

        BkTableKind ( BkTableKind parent, Class <? extends DbSchema> clazz ) {
            this.parent = parent;
            this.clazz = clazz;
        }

        /**
         * @return
         */
        public
        BkTableKind getParent () {
            return parent;
        }

        public
        Class <? extends DbSchema> getBkClass () {
            return bkClass;
        }

        public
        void setBkClass ( Class <? extends DbSchema> bkClass ) {
            this.bkClass = bkClass;
        }

//        /**
//         * @return
//         */
//        public
//        int getDimension () {
//            return dimension;
//        }
//
//        public
//        int[] getSizes () {
//            return sizes;
//        }
    }

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

        return

    }
}


