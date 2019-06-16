package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.entities.HtEntity;

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
    HtEntity sender;
    HtEntity this_;
    HtEntity self;
    HtEntity entity;
    List <ICallable>

            metaCallContext, coinductionStack
}
