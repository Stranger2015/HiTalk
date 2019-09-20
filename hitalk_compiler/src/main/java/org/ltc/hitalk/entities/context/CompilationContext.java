package org.ltc.hitalk.entities.context;

import org.ltc.hitalk.entities.HtEntityIdentifier;
import org.ltc.hitalk.entities.HtProperty;

import java.util.ArrayDeque;
import java.util.Deque;

public
class CompilationContext extends Context {
    Deque <?> coinductionStack = new ArrayDeque <>();
    LoadContext loadContext;
    ExecutionContext executionContext;
    HtEntityIdentifier entityIdentifier;
    String entityPrefix;
    //    FOO;
//    LINES;
    /**
     *
     */
    Deque <?> metacallContext = new ArrayDeque <>();
//    METAVARS;
//    RUNTIME;

    HtEntityIdentifier sender;
    HtEntityIdentifier this_;
    HtEntityIdentifier entity;

    public
    CompilationContext ( HtProperty... props ) {
        super(props);
    }

    public
    String get ( Kind.Loading basename ) {
        return null;
    }
}
