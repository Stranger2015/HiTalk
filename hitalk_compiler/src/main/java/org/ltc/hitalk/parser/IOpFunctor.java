package org.ltc.hitalk.parser;

import org.ltc.hitalk.wam.compiler.IFunctor;

public interface IOpFunctor extends IFunctor {
    int getPriority();
}
