package org.ltc.hitalk.wam.printer;

import com.thesett.common.util.doublemaps.SymbolTable;
import com.thesett.text.api.model.TextTableModel;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledQuery;
import org.ltc.hitalk.wam.compiler.HiTalkWAMInstruction;
import org.ltc.hitalk.wam.compiler.HiTalkWAMOptimizeableListing;

public
class HtUnoptimizedByteCodePrinter extends HtBasePrinter {
    public
    HtUnoptimizedByteCodePrinter ( SymbolTable <Integer, String, Object> symbolTable,
                                   IVafInterner interner,
                                   HtPositionalTermTraverser traverser, int i, TextTableModel printTable ) {
        super(symbolTable, interner, traverser, i, printTable);
    }


    /**
     * {@inheritDoc}
     */
    protected
    void enterPredicate ( HtPredicate predicate ) {
        if (predicate instanceof HiTalkWAMCompiledPredicate) {
            HiTalkWAMOptimizeableListing compiledPredicate = (HiTalkWAMCompiledPredicate) predicate;

            for (HiTalkWAMInstruction instruction : compiledPredicate.getUnoptimizedInstructions()) {
                addLineToRow(instruction.toString());
                nextRow();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected
    void enterClause ( HtClause clause ) {
        if (clause instanceof HiTalkWAMCompiledQuery) {
            HiTalkWAMOptimizeableListing query = (HiTalkWAMCompiledQuery) clause;

            for (HiTalkWAMInstruction instruction : query.getUnoptimizedInstructions()) {
                addLineToRow(instruction.toString());
                nextRow();
            }
        }
    }
}
