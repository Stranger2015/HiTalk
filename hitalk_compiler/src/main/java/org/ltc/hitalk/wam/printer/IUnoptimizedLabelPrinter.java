package org.ltc.hitalk.wam.printer;

import com.thesett.common.util.doublemaps.SymbolTable;
import com.thesett.text.api.model.TextTableModel;
import org.ltc.hitalk.compiler.IVafInterner;

public
class IUnoptimizedLabelPrinter extends IBasePrinter {
    public IUnoptimizedLabelPrinter ( SymbolTable <Integer, String, Object> symbolTable,
                                IVafInterner interner,
                                HtPositionalTermTraverser traverser,
                                int i,
                                TextTableModel printTable ) {
        super(symbolTable, interner, traverser, i, printTable);
    }
}
