package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.interpreter.PrologInterpreter;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;

/**
 * @param <T>
 * @param <P>
 * @param <Q>
 */
public class HiTalkInterpreter<T extends HtMethod, P, Q> extends PrologInterpreter <T, P, Q> {
    /**
     * @param parser
     * @param compiler
     * @param resolver
     */
    public HiTalkInterpreter ( PlPrologParser parser, ICompiler <T, P, Q> compiler, IResolver <P, Q> resolver ) {
        super(parser, compiler, resolver);
    }

    /**
     * @return
     */
    public String getMeta () {
        return meta;
    }

    public final static String meta = "solve(Ctx,(G1,G2)):-\n" +
            "    !, solve(Ctx,G1),\n" +
            "       solve(Ctx,G2).\n" +

            "solve(Ctx,U>>G):- !,\n" +
            "    solve([U|Ctx],G).\n" +

            "solve(Ctx,G):-\n" +
            "    member(U,Ctx),\n" +
            "    method(U,G,Body),\n" +
            "    solve(Ctx,Body).\n" +

            "solve(_, G):-\n" +
            "    predicate_property(G, built_in),\n" +
            "    call(G).\n" +
            "method(U, Head, Body) :-\n" +
//            "%    current_entity(U),\n" +
            "    clause( U::Head, Body ).";
}
