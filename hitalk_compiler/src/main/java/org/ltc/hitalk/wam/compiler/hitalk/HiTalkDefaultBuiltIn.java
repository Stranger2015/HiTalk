package org.ltc.hitalk.wam.compiler.hitalk;


import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.wam.compiler.prolog.PrologDefaultBuiltIn;

/**
 * DefaultBuiltIn implements the standard WAM Prolog compilation for normal Prolog programs. Splitting this out into
 * DefaultBuiltIn which supplies the  interface, allows different compilations to be used for built in
 * predicates that behave differently to the normal compilation.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Generate instructions to set up the arguments to a call to a built-in functor.</td></tr>
 * <tr><td> Generate instructions to call to a built-in functor.</td></tr>
 * </table></pre>
 *
 * @author Rupert Smith
 */
public
class HiTalkDefaultBuiltIn extends PrologDefaultBuiltIn {

    /* Used for debugging. */
    /* private static final Logger log = Logger.getLogger(DefaultBuiltIn.class.getName()); */

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     */
    public HiTalkDefaultBuiltIn ( ISymbolTable <Integer, String, Object> symbolTable,
                                  IVafInterner interner ) {
        super(symbolTable, interner);
    }
}