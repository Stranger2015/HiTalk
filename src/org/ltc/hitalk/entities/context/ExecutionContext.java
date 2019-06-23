package org.ltc.hitalk.entities.context;


import org.ltc.hitalk.entities.HtEntityIdentifier;
import org.ltc.hitalk.wam.compiler.ICallable;

import java.util.List;

/**
 * execution_context(
 * ExecutionContext,
 * Entity,
 * Sender,
 * This,
 * Self,
 * MetaCallContext,
 * CoinductionStack
 * )
 * <p>
 * execution_context(
 * ?nonvar,
 * ?entity_identifier,
 * ?object_identifier,
 * ?object_identifier,
 * ?object_identifier,
 *
 * @list(callable),
 * @list(callable) )
 * - zero_or_one
 */
public
class ExecutionContext {
    HtEntityIdentifier sender;
    HtEntityIdentifier this_;
    HtEntityIdentifier self;
    HtEntityIdentifier entity;
    List <ICallable>

            metaCallContext, coinductionStack;

    public
    IMetrics getCurrentMetrics () {
        return null;
    }

    public
    IMetrics getMaxMetrics () {

        IMetrics maxMetrics = null;
        return maxMetrics;
    }
}
