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
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.task.PreCompilerTask;

import java.util.List;

/**
 *
 */
public class PrologPreprocessor<T extends HtClause, TT extends PreCompilerTask<T>, P, Q, PC, QC>
        extends PrologPreCompiler<T, TT, P, Q, PC, QC> {

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     */
    public PrologPreprocessor(ISymbolTable<Integer, String, Object> symbolTable,
                              IVafInterner interner,
                              PrologDefaultBuiltIn defaultBuiltIn,
                              PrologBuiltInTransform<T, P, Q, PC, QC> builtInTransform,
                              IResolver<PC, QC> resolver,
                              HtPrologParser parser) {

        super(symbolTable, interner, defaultBuiltIn, builtInTransform, resolver, parser);
//        taskQueue.add(new )
    }
//
//    /**
//     * @param clause
//     */
//    public void compile ( T clause ) {
//        super.compile(clause);
//    }

    public List<ITerm> expandTerm(ITerm term) throws Exception {
        return super.expandTerm(term);
    }

    /**
     * @param term
     * @return
     */
    public List<ITerm> callTermExpansion(ITerm term) throws Exception {
        return super.callTermExpansion(term);
    }

    /**
     * @param goal
     * @return
     */
    public List<ITerm> expandGoal(ITerm goal) {
        return super.expandGoal(goal);
    }
}
