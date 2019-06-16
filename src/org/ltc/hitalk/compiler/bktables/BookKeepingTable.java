package org.ltc.hitalk.compiler.bktables;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @param <T>
 */
public abstract
class BookKeepingTable<T> {

    protected final Set <T> table = new HashSet <>();
    protected final Iterator <T> current;

    public
    BookKeepingTable () {
        current = table.iterator();
    }

    enum BkTables {
        BEFORE_EVENTS,//<HtEvent>
        AFTER_EVENTS,
        LOADED_ENTITIES,
        ENTITY_RELATIONS,
        LOADED_FILES,
        RUNTIME_FLAGS,
        STATIC_BINDING_CACHES,
        DYNAMIC_BINDING_LOOKUP_CACHES_1,
        DYNAMIC_BINDING_LOOKUP_CACHES_2,
        LIBRARY_PATHS,
        EXTENSION_POINTS_FOR_LOGTALK_MAKE,
        GOAL_TERM_EXPANSION_DEFAULT_HOOKS,
        ENGINES,
        COUNTERS,
        DEBUG_HOOK_PREDS,
        INTERNAL_INIT_FLAGS,
        USER_DEFINED_FLAGS,
        ;
    }
}


