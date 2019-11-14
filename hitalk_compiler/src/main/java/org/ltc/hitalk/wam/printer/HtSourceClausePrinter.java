package org.ltc.hitalk.wam.printer;

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

import com.thesett.aima.logic.fol.Variable;
import com.thesett.common.util.doublemaps.SymbolTable;
import com.thesett.text.api.model.TextTableModel;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.wam.compiler.IFunctor;

/**
 * SourceClausePrinter prints the source of a compiled clause in a vertically spread out way, so that each predicate
 * call and argument appears in its own row. This leaves enough space vertically to print the compiled byte code
 * alongside.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Print each predicate call and argument in its own row.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public
class HtSourceClausePrinter extends HtBasePrinter {
    /**
     * Creates a printer.
     *
     * @param interner    The name interner.
     * @param symbolTable The compiler symbol table.
     * @param traverser   The positional context traverser.
     * @param column      The column to print to.
     * @param table       The table to inform of cell sizes and positions.
     */
    public HtSourceClausePrinter ( SymbolTable <Integer, String, Object> symbolTable,
                                   IVafInterner interner, IPositionalTermTraverser traverser, int column, TextTableModel table ) {
        super(symbolTable, interner, traverser, column, table);
    }

    /**
     * {@inheritDoc}
     */
    protected void enterFunctor ( IFunctor functor ) {
        String showContextAs = interner.getFunctorName(functor) + (functor.isAtom() ? "" : "(");

        int delta = showContextAs.length();

        addLineToRow(indent + showContextAs);
        nextRow();

        indent = indenter.generateTraceIndent(delta);
    }

    /**
     * {@inheritDoc}
     */
    protected void leaveFunctor ( IFunctor functor ) {
        String toAppend = indent;
        boolean addData = false;

        if (functor.isCompound()) {
            toAppend += ")";
            addData = true;
        }

        if (!traverser.isInHead() && !traverser.isLastBodyFunctor() && traverser.isTopLevel()) {
            toAppend += ",";
            addData = true;
        }

        if (traverser.isInHead() && traverser.isTopLevel()) {
            toAppend += " :-";
            addData = true;
        }

        if (addData) {
            addLineToRow(toAppend);
            nextRow();
        }

        indent = indenter.generateTraceIndent(-indenter.getLastDelta());
    }

    /**
     * {@inheritDoc}
     */
    protected
    void enterVariable ( Variable variable ) {
        String showContextAs = interner.getVariableName(variable);

        int delta = showContextAs.length();

        addLineToRow(indent + showContextAs);
        nextRow();

        indent = indenter.generateTraceIndent(delta);
    }
}
