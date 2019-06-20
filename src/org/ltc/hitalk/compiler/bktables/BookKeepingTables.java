package org.ltc.hitalk.compiler.bktables;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @param <T>
 */
public
class BookKeepingTables<NT extends CharSequence, T extends INameable <NT>> {

    private final static int TAB_LENGTH = BkTableKind.USER_DEFINED_FLAGS.ordinal() + 1;
    private final Map <NT, T>[] tables = new HashMap[TAB_LENGTH];
    private final BiConsumer <NT, T>[] actions = new BiConsumer[TAB_LENGTH];

    /**
     * @return
     */
    public
    Map <NT, T>[] getTables () {
        return tables;
    }

    /**
     *
     */
    public
    BookKeepingTable () {
        current = table.iterator();
    }

    /**
     * @param tableIdx
     * @return
     */
    public
    BiConsumer <NT, T> getAction ( BkTableKind tableIdx ) {

    }


    /**
     *
     */
    public
    enum BkTableKind {
        BEFORE_EVENTS,//<HtEvent>
        AFTER_EVENTS,

        LOADED_ENTITIES,
        ENTITY_RELATIONS,
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

        //BookKeepingTables<NT, INameable<T>> tables = new BookKeepingTables<>();
        private final BkTableKind parent;

        private final
        BiConsumer <>

        /**
         *
         */
        private
        BkTableKind () {
            parent = null;
        }

        /**
         * @param parent
         */
        private
        BkTableKind ( BkTableKind parent ) {

            this.parent = parent;
        }

    }
}


