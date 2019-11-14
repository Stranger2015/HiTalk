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

import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.Variable;
import com.thesett.common.util.doublemaps.SymbolTable;
import com.thesett.text.api.model.TextTableModel;
import com.thesett.text.impl.model.TextTableImpl;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 */
public abstract
class HtWAMCompiledTermsPrintingVisitor extends HtDelegatingAllTermsVisitor implements IPositionalTermVisitor {
    /**
     * The positional traverser used to traverse the clause being printed.
     */
    protected IPositionalTermTraverser traverser;

    /**
     * The name interner.
     */
    protected final IVafInterner interner;

    /**
     * The symbol table.
     */
    protected final SymbolTable <Integer, String, Object> symbolTable;

    /**
     * Holds the string buffer to pretty print the results into.
     */
    protected final StringBuilder result;

    /**
     * Holds a list of all column printers to apply.
     */
    Collection <IAllTermsVisitor> printers = new ArrayList <>();

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
    public HtWAMCompiledTermsPrintingVisitor ( IVafInterner interner,
                                               SymbolTable <Integer, String, Object> symbolTable,
                                               StringBuilder result ) {
        super(null);
        this.interner = interner;
        this.result = result;
        this.symbolTable = symbolTable;

    }

    /**
     * Creates an AllTermsVisitor that by default delegates all visit operations to the specified delegate.
     * @param delegate The delegate, may be <tt>null</tt> if none is to be used.
     * @param interner
     * @param symbolTable
     * @param result
     */
    public HtWAMCompiledTermsPrintingVisitor ( IAllTermsVisitor delegate,
                                               IVafInterner interner,
                                               SymbolTable <Integer, String, Object> symbolTable,
                                               StringBuilder result ) {
        super(delegate);
        this.interner = interner;
        this.symbolTable = symbolTable;
        this.result = result;
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
     */
    public
    void visit ( HtPredicate predicate ) {
        printers.forEach(printer -> printer.visit(predicate));
        super.visit(predicate);
    }

    /**
     * {@inheritDoc}
     */
    public void visit ( HtClause clause ) throws LinkageException {
        for (IAllTermsVisitor printer : printers) {
            printer.visit(clause);
        }

        super.visit(clause);
    }

    /**
     * {@inheritDoc}
     */
    public void visit ( IFunctor functor ) throws LinkageException {
        for (IAllTermsVisitor printer : printers) {
            printer.visit(functor);
        }

        super.visit(functor);
    }

    /**
     * {@inheritDoc}
     */
    public
    void visit ( Variable variable ) {
        printers.forEach(printer -> printer.visit(variable));

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
                int padding = (Optional.of(maxColumnSize).orElse(0)) - valueToPrint.length();
                padding = Math.max(padding, 0);

                result.append(IntStream.range(0, padding).mapToObj(s -> " ")
                        .collect(Collectors.joining("", "", " % ")));
            }

            result.append("\n");
        }
    }
}
