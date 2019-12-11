package org.ltc.hitalk.wam.compiler.hilog;

import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.PrologBuiltInTransform;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.prolog.PrologDefaultBuiltIn;
import org.ltc.hitalk.wam.compiler.prolog.PrologPreprocessor;
import org.ltc.hitalk.wam.task.TransformTask;

/**
 * /*decode_hilog_term(HiLogPred,[Arg],Code,_Level,Depth) :-
 * Depth1 is Depth+1,
 * is_unary_op(HiLogPred),
 * !,
 * escape(HiLogPred,EscPred),
 * decode_literal_internal(Arg,ArgCode,1,Depth1),
 * Code = [EscPred,' ',ArgCode].
 * <p>
 * decode_hilog_term(HiLogPred,[Arg1,Arg2],Code,_Level,Depth) :-
 * Depth1 is Depth+1,
 * is_binary_op(HiLogPred),
 * !,
 * escape(HiLogPred,EscPred),
 * decode_literal_internal(Arg1,Arg1Code,1,Depth1),
 * decode_literal_internal(Arg2,Arg2Code,1,Depth1),
 * Code = [Arg1Code,' ',EscPred,' ',Arg2Code].
 * <p>
 * decode_hilog_term(HiLogPred,Args,Code,Level,Depth) :-
 * Depth1 is Depth+1,
 * decode_literal_internal(HiLogPred,HiLogPredCode,1,Depth1),
 * Code = [HiLogPredCode|RestCode1],
 * (Args == []
 * -> (Level==0 -> RestCode1 = RestCode2
 * ; RestCode1 = ['(',')'|RestCode2]
 * )
 * ;
 * decode_list_add_separator(Args,ArgCode1,decode_literal_internal(_,_,1,Depth1),FL_COMMA),
 * RestCode1 = ['(',ArgCode1,')'|RestCode2]
 * ),
 * RestCode2 = [].
 */
public class HiLogPreprocessor<T extends HtClause, P, Q, TC extends ITerm, TT extends TransformTask <TC>>
        extends PrologPreprocessor <T, P, Q, TC, TT> {
    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable      The symbol table for the machine.
     * @param interner         The interner for the machine.
     * @param defaultBuiltIn
     * @param builtInTransform
     * @param resolver
     * @param parser
     */
    public HiLogPreprocessor ( ISymbolTable <Integer, String, Object> symbolTable,
                               IVafInterner interner,
                               PrologDefaultBuiltIn defaultBuiltIn,
                               PrologBuiltInTransform builtInTransform,
                               IResolver resolver,
                               PlPrologParser parser ) {
        super(symbolTable, interner, defaultBuiltIn, builtInTransform, resolver, parser);
    }
}
