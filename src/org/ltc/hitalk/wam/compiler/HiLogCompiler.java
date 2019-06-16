package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.*;
import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;

/**
 *
 */
public
class HiLogCompiler extends BaseCompiler <Clause, HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> {

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     */
    public
    HiLogCompiler ( SymbolTable <Integer, String, Object> symbolTable, VariableAndFunctorInterner interner ) {
        super(symbolTable, interner);
    }

    @Override
    public
    LogicCompiler <Clause, Clause, Clause> getPreCompiler () {
        return null;
    }

    @Override
    public
    Parser <Clause, Token> getParser () {
        return (Parser <Clause, Token>) parser;
    }

    /**
     * Compiles a sentence into a (presumably binary) form, that provides a Java interface into the compiled structure.
     *
     * @param sentence The sentence to compile.
     * @throws SourceCodeException If there is an error in the source to be compiled that prevents its compilation.
     */
    @Override
    public
    void compile ( Sentence <Clause> sentence ) throws SourceCodeException {

    }
}
