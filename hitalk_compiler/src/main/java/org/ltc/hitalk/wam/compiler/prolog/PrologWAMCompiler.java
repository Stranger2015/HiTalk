package org.ltc.hitalk.wam.compiler.prolog;

import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.BaseCompiler;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledQuery;
import org.ltc.hitalk.wam.compiler.hitalk.PrologInstructionCompiler;

/**
 *
 * WAMCompiler implements the {@link } interface for the complete WAM compilation chain. It is a
 * supervising compiler, that chains together the work of the compiler pipe-line stages.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Chain together the compiler pipe-line stages.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class PrologWAMCompiler extends BaseCompiler <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> {

    protected PrologPreCompiler preCompiler;
    protected PrologInstructionCompiler instructionCompiler;

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     */
    public PrologWAMCompiler ( SymbolTable <Integer, String, Object> symbolTable,
                               VariableAndFunctorInterner interner, PlPrologParser parser ) {
        super(symbolTable, interner, parser);
    }

    @Override
    public void endScope () throws SourceCodeException {

    }
}
