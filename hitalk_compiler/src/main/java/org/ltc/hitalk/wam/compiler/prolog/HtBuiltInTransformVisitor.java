package org.ltc.hitalk.wam.compiler.prolog;/*  * Copyright The Sett Ltd, 2005 to 2014.  *  * Licensed under the Apache License, Version 2.0 (the "License");  * you may not use this file except in compliance with the License.  * You may obtain a copy of the License at  *  *     http://www.apache.org/licenses/LICENSE-2.0  *  * Unless required by applicable law or agreed to in writing, software  * distributed under the License is distributed on an "AS IS" BASIS,  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  * See the License for the specific language governing permissions and  * limitations under the License.  */

import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.HtPositionalTermTraverser;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.ltc.hitalk.wam.compiler.builtins.PrologBuiltInFunctor;
import org.ltc.hitalk.wam.printer.HtBasePositionalVisitor;
import org.ltc.hitalk.wam.printer.IPositionalTermTraverser;
import org.ltc.hitalk.wam.printer.IPositionalTermVisitor;

/**
 * BuiltInTransformVisitor should be used with a depth first positional walk over a term to compile.
 * On leaving each term, that is in a post-fix order, if the term is a functor, the built-in transformation function
 * is applied to it.
 * If the built-in applies a transformation to a functor, it is substituted within its parent for the built-in.
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations  * <tr><td> Substitute built-ins into a term to compile.</td></tr>
 * </table></pre>  *  * @author Rupert Smith
 */
public class HtBuiltInTransformVisitor extends HtBasePositionalVisitor
        implements IPositionalTermVisitor {
    /* private static final Logger log = Logger.getLogger(BuiltInTransformVisitor.class.getName()); */

    /**
     * Holds the built in transformation function.
     */
    protected final PrologBuiltInTransform <HtClause, HtPredicate, HtClause> builtInTransform;

    /**
     * Creates the visitor with the supplied interner, symbol table and traverser.
     *
     * @param interner         The name interner.
     * @param symbolTable      The compiler symbol table.
     * @param traverser        The positional context traverser.
     * @param builtInTransform The built-in transformation function.
     */
    public HtBuiltInTransformVisitor (
            ISymbolTable <Integer, String, Object> symbolTable,
            IVafInterner interner,
            HtPositionalTermTraverser traverser,
            PrologBuiltInTransform <HtClause, HtPredicate, HtClause> builtInTransform ) {

        super(symbolTable, interner, traverser);
        this.builtInTransform = builtInTransform;
    }

    /**
     * {@inheritDoc}
     */
    public void setPositionalTraverser ( IPositionalTermTraverser traverser ) {
        this.traverser = traverser;
    }

    /**
     * Applies the built-in transform during a post-fix visit of a term.
     *
     * @param functor The functor to visit.
     */
    protected void leaveFunctor ( IFunctor functor ) {
        /*log.fine("Transformed: " + functor + " to " + transformed.getClass());*/
        int pos = traverser.getPosition();
        if (!traverser.isInHead() && (pos >= 0)) {
            IFunctor transformed = builtInTransform.apply(functor);
            if (functor != transformed) {
                PrologBuiltInFunctor builtInFunctor = (PrologBuiltInFunctor) transformed;
                ITerm parentTerm = traverser.getParentContext().getTerm();
                if (parentTerm instanceof HtClause) {
                    HtClause parentClause = (HtClause) parentTerm;
                    parentClause.getBody().setHead(pos, builtInFunctor);
                } else if (parentTerm instanceof IFunctor) {
                    IFunctor parentFunctor = (IFunctor) parentTerm;
                    parentFunctor.setArgument(pos, builtInFunctor);
                }
            }
        }
    }
}
