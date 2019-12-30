package org.ltc.hitalk.wam.compiler.prolog;

import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.wam.printer.IPositionalTermTraverser;
import org.ltc.hitalk.wam.printer.IPositionalTermVisitor;

/**
 *
 */
public class HtTopLevelCheckVisitor implements IPositionalTermVisitor {
    protected final ISymbolTable <Integer, String, Object> symbolTable;
    protected final IVafInterner interner;
    protected IPositionalTermTraverser positionalTraverser;

    public HtTopLevelCheckVisitor ( ISymbolTable <Integer, String, Object> symbolTable,
                                    IVafInterner interner,
                                    IPositionalTermTraverser positionalTraverser ) {
        this.symbolTable = symbolTable;
        this.interner = interner;
        this.positionalTraverser = positionalTraverser;
    }

    /**
     * @param positionalTraverser
     */
    public void setPositionalTraverser ( IPositionalTermTraverser positionalTraverser ) {
        this.positionalTraverser = positionalTraverser;
    }
}
