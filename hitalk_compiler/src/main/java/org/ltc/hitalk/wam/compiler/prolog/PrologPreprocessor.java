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

import com.thesett.aima.logic.fol.Sentence;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.PrologBuiltInTransform;
import org.ltc.hitalk.compiler.bktables.IApplication;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.interpreter.DcgRule;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlTokenSource;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.task.TransformTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class PrologPreprocessor<TC extends ITerm, TT extends TransformTask <TC>> extends PrologPreCompiler {

    protected final List <TT> components = new ArrayList <>();

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable        The symbol table for the machine.
     * @param interner           The interner for the machine.
     */
    public PrologPreprocessor ( SymbolTable <Integer, String, Object> symbolTable,
                                IVafInterner interner,
                                PrologDefaultBuiltIn defaultBuiltIn,
                                PrologBuiltInTransform <IApplication, HtClause> builtInTransform,
                                IResolver <HtPredicate, HtClause> resolver,
                                PlPrologParser parser ) {

        super(symbolTable, interner, defaultBuiltIn, builtInTransform, resolver, parser);
    }

    @Override
    public void compile ( PlTokenSource tokenSource, HtProperty... flags ) throws IOException, SourceCodeException {

    }

    @Override
    public void compile ( HtClause clause, HtProperty... flags ) throws SourceCodeException {

    }

    @Override
    public void compileDcgRule ( DcgRule rule ) throws SourceCodeException {

    }

    @Override
    public void compileQuery ( HtClause query ) throws SourceCodeException {

    }

    @Override
    public void compile ( String fileName, HtProperty[] flags ) {

    }

    @Override
    public void compile ( Sentence <HtClause> sentence ) throws SourceCodeException {

    }

    @Override
    public void endScope () throws SourceCodeException {

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