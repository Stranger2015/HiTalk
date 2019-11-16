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
package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.Variable;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.printer.HtBasePositionalVisitor;
import org.ltc.hitalk.wam.printer.IPositionalTermTraverser;
import org.ltc.hitalk.wam.printer.IPositionalTermVisitor;

public class HiTalkMetaInterpreterVisitor extends HtBasePositionalVisitor
        implements IPositionalTermVisitor {

    protected IPositionalTermTraverser positionalTraverser;

    /**
     * Creates a positional visitor.
     *
     * @param symbolTable The compiler symbol table.
     * @param interner    The name interner.
     */
    public HiTalkMetaInterpreterVisitor ( SymbolTable <Integer, String, Object> symbolTable, IVafInterner interner ) {
        super(symbolTable, interner);
    }

    protected void enterPredicate ( HtPredicate predicate ) {
        super.enterPredicate(predicate);
    }

    protected void leavePredicate ( HtPredicate predicate ) {
        super.leavePredicate(predicate);
    }

    protected void enterListTerm ( ListTerm dottedPair ) throws LinkageException {
        super.enterListTerm(dottedPair);
    }

    protected void leaveListTerm ( ListTerm dottedPair ) {
        super.leaveListTerm(dottedPair);
    }

    protected void enterFunctor ( IFunctor functor ) throws LinkageException {
        super.enterFunctor(functor);
    }

    protected void leaveFunctor ( IFunctor functor ) {
        super.leaveFunctor(functor);
    }

    protected void enterVariable ( Variable variable ) {
        super.enterVariable(variable);
    }

    protected void leaveVariable ( Variable variable ) {
        super.leaveVariable(variable);
    }

    protected void enterClause ( HtClause clause ) throws LinkageException {
        super.enterClause(clause);
    }

    protected void leaveClause ( HtClause clause ) {
        super.leaveClause(clause);
    }

    public void setPositionalTraverser ( IPositionalTermTraverser positionalTraverser ) {
        this.positionalTraverser = positionalTraverser;
    }
}
