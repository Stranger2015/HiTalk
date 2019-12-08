/*
 * Copyright The Sett Ltd, 2005 to 2014.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ltc.hitalk.wam.compiler.prolog;

import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.PrologBuiltInTransform;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.task.TransformTask;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class PrologPreprocessor<T extends HtClause, P, Q, TC extends ITerm, TT extends TransformTask <TC>>
        extends PrologPreCompiler <T, P, Q> {

    protected final List <TT> components = new ArrayList <>();

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     */
    public PrologPreprocessor ( ISymbolTable <Integer, String, Object> symbolTable,
                                IVafInterner interner,
                                PrologDefaultBuiltIn defaultBuiltIn,
                                PrologBuiltInTransform <T, P, Q> builtInTransform,
                                IResolver <HtPredicate, HtClause> resolver,
                                PlPrologParser parser ) {

        super(symbolTable, interner, defaultBuiltIn, builtInTransform, resolver, parser);
    }
}

/*decode_hilog_term(HiLogPred,[Arg],Code,_Level,Depth) :-
	Depth1 is Depth+1,
	is_unary_op(HiLogPred),
	!,
        escape(HiLogPred,EscPred),
	decode_literal_internal(Arg,ArgCode,1,Depth1),
	Code = [EscPred,' ',ArgCode].

decode_hilog_term(HiLogPred,[Arg1,Arg2],Code,_Level,Depth) :-
	Depth1 is Depth+1,
	is_binary_op(HiLogPred),
	!,
        escape(HiLogPred,EscPred),
	decode_literal_internal(Arg1,Arg1Code,1,Depth1),
	decode_literal_internal(Arg2,Arg2Code,1,Depth1),
	Code = [Arg1Code,' ',EscPred,' ',Arg2Code].

decode_hilog_term(HiLogPred,Args,Code,Level,Depth) :-
	Depth1 is Depth+1,
	decode_literal_internal(HiLogPred,HiLogPredCode,1,Depth1),
	Code = [HiLogPredCode|RestCode1],
	(Args == []
	-> (Level==0 -> RestCode1 = RestCode2
	   ; RestCode1 = ['(',')'|RestCode2]
	   )
	;
	    decode_list_add_separator(Args,ArgCode1,decode_literal_internal(_,_,1,Depth1),FL_COMMA),
	    RestCode1 = ['(',ArgCode1,')'|RestCode2]
	),
	RestCode2 = [].
  */