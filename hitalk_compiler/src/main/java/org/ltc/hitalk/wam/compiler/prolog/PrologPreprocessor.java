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
