package org.ltc.hitalk.wam.compiler.hitalk;

import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.wam.compiler.HtMethod;
import org.ltc.hitalk.wam.compiler.prolog.PrologInterpreter;
import org.ltc.hitalk.wam.task.PreCompilerTask;

/**
 * @param <T>
 * @param <P>
 * @param <Q>
 */
public class HiTalkInterpreter<T extends HtMethod, P, Q,
        PC extends HiTalkWAMCompiledPredicate,
        QC extends HiTalkWAMCompiledQuery>

        extends PrologInterpreter<T, P, Q, PC, QC> {

    /**
     * @param parser
     * @param compiler
     * @param resolver
     */
    public HiTalkInterpreter(ISymbolTable<Integer, String, Object> symbolTable,
                             ITermFactory termFactory,
                             ICompiler<T, P, Q, PC, QC> compiler,
                             IResolver<PC, QC> resolver,
                             HtPrologParser parser,
                             IPreCompiler<T, PreCompilerTask<T>, P, Q, PC, QC> preCompiler) {
        super(symbolTable, termFactory, compiler, resolver, parser, preCompiler);
    }

    /**
     * @return
     */
    public String getMeta() {
        return meta;
    }

    public final static String meta =
            "solve(Ctx,(G1,G2)):-\n" +
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
