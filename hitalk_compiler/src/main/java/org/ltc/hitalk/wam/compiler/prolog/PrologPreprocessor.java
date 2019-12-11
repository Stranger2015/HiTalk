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
import org.ltc.hitalk.wam.compiler.IFunctor;
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

    public void compile ( T clause ) {
        super.compile(clause);
    }

    /**
     * expand_term(+Term1, -Term2)
     * This predicate is normally called by the compiler on terms read from the input to perform preprocessing.
     * It consists of four steps, where each step processes the output of the previous step.
     * Test conditional compilation directives and translate all input to [] if we are in a `false branch' of
     * the conditional compilation. See section 4.3.1.2.
     * <p>
     * Call term_expansion/2. This predicate is first tried in the module that is being compiled and then
     * in modules from which this module inherits according to default_module/2. The output of the expansion in
     * a module is used as input for the next module. Using the default setup and when compiling a normal
     * application module M, this implies expansion is executed in M, user and finally in system. Library modules
     * inherit directly from system and can thus not be re-interpreted by term expansion rules in user.
     * <p>
     * Call DCG expansion (dcg_translate_rule/2).
     * <p>
     * Call expand_goal/2 on each body term that appears in the output of the previous steps.
     *
     * @param term
     * @return
     */
    public List <ITerm> expandTerm ( ITerm term ) {
        return super.expandTerm(term);
    }

    /**
     * @param term
     * @return
     */
    public List <ITerm> callTermExpansion ( ITerm term ) {
        return super.callTermExpansion(term);
    }

    /**
     * @param goal
     * @return
     */
    public List <IFunctor> expandGoal ( IFunctor goal ) {
        return super.expandGoal(goal);
    }
}
