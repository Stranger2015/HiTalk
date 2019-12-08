package org.ltc.hitalk.wam.printer;

import com.thesett.text.api.model.TextTableModel;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.utils.ISymbolTable;

public
class HtLabelPrinter extends HtBasePrinter {

    public HtLabelPrinter ( ISymbolTable <Integer, String, Object> symbolTable,
                            IVafInterner interner,
                            IPositionalTermTraverser traverser, int i, TextTableModel printTable ) {
        super(symbolTable, interner, traverser, i, printTable);
    }
}
