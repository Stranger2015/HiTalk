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

package org.ltc.hitalk.wam.printer;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.Variable;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.util.doublemaps.SymbolTable;
import com.thesett.text.api.model.TextTableModel;
import com.thesett.text.impl.model.TextTableImpl;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;

import java.util.ArrayList;
import java.util.Collection;


public abstract
class HtWAMCompiledTermsPrintingVisitor extends HtDelegatingAllTermsVisitor implements HtPositionalTermVisitor {
    /**
     * The positional traverser used to traverse the clause being printed.
     */
    protected HtPositionalTermTraverser traverser;

    /**
     * The name interner.
     */
    private final VariableAndFunctorInterner interner;

    /**
     * The symbol table.
     */
    private final SymbolTable <Integer, String, Object> symbolTable;

    /**
     * Holds the string buffer to pretty print the results into.
     */
    private final StringBuilder result;

    /**
     * Holds a list of all column printers to apply.
     */
    Collection <HtAllTermsVisitor> printers = new ArrayList <>();

    /**
     * Holds the table model to render the output to.
     */
    TextTableModel printTable = new TextTableImpl();

    /**
     * Creates a pretty printing visitor for clauses being compiled in WAM.
     *
     * @param interner    The symbol name table.
     * @param symbolTable The symbol table for the compilation.
     * @param result      A string buffer to place the results in.
     */
    public
    HtWAMCompiledTermsPrintingVisitor ( VariableAndFunctorInterner interner,
                                        SymbolTable <Integer, String, Object> symbolTable, StringBuilder result ) {
        super(null);
        this.interner = interner;
        this.result = result;
        this.symbolTable = symbolTable;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public
    void setPositionalTraverser ( HtPositionalTermTraverser traverser ) {
        this.traverser = traverser;
    }

    /**
     * {@inheritDoc}
     */
    public
    void visit ( HtPredicate predicate ) {
        for (HtAllTermsVisitor printer : printers) {
            printer.visit(predicate);
        }

        super.visit(predicate);
    }

    /**
     * {@inheritDoc}
     */
    public
    void visit ( HtClause clause ) {
        for (HtAllTermsVisitor printer : printers) {
            printer.visit(clause);
        }

        super.visit(clause);
    }

    /**
     * {@inheritDoc}
     */
    public
    void visit ( Functor functor ) {
        for (HtAllTermsVisitor printer : printers) {
            printer.visit(functor);
        }

        super.visit(functor);
    }

    /**
     * {@inheritDoc}
     */
    public
    void visit ( Variable variable ) {
        for (HtAllTermsVisitor printer : printers) {
            printer.visit(variable);
        }

        super.visit(variable);
    }

    /**
     * Sets up the stack of column printers.
     */
    protected
    void initializePrinters () {
        int maxColumns = 0;

        printers.add(new HtSourceClausePrinter(symbolTable, interner, traverser, maxColumns++, printTable));
        printers.add(new HtPositionPrinter(symbolTable, interner, traverser, maxColumns++, printTable));
        printers.add(new HtUnoptimizedLabelPrinter(symbolTable, interner, traverser, maxColumns++, printTable));
        printers.add(new HtUnoptimizedByteCodePrinter(symbolTable, interner, traverser, maxColumns++, printTable));
        printers.add(new HtLabelPrinter(symbolTable, interner, traverser, maxColumns++, printTable));
        printers.add(new HtByteCodePrinter(symbolTable, interner, traverser, maxColumns, printTable));
    }

    /**
     * Assembles the accumulated output in all rows and columns into a table. The table is appended onto {@link #result}
     */
    protected
    void printTable () {
        for (int i = 0; i < printTable.getRowCount(); i++) {
            for (int j = 0; j < printTable.getColumnCount(); j++) {
                String valueToPrint = printTable.get(j, i);
                valueToPrint = (valueToPrint == null) ? "" : valueToPrint;
                result.append(valueToPrint);

                Integer maxColumnSize = printTable.getMaxColumnSize(j);
                int padding = ((maxColumnSize == null) ? 0 : maxColumnSize) - valueToPrint.length();
                padding = (padding < 0) ? 0 : padding;

                for (int s = 0; s < padding; s++) {
                    result.append(" ");
                }

                result.append(" % ");
            }

            result.append("\n");
        }
    }
}
