package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.bktables.HiTalkFlag;
import org.ltc.hitalk.parser.HtPrologParser;

/**
 *
 */
@Deprecated
public
class HiLogInstructionCompiler extends BaseInstructionCompiler <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> {

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     */
    public
    HiLogInstructionCompiler ( SymbolTable <Integer, String, Object> symbolTable, VariableAndFunctorInterner interner ) {
        super(symbolTable, interner);
    }

    public
    HtPrologParser getParser () {
        return parser;
    }

    /**
     * @param sentence
     * @param flags
     */
    public
    void compile ( Sentence <Clause> sentence, HiTalkFlag... flags ) {

    }

    /**
     * Compiles a sentence into a (presumably binary) form, that provides a Java interface into the compiled structure.
     *
     * @param sentence The sentence to compile.
     */
    @Override
    public
    void compile ( Sentence <Clause> sentence ) {

    }
}
