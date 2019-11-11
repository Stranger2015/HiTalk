package org.ltc.hitalk.wam.compiler.prolog;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.Resolver;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.BaseCompiler;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlTokenSource;
import org.ltc.hitalk.wam.compiler.hitalk.PrologInstructionCompiler;

import java.io.IOException;

/**
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
public class PrologWAMCompiler<T extends HtClause, P, Q>
        extends BaseCompiler <T, P, Q> {

    protected PrologPreCompiler preCompiler;
    protected PrologInstructionCompiler instructionCompiler;

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     */
    public PrologWAMCompiler ( SymbolTable <Integer, String, Object> symbolTable,
                               VariableAndFunctorInterner interner,
                               PlPrologParser parser,
                               LogicCompilerObserver <P, Q> observer ) {
        super(symbolTable, interner, parser, observer);
    }

    @Override
    public void endScope () throws SourceCodeException {

    }

    @Override
    public void compile ( HtClause clause, HtProperty... flags ) throws SourceCodeException {
        preCompiler.compile(clause, flags);
    }

//    @Override
//    public void compileQuery ( HtClause query ) throws SourceCodeException {
//
//    }

    /**
     * @param tokenSource
     * @param flags
     * @throws IOException
     * @throws SourceCodeException
     */
    @Override
    public void compile ( PlTokenSource tokenSource, HtProperty... flags ) throws IOException, SourceCodeException {
        preCompiler.compile(tokenSource, flags);
    }

    public void compileQuery ( Q query ) throws SourceCodeException {

    }

    public void setResolver ( Resolver <P, Q> resolver ) {

    }

    public void compile ( T clause ) {

    }

    /**
     * Chains compilation completion events onto the instruction compiler.
     */
    public class ClauseChainObserver implements LogicCompilerObserver <HtClause, HtClause> {
        /**
         * {@inheritDoc}
         */
        public void onCompilation ( Sentence <HtClause> sentence ) throws SourceCodeException {
            PrologWAMCompiler.this.instructionCompiler.compile(sentence);
        }

        /**
         * {@inheritDoc}
         */
        public void onQueryCompilation ( Sentence <HtClause> sentence ) throws SourceCodeException {
            PrologWAMCompiler.this.instructionCompiler.compileQuery(sentence);
        }
    }
}
