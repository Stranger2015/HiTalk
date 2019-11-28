package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.interpreter.PrologInterpreter;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;

public class HiTalkInterpreter<T extends HtMethod, P, Q> extends PrologInterpreter <T, P, Q> {
    /**
     * @param parser
     * @param compiler
     * @param resolver
     */
    public HiTalkInterpreter ( PlPrologParser parser, ICompiler <T, P, Q> compiler, IResolver <P, Q> resolver ) {
        super(parser, compiler, resolver);
    }
}
