package org.ltc.hitalk.wam.compiler.prolog;

import org.ltc.hitalk.core.IHitalkObject;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.IFunctor;

public interface IExecutionTask extends IHitalkObject, IResolver<HtPredicate, HtClause> {
    boolean call(IFunctor goal);
}
