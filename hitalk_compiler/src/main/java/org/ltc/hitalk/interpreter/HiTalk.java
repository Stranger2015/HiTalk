package org.ltc.hitalk.interpreter;

import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.parser.HtClause;

/**
 *
 */
public
class HiTalk<T extends HtClause, P, Q> extends TopLevel <T, P, Q> {

//    public final Deque <Resolver <Term, Queue>> topLevelStack = new ArrayDeque <>();

    /**
     *
     */
    public HiTalk ( HtResolutionEngine engine,
                    PrologInterpreter <P, Q> interpreter, ICompiler <T, P, Q> compiler ) {

        super(engine, interpreter, compiler);
    }
}
