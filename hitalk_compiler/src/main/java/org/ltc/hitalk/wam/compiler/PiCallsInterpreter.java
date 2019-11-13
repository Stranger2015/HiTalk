package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.IVafInterner;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.wam.printer.HtBasePositionalVisitor;
import org.ltc.hitalk.wam.printer.HtPositionalTermTraverser;
import org.ltc.hitalk.wam.printer.IPositionalTermVisitor;

public class PiCallsInterpreter extends HtBasePositionalVisitor
        implements IPositionalTermVisitor {

    protected HtPositionalTermTraverser positionalTraverser;

    /**
     * Creates a positional visitor.
     *
     * @param symbolTable The compiler symbol table.
     * @param interner    The name interner.
     */
    public PiCallsInterpreter ( SymbolTable <Integer, String, Object> symbolTable, IVafInterner interner ) {
        super(symbolTable, interner);
    }

    public void setPositionalTraverser ( HtPositionalTermTraverser positionalTraverser ) {

    }
}
