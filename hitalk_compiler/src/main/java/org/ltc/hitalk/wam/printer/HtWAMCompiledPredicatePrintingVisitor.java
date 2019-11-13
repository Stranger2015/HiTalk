package org.ltc.hitalk.wam.printer;

import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.entities.HtPredicate;

public
class HtWAMCompiledPredicatePrintingVisitor extends HtWAMCompiledTermsPrintingVisitor {
    /**
     * Creates a pretty printing visitor for clauses being compiled in WAM.
     *
     * @param interner    The symbol name table.
     * @param symbolTable The symbol table for the compilation.
     * @param result      A string buffer to place the results in.
     */
    public HtWAMCompiledPredicatePrintingVisitor (
            SymbolTable <Integer, String, Object> symbolTable,
            IVafInterner interner,
            StringBuilder result ) {
        super(interner, symbolTable, result);
    }

    /**
     * {@inheritDoc}
     */
    public
    void visit ( HtPredicate predicate ) {
        if (traverser.isEnteringContext()) {
            initializePrinters();
        }
        else if (traverser.isLeavingContext()) {
            printTable();
        }

        super.visit(predicate);
    }
}

