package org.ltc.hitalk.wam.compiler.hilog;

import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.wam.compiler.prolog.PrologBuiltInTransform;
import org.ltc.hitalk.wam.compiler.prolog.PrologDefaultBuiltIn;
import org.ltc.hitalk.wam.compiler.prolog.PrologPreCompiler;
import org.ltc.hitalk.wam.compiler.prolog.PrologPreprocessor;
import org.ltc.hitalk.wam.task.PreCompilerTask;

public class HiLogPreCompiler<T extends HtClause, TT extends PreCompilerTask<T>, P, Q, PC, QC>
        extends PrologPreCompiler<T, TT, P, Q, PC, QC> {
    /**
     * @param symbolTable
     * @param interner
     * @param defaultBuiltIn
     * @param builtInTransform
     * @param resolver
     * @param parser
     */
    public HiLogPreCompiler(ISymbolTable<Integer, String, Object> symbolTable,
                            IVafInterner interner,
                            PrologDefaultBuiltIn defaultBuiltIn,
                            PrologBuiltInTransform<T, P, Q, PC, QC> builtInTransform,
                            IResolver<PC, QC> resolver,
                            HtPrologParser parser) {
        super(symbolTable, interner, defaultBuiltIn, builtInTransform, resolver, parser);
    }

    protected void initTasks(PrologPreprocessor<T, TT, P, Q, PC, QC> pp) {
        super.initTasks(pp);
    }

//    public HiLogPreCompiler() throws Exception {
//        super();
//    }
}