package org.ltc.hitalk.wam.printer;

import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.util.doublemaps.SymbolTable;
import com.thesett.text.api.model.TextTableModel;

public
class HtUnoptimizedLabelPrinter extends HtBasePrinter {
    public
    HtUnoptimizedLabelPrinter ( SymbolTable <Integer, String, Object> symbolTable,
                                VariableAndFunctorInterner interner,
                                HtPositionalTermTraverser traverser,
                                int i,
                                TextTableModel printTable ) {
        super(symbolTable, interner, traverser, i, printTable);
    }
}
