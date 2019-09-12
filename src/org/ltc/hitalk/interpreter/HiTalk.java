package org.ltc.hitalk.interpreter;

import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.parser.HtClause;

/**
 *
 */
public
class HiTalk<T,Q> extends TopLevel<T,Q> {

//    public final Deque <Resolver <Term, Queue>> topLevelStack = new ArrayDeque <>();

    /**
     *
     */
    public
    HiTalk ( HtResolutionEngine<T,Q> engine,
             HiTalkInterpreter <Term, Q> interpreter,
             ICompiler<HtClause,T,Q> compiler ) {
        super(engine, interpreter, compiler);
    }
}
