package org.ltc.hitalk.wam.printer;

import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.util.doublemaps.SymbolTable;
import com.thesett.text.api.model.TextTableModel;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledQuery;
import org.ltc.hitalk.wam.compiler.HiTalkWAMInstruction;
import org.ltc.hitalk.wam.compiler.HiTalkWAMOptimizeableListing;

public
class HtByteCodePrinter extends HtBasePrinter {
    public
    HtByteCodePrinter ( SymbolTable <Integer, String, Object> symbolTable,
                        VariableAndFunctorInterner interner,
                        HtPositionalTermTraverser traverser, int i, TextTableModel printTable ) {
        super(symbolTable, interner, traverser, i, printTable);
    }

    /**
     * {@inheritDoc}
     */
    protected
    void enterClause ( HtClause clause ) {
        if (clause instanceof HiTalkWAMCompiledQuery) {
            HiTalkWAMOptimizeableListing query = (HiTalkWAMCompiledQuery) clause;

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
            HiTalkWAMOptimizeableListing compiledPredicate = (HiTalkWAMCompiledPredicate) predicate;

            for (HiTalkWAMInstruction instruction : compiledPredicate.getInstructions()) {
                addLineToRow(instruction.toString());
                nextRow();
            }
        }
    }
}



