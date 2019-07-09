package org.ltc.hitalk.wam.interpreter;

import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.interpreter.ResolutionEngine;
import org.ltc.hitalk.wam.compiler.HiTalkCompilerApp;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledQuery;

/**
 *
 */
public
class HiTalk extends TopLevel {

//    public final Deque <Resolver <Term, Queue>> topLevelStack = new ArrayDeque <>();

    /**
     *
     */
    public
    HiTalk ( ResolutionEngine engine, HiTalkInterpreter <Term, HiTalkWAMCompiledQuery> interpreter, HiTalkCompilerApp compiler ) {
        super(engine, interpreter, compiler);
    }
}
