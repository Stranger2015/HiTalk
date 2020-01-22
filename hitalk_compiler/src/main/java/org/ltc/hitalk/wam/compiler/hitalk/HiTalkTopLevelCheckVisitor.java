/*
 * Copyright The Sett Ltd, 2005 to 2014.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ltc.hitalk.wam.compiler.hitalk;

import com.thesett.aima.logic.fol.LinkageException;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.ltc.hitalk.wam.compiler.builtins.HtConjunction;
import org.ltc.hitalk.wam.compiler.builtins.HtDisjunction;
import org.ltc.hitalk.wam.printer.HtBasePositionalVisitor;
import org.ltc.hitalk.wam.printer.IPositionalContext;
import org.ltc.hitalk.wam.printer.IPositionalTermTraverser;
import org.ltc.hitalk.wam.printer.IPositionalTermVisitor;

import static com.thesett.aima.logic.fol.wam.compiler.SymbolTableKeys.SYMKEY_TOP_LEVEL_FUNCTOR;

/**
 * Conjunctions and disjunctions are treated specially by this transform. The conjunction and disjunction operators may
 * appear within any structure, but are only to be compiled as such if they are 'top-level'. They are considered
 * top-level when they appear at the top-level within a clause, or directly beneath a parent conjunction or disjunction
 * that is considered to be top-level. Effectively they are flattened into the top-level of the clause in which they
 * appear, but the original structure is preserved rather than actually flattened at this time, as it can change meaning
 * depending on how the term is bracketed.
 * <p>
 * <p/>This traversal simply marks all conjunctions and disjunctions that are part of the clause top-level, with the
 * top-level flag. The functors appearing as arguments to those terms, are also marked as top-level, since they will be
 * evaluated by the clause, not treated as structure definitions.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Check which functors are considered to be top-level within a clause. </td></tr>
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class HiTalkTopLevelCheckVisitor extends HtBasePositionalVisitor implements IPositionalTermVisitor {

    // Used for debugging.
    /* private static final Logger log = Logger.getLogger(TopLevelCheckVisitor.class.getName()); */

    /**
     * Creates the visitor with the supplied interner, symbol table and traverser.
     *
     * @param interner    The name interner.
     * @param symbolTable The compiler symbol table.
     * @param traverser   The positional context traverser.
     */
    public HiTalkTopLevelCheckVisitor (
            ISymbolTable <Integer, String, Object> symbolTable,
            IVafInterner interner,
            IPositionalTermTraverser traverser ) {

        super(symbolTable, interner, traverser);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPositionalTraverser ( IPositionalTermTraverser traverser ) {
        this.traverser = traverser;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p/>Sets the top-level flag on a functor, if appropriate.
     */
    protected void enterFunctor ( IFunctor functor ) {
        if (isTopLevel()) {
            symbolTable.put(functor.getString(), SYMKEY_TOP_LEVEL_FUNCTOR, true);
        }
    }

    /**
     * Functors are considered top-level when they appear at the top-level within a clause, or directly beneath a parent
     * conjunction or disjunction that is considered to be top-level.
     *
     * @return <tt>true</tt> iff the current position is considered to be top-level.
     */
    private boolean isTopLevel () {
        if (traverser.isTopLevel()) {
            return true;
        } else {
            IPositionalContext parentContext = traverser.getParentContext();

            if (parentContext != null) {
                ITerm parentTerm = parentContext.getTerm();

                if ((parentTerm instanceof HtConjunction) || (parentTerm instanceof HtDisjunction)) {
                    Boolean isTopLevel = (Boolean) symbolTable.get(parentTerm.getString(), SYMKEY_TOP_LEVEL_FUNCTOR);

                    return (isTopLevel == null) ? false : isTopLevel;
                }
            }
        }

        return false;
    }

    public void visit ( HtPredicate predicate ) {

    }

    public void visit ( IFunctor functor ) throws LinkageException {

    }

    public void visit ( HtClause clause ) throws LinkageException {

    }

    public void visit ( ListTerm listTerm ) throws LinkageException {

    }
}
