package org.ltc.hitalk.wam.compiler.prolog;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.compiler.BaseCompiler;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.BaseApp;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.core.IHitalkObject;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.ParseException;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.parser.PlTokenSource;
import org.ltc.hitalk.wam.compiler.CompilerFactory;
import org.ltc.hitalk.wam.compiler.ICompilerFactory;

import java.io.IOException;

import static org.ltc.hitalk.wam.compiler.Language.PROLOG;

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
@SuppressWarnings("rawtypes")
public class PrologWAMCompiler<T extends HtClause, P, Q, PC, QC>
        extends BaseCompiler <T, P, Q> implements IHitalkObject {

    public void setPreCompiler ( PrologPreCompiler <T, P, Q> preCompiler ) {
        this.preCompiler = preCompiler;
    }

    public void setInstructionCompiler ( PrologInstructionCompiler <T, PC, QC> instructionCompiler ) {
        this.instructionCompiler = (ICompiler <T, HtPredicate, HtClause>) instructionCompiler;
//        this.observer2 = new ChainedCompilerObserver();
    }

    protected ICompiler <T, P, Q> preCompiler;
    //    protected LogicCompilerObserver <P, Q> observer1;
//    protected LogicCompilerObserver <PC, QC> observer2;
    protected ICompiler <T, HtPredicate, HtClause> instructionCompiler;

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     */
    public PrologWAMCompiler ( ISymbolTable <Integer, String, Object> symbolTable,
                               IVafInterner interner,
                               PlPrologParser parser,
                               //  PrologPreCompiler <T, P, Q> preCompiler,
                               //  PrologInstructionCompiler <T, PC, QC> instructionCompiler,
                               LogicCompilerObserver <P, Q> observer ) {
        super(symbolTable, interner, parser, observer);

        ICompilerFactory <T, P, Q, HtPredicate, HtClause> cf = new CompilerFactory <>();
//        final PrologDefaultBuiltIn defaultBuiltIn = new PrologDefaultBuiltIn(symbolTable, interner);
        this.preCompiler = cf.createPreCompiler(PROLOG);
        this.instructionCompiler = cf.createInstrCompiler(PROLOG);
        this.preCompiler.setCompilerObserver(new PrologWAMCompiler.ClauseChainObserver());

    }

    public PrologWAMCompiler () {
        final BaseApp.AppContext appCtx = BaseApp.getAppContext();

        ICompilerFactory <T, P, Q, HtPredicate, HtClause> cf = new CompilerFactory <>();
        appCtx.setCompilerFactory(cf);
        this.preCompiler = cf.createPreCompiler(PROLOG);
        this.instructionCompiler = cf.createInstrCompiler(PROLOG);
        this.preCompiler.setCompilerObserver(new PrologWAMCompiler.ClauseChainObserver());
    }

    @Override
    public void endScope () throws SourceCodeException {
        preCompiler.endScope();
        instructionCompiler.endScope();
    }

    @Override
    public void compile ( T clause, HtProperty... flags ) throws SourceCodeException {
        preCompiler.compile(clause, flags);
    }

    /**
     * @param tokenSource
     * @param flags
     * @throws IOException
     * @throws SourceCodeException
     */
    @Override
    public void compile ( PlTokenSource tokenSource, HtProperty... flags ) throws IOException, SourceCodeException, ParseException {
        getPreCompiler().compile(tokenSource, flags);
    }

    private ICompiler <T, P, Q> getPreCompiler () {
        return preCompiler;
    }

    public void compileQuery ( Q query ) {

    }

    /**
     * @param resolver
     */
    public void setResolver ( IResolver <P, Q> resolver ) {

    }

    public void compile ( T clause ) {

    }

    /**
     * Chains compilation completion events onto the instruction compiler.
     */
    public class ClauseChainObserver implements LogicCompilerObserver <T, Q> {
        /**
         * {@inheritDoc}
         */
        public void onCompilation ( Sentence <T> sentence ) throws SourceCodeException {
            PrologWAMCompiler.this.instructionCompiler.compile(sentence);
        }

        /**
         * {@inheritDoc}
         */
        public void onQueryCompilation ( Sentence <Q> sentence ) throws SourceCodeException {
            PrologWAMCompiler.this.instructionCompiler.compileQuery((HtClause) sentence);
        }
    }
}
