package org.ltc.hitalk.entities.context;

import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.entities.PropertyOwner;

/**
 *
 */
public
class Context extends PropertyOwner {

    /**
     *
     */
    public
    enum Kind {
        /**
         * CompCtx(Ctx, Head, _, Entity, Sender, This, Self, Prefix, MetaVars, MetaCallCtx, ExCtx, _, Stack, Lines),
         * =========================
         * <p>
         * basename,
         * directory,
         * entity_identifier,
         * entity_prefix,
         * entity_type,
         * file,
         * flags,
         * source,
         * stream,
         * target,
         * term,
         * term_position,
         * variable_names
         */
        COMPILATION,
        /**
         * executionContext(ExCtx, user, user, user, HookEntity, [], []),
         * ==========================================================
         * 1.coinduction_stack,
         * 2.context_id,        /user
         * 3.entity,            /user
         * 4.metacall_context,  /user
         * 5.self,              /hook_entity
         * 6.sender,
         * 7.this_;
         */
        EXECUTION,
        /**
         * entity_identifier
         * entity_prefix
         * entity_type
         * source
         * file
         * basename
         * directory
         * stream
         * target
         * flags
         * term
         * term_position
         * variable_names
         */
        LOADING,
        ;
    }

    /**
     * @param props
     */
    public
    Context ( HtProperty... props ) {
        super(props);
    }


}
