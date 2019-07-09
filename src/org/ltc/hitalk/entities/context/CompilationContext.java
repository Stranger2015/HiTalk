package org.ltc.hitalk.entities.context;

import org.ltc.hitalk.compiler.bktables.HiTalkFlag;
import org.ltc.hitalk.entities.HtEntityIdentifier;

import java.util.ArrayDeque;
import java.util.Deque;

public
class CompilationContext extends Context {
    Deque <?> coinductionStack = new ArrayDeque <>();
    //    CTX, //this context
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
    CompilationContext ( HiTalkFlag... flags ) {
        super(flags);
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
