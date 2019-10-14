package org.ltc.hitalk.wam.compiler.prolog;

import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.BaseCompiler;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledQuery;

/**
 *
 */
public class PrologWAMCompiler extends BaseCompiler <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> {

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
}
