package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.compiler.PositionalTermTraverser;
import com.thesett.aima.logic.fol.wam.compiler.PositionAndOccurrenceVisitor;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.IVafInterner;

public class HtPositionAndOccurrenceVisitor extends PositionAndOccurrenceVisitor {
    public HtPositionAndOccurrenceVisitor ( IVafInterner interner, SymbolTable <Integer, String, Object> symbolTable, PositionalTermTraverser positionalTraverser ) {
        super();
    }
}
