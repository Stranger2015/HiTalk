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

import com.thesett.common.util.TraceIndenter;
import com.thesett.text.api.model.TextTableModel;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.utils.ISymbolTable;

/**
 * BasePrinter is a base class for writing debug pretty printers for compiled clauses.
 * <p>
 * <p/>It uses positional context information from a {@link }, to determine whether terms are
 * being entered or left, and splits these down into calls on appropriate enter/leave methods. Default no-op
 * implementations of these methods are supplied by this base class and may be extended by specific printers to append
 * data into table cells, using the {@link #addLineToRow(String)} and {@link #nextRow()} methods.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Provide default enter/leave methods for every part of a term.
 * <tr><td> Accept lines of data appended into table cells.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public
class HtBasePrinter extends HtBasePositionalVisitor {
    /**
     * Defines the column that this printer outputs to.
     */
    private final int currentColumn;

    /**
     * Holds the current row this printer is outputting to.
     */
    protected int currentRow;

    /**
     * The context depth indenter.
     */
    protected TraceIndenter indenter = new TraceIndenter(true);

    /**
     * The current indent.
     */
    protected String indent = "";

    /**
     * The grid of cells to output to.
     */
    private final TextTableModel table;

    /**
     * Creates a printer.
     *
     * @param interner    The name interner.
     * @param symbolTable The compiler symbol table.
     * @param traverser   The positional context traverser.
     * @param column      The column to print to.
     * @param table       The table to inform of cell sizes and positions.
     */
    public HtBasePrinter ( ISymbolTable <Integer, String, Object> symbolTable,
                           IVafInterner interner,
                           IPositionalTermTraverser traverser,
                           int column, TextTableModel table ) {
        super(symbolTable, interner, traverser);

        this.currentColumn = column;
        this.table = table;
    }

    /**
     * Adds a line of data to the current cell.
     *
     * @param line The line to print in the current cell.
     */
    protected
    void addLineToRow ( String line ) {
        table.put(currentColumn, currentRow, line);
    }

    /**
     * Moves on to the next cell in the next row of the table.
     */
    protected
    void nextRow () {
        ++currentRow;
    }
}
