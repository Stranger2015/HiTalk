package org.ltc.hitalk.entities.context;


import org.ltc.hitalk.compiler.bktables.HiTalkFlag;

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
class ExecutionContext extends Context {

    public
    ExecutionContext ( HiTalkFlag... props ) {
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

    /**
     * @return
     */
    @Override
    public
    HiTalkFlag[] getFlags () {
        return new HiTalkFlag[0];
    }

    @Override
    public
    String get ( Kind.Loading basename ) {
        return null;
    }
}
