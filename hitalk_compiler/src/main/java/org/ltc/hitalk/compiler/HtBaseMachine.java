
package org.ltc.hitalk.compiler;

import org.ltc.hitalk.core.utils.ISymbolTable;

/**
 * BaseMachine provides a base for implementing abstract machines components, such as compilers, interpreters, byte code
 * interpreters and so on, on top of. It encapsulates an extensible symbol table, that allows the mapping of arbitrary
 * fields against symbols and the ability to nest symbols within the scope of other symbols. The symbols may be the
 * interned names of functors or variables in the language.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Provide a symbol table in which arbitrary fields can be held against symbols in the language.
 * <tr><td> Provide an interner to intern variable and functor names with.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public abstract class HtBaseMachine {

    /**
     * Holds the machines symbol table.
     */
    protected ISymbolTable <Integer, String, Object> symbolTable;

    /**
     * Holds the machines symbol name interner.
     */
    protected IVafInterner interner;

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     */
    public HtBaseMachine ( ISymbolTable <Integer, String, Object> symbolTable, IVafInterner interner ) {
        this.symbolTable = symbolTable;
        this.interner = interner;
    }

    /**
     * Provides the symbol table.
     *
     * @return The symbol table.
     */
    public ISymbolTable <Integer, String, Object> getSymbolTable () {
        return symbolTable;
    }

    /**
     * Provides the interner.
     *
     * @return The interner.
     */
    public IVafInterner getInterner () {
        return interner;
    }
}
