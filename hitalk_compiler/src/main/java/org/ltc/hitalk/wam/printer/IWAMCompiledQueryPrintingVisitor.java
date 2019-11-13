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

import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.parser.HtClause;

/**
 * Pretty printer for compiled queries.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Pretty print compiled queries with internal information about the compilation.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public
class HtWAMCompiledQueryPrintingVisitor extends IWAMCompiledTermsPrintingVisitor {
    /**
     * Creates a pretty printing visitor for clauses being compiled in WAM.
     *
     * @param interner    The symbol name table.
     * @param symbolTable The symbol table for the compilation.
     * @param result      A string buffer to place the results in.
     */
    public HtWAMCompiledQueryPrintingVisitor ( SymbolTable <Integer, String, Object> symbolTable,
                                               IVafInterner interner,
                                               StringBuilder result ) {
        super(interner, symbolTable, result);
    }

    /**
     * {@inheritDoc}
     */
    public
    void visit ( HtClause clause ) {
        if (traverser.isEnteringContext()) {
            initializePrinters();
        }
        else if (traverser.isLeavingContext()) {
            printTable();
        }

        super.visit(clause);
    }
}
