package org.ltc.hitalk.entities.context;

import org.ltc.hitalk.compiler.bktables.Flag;
import org.ltc.hitalk.entities.HtEntityIdentifier;

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
    Deque <?> metacallContext = new ArrayDeque <>();
//    METAVARS;
//    RUNTIME;

    HtEntityIdentifier sender;
    HtEntityIdentifier this_;
    HtEntityIdentifier entity;

    public
    CompilationContext ( Flag... flags ) {
        super(flags);
    }

    /**
     * @return
     */
    @Override
    public
    Flag[] getFlags () {
        return new Flag[0];
    }

    public
    String get ( Kind.Loading basename ) {
        return null;
    }
}
