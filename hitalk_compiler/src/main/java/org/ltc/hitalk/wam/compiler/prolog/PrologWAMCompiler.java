package org.ltc.hitalk.wam.compiler.prolog;

import org.ltc.hitalk.compiler.BaseCompiler;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.IHitalkObject;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.parser.HtSourceCodeException;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.wam.compiler.BaseInstructionCompiler;
import org.ltc.hitalk.wam.compiler.CompilerFactory;
import org.ltc.hitalk.wam.compiler.ICompilerFactory;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.util.EnumSet.of;
import static org.ltc.hitalk.core.BaseApp.AppContext;
import static org.ltc.hitalk.core.BaseApp.getAppContext;
import static org.ltc.hitalk.parser.Directive.DirectiveKind.DK_IF;
import static org.ltc.hitalk.parser.PlLexer.getTokenSourceForIoFileName;
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
public class PrologWAMCompiler<T extends HtClause, P, Q, PC extends HiTalkWAMCompiledPredicate, QC extends HiTalkWAMCompiledQuery>
        extends BaseCompiler<T, P, Q, PC, QC> implements IHitalkObject {

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     */
    public PrologWAMCompiler(ISymbolTable<Integer, String, Object> symbolTable,
                             IVafInterner interner,
                             HtPrologParser parser,
                             ICompilerObserver<P, Q> observer) {
        super(symbolTable, interner, parser, observer);

        ICompilerFactory<T, P, Q, PC, QC> cf = new CompilerFactory<>();
//        final PrologDefaultBuiltIn defaultBuiltIn = new PrologDefaultBuiltIn(symbolTable, interner);
        this.preCompiler = cf.createPreCompiler(PROLOG);
        setInstructionCompiler(cf.createInstrCompiler(PROLOG));
        this.preCompiler.setCompilerObserver(new ClauseChainObserver());

    }

    /**
     * @param preCompiler
     */
    public void setPreCompiler(IPreCompiler preCompiler) {
        this.preCompiler = preCompiler;
    }

    /**
     * @param instructionCompiler
     */
    public void setInstructionCompiler(BaseInstructionCompiler<T, P, Q, PC, QC> instructionCompiler) {
        this.instructionCompiler = instructionCompiler;
    }

    protected IPreCompiler preCompiler;
    protected BaseInstructionCompiler<T, P, Q, PC, QC> instructionCompiler;

    /**
     *
     */
    public PrologWAMCompiler() {
        final AppContext appCtx = getAppContext();

        ICompilerFactory<T, P, Q, PC, QC> cf = new CompilerFactory<>();
        appCtx.setCompilerFactory(cf);
        this.preCompiler = cf.createPreCompiler(PROLOG);
        setInstructionCompiler(cf.createInstrCompiler(PROLOG));
        this.preCompiler.setCompilerObserver(new ClauseChainObserver());
    }


    /**
     * @throws HtSourceCodeException
     */
    @Override
    public void endScope() throws Exception {
        preCompiler.endScope();
        instructionCompiler.endScope();
    }

    /**
     * @param clause
     * @param flags
     * @throws HtSourceCodeException
     */
    @Override
    public void compile(T clause, HtProperty... flags) throws HtSourceCodeException {
//        instructionCompiler.compile(clause, flags);
    }

    /**
     * @param fnl
     * @return
     * @throws IOException
     * @throws HtSourceCodeException
     */
    public List<T> compileFiles(List<String> fnl) throws Exception {
        return compileFiles(fnl, EMPTY_FLAG_ARRAY);
    }

    public List<T> compileFiles(List<String> fnl, HtProperty... flags) throws Exception {
        List<T> list = new ArrayList<>();
        for (String fn : fnl) {
            list = compileFile(fn, flags);
        }
        return list;
    }

    /**
     * @param fn
     * @param flags
     * @throws IOException
     */
    @Override
    public List<T> compileFile(String fn, HtProperty... flags) throws Exception {
        return compile(getTokenSourceForIoFileName(fn));
    }

    /**
     * @param tokenSource
     * @param flags
     * @throws IOException
     * @throws HtSourceCodeException
     */
    @Override
    public List<T> compile(PlLexer tokenSource, HtProperty... flags) throws Exception {
        return preCompiler.preCompile(tokenSource, of(DK_IF));
    }

    /**
     * Establishes an observer on the compiled forms that the compiler outputs.
     *
     * @param observer The compiler output observer.
     */
    public void setCompilerObserver(ICompilerObserver<P, Q> observer) {
        instructionCompiler.setCompilerObserver(observer);
    }

    /**
     * @return
     */
    public IPreCompiler getPreCompiler() {
        return preCompiler;
    }

    public void compileQuery(Q query) {

    }

    /**
     * @param resolver
     */
    public void setResolver(IResolver<PC, QC> resolver) {

    }

    /**
     * @param clause
     * @throws HtSourceCodeException
     */
    @Override
    public void compile(T clause) throws HtSourceCodeException {
        logger.info("Compiling clause ...");
    }

    public void toString0(StringBuilder sb) {

    }

    public BaseInstructionCompiler<T, P, Q, PC, QC> getInstructionCompiler() {
        return instructionCompiler;
    }

    /**
     * Chains compilation completion events onto the instruction compiler.
     */
    public class ClauseChainObserver implements ICompilerObserver<T, Q> {
        /**
         * {@inheritDoc}
         */
        public void onCompilation(T clause) throws Exception {
            PrologWAMCompiler.this.instructionCompiler.compile(clause);
        }

        /**
         * {@inheritDoc}
         */
        public void onQueryCompilation(Q clause) throws Exception {
            PrologWAMCompiler.this.instructionCompiler.compileQuery((HtClause) clause);
        }
    }
}
