package org.ltc.hitalk.entities.context;


import org.ltc.hitalk.entities.HtProperty;

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
@Deprecated
public
class ExecutionContext extends Context {
//    HtEntityIdentifier sender;
//    HtEntityIdentifier this_;
//    HtEntityIdentifier self;
//    HtEntityIdentifier entity;
//    List <ICallable>
//
//            metaCallContext, coinductionStack;


    public
    ExecutionContext ( HtProperty... props ) {
        super(props);
    }

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
