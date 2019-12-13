package org.ltc.hitalk.wam.printer;

import com.thesett.text.api.model.TextTableModel;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.IWAMOptimizeableListing;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledQuery;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMInstruction;

public
class HtByteCodePrinter extends HtBasePrinter {
    public HtByteCodePrinter ( ISymbolTable <Integer, String, Object> symbolTable,
                               IVafInterner interner,
                               IPositionalTermTraverser traverser, int i, TextTableModel printTable ) {
        super(symbolTable, interner, traverser, i, printTable);
    }

    /**
     * {@inheritDoc}
     */
    protected
    void enterClause ( HtClause clause ) {
        if (clause instanceof HiTalkWAMCompiledQuery) {
            IWAMOptimizeableListing query = (HiTalkWAMCompiledQuery) clause;

            for (HiTalkWAMInstruction instruction : query.getInstructions()) {
                addLineToRow(instruction.toString());
                nextRow();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected
    void enterPredicate ( HtPredicate predicate ) {
        if (predicate instanceof HiTalkWAMCompiledPredicate) {
            IWAMOptimizeableListing compiledPredicate = (HiTalkWAMCompiledPredicate) predicate;

            for (HiTalkWAMInstruction instruction : compiledPredicate.getInstructions()) {
                addLineToRow(instruction.toString());
                nextRow();
            }
        }
    }
}



