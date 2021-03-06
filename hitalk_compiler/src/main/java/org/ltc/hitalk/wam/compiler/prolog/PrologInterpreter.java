package org.ltc.hitalk.wam.compiler.prolog;

import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.wam.compiler.WAMCallPoint;
import com.thesett.aima.logic.fol.wam.machine.WAMInternalRegisters;
import com.thesett.aima.logic.fol.wam.machine.WAMMemoryLayout;
import com.thesett.aima.logic.fol.wam.machine.WAMResolvingMachineDPIMonitor;
import jline.ConsoleReader;
import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.interpreter.HtResolutionEngine;
import org.ltc.hitalk.interpreter.IInterpreter;
import org.ltc.hitalk.interpreter.Mode;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.parser.HtSourceCodeException;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.parser.PlToken.TokenKind;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.Language;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledQuery;
import org.ltc.hitalk.wam.machine.HiTalkWAMResolvingMachine;
import org.ltc.hitalk.wam.task.PreCompilerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Deque;
import java.util.Iterator;
import java.util.Set;

/**
 * @param <T>
 * @param <P>
 * @param <Q>
 */
public
class PrologInterpreter<T extends HtClause, P, Q,
        PC extends HiTalkWAMCompiledPredicate,
        QC extends HiTalkWAMCompiledQuery>

        extends HtResolutionEngine<T, PreCompilerTask<T>, P, Q, PC, QC> implements IInterpreter<T, P, Q, PC, QC> {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    /**
     * Creates a prolog parser using the specified interner.
     *
     * @param symbolTable
     * @param termFactory
     * @param compiler
     * @param resolver
     * @param parser
     * @param preCompiler
     */
    public PrologInterpreter(ISymbolTable<Integer, String, Object> symbolTable,
                             ITermFactory termFactory,
                             ICompiler<T, P, Q, PC, QC> compiler,
                             IResolver<PC, QC> resolver,
                             HtPrologParser parser,
                             IPreCompiler<T, PreCompilerTask<T>, P, Q, PC, QC> preCompiler) {
        super(symbolTable, termFactory, compiler, resolver, parser, preCompiler);
        engine = new HtResolutionEngine<>(symbolTable, termFactory, compiler, resolver, parser, preCompiler);
    }

    private final static String meta =
            "solve((G1,G2)):-\n" +
                    "    !, solve(G1),\n" +
                    "       solve(G2).\n" +

                    "solve(G):-\n" +
                    "    clause(G,Body),\n" +
                    "    solveBody).\n" +

                    "solve(G):-\n" +
                    "    predicate_property(G, built_in),\n" +
                    "    call(G).\n";

    protected HtPrologParser parser;
    private ConsoleReader reader;

    public String getMeta() {
        return meta;
    }

    //    protected IConfig config;
    protected ICompilerObserver<P, Q> observer;
    protected Mode mode;

    public HiTalkWAMResolvingMachine<PC, QC> engine;

    protected int currentPredicateName;

//    /**
//     * @param compiler
//     * @param parser
//     */
//    public PrologInterpreter(ISymbolTable<Integer, String, Object> symbolTable,
//                             ICompiler<T, P, Q> compiler,
//                             IResolver<P, Q> resolver,
//                             HtPrologParser parser,
//                             IPreCompiler preCompiler) {
//        this(symbolTable, parser, compiler, resolver, parser);
//        engine = new HtResolutionEngine(symbolTable, getFactory(), compiler, resolver, parser, getInterner(), preCompiler);
//    }

//    /**
//     * @return
//     */
//    @Override
//    public Mode getMode() {
//        return mode;
//    }

    /**
     * @return
     */
    public Logger getConsole() {
        return logger;
    }

    @Override
    public Deque<PlLexer> getTokenSourceStack() {
        return parser.getTokenSourceStack();
    }

    /**
     * @return
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * @return
     */
    @Override
    public HtPrologParser getParser() {
        return parser;
    }

    @Override
    public IVafInterner getInterner() {
        return parser.getInterner();
    }

    @Override
    public void setInterner(IVafInterner interner) {
        parser.setInterner(interner);
    }

    @Override
    public ITermFactory getFactory() {
        return null;
    }

    public IOperatorTable getOptable() {
        return null;
    }

    @Override
    public void setOptable(IOperatorTable optable) {

    }

    /**
     * @return
     */
    @Override
    public Language language() {
        return parser.language();
    }

    /**
     * @return
     */
    public ITerm parse() throws IOException {
        return next();
    }

    /**
     * @return
     */
    public PlLexer getTokenSource() {
        return parser.getTokenSource();
    }

    @Override
    public void initializeBuiltIns() {
        parser.initializeBuiltIns();
    }

    /**
     * @param rdelim
     * @return
     * @throws IOException
     */
    public ITerm expr(TokenKind rdelim) throws Exception {
        return parser.expr(rdelim);
    }

    /**
     * @return
     * @throws IOException
     */
    public ITerm next() throws IOException {
        return null;
    }

//    @Override
//    @SuppressWarnings("unchecked")
//    public T parseClause() throws Exception {
//        return (T) parser.parseClause();
//    }

//    /**
//     * @param clause
//     * @throws HtSourceCodeException
//     */
//    @Override
//    public void evaluate ( T clause ) throws Exception {
//        if (clause.isQuery()) {
//            engine.endScope();
//            engine.compile(clause);
//        } else {
//            // Check if the program clause is new, or a continuation of the current predicate.
//            int name = clause.getHead().getName();
//
//            if (/*currentPredicateName == null ||*/ currentPredicateName != name) {
//                engine.endScope();
//                currentPredicateName = name;
//            }
//
//            addProgramClause(clause);
//        }
//    }

    /**
     * @param clause
     */
    protected void addProgramClause(HtClause clause) {
//        final PredicateTable predicateTable = getPredicateTable();
        /*final HtPredicateDefinition def = */
//        predicateTable.lookup(clause);
    }

    /**
     * @return
     */
//    @Override
    public ConsoleReader getConsoleReader() {
        return reader;
    }

    /**
     * @param reader
     */
//    @Override
    public void setConsoleReader(ConsoleReader reader) {
        this.reader = reader;
    }

    /**
     * @return
     */
//    @Override
    public String getQueryPrompt() {
        return "?- ";
    }

    /**
     * @return
     */
//    @Override
    public ConsoleReader initializeCommandLineReader() {
        return reader;
    }

    /**
     * Adds the specified construction to the domain of resolution searched by this resolver.
     *
     * @param term The term to add to the domain.
     * @throws LinkageException If the term to add to the domain, cannot be added to it, because it depends on the
     *                          existance of other clauses which are not in the domain. Implementations may elect to
     *                          raise this as an error at the time the clauses are added to the domain, or during
     *                          resolution, or simply to fail to find a resolution.
     */
    /**
     * Sets the query to resolve.
     *
     * @param query The query to resolve.
     * @throws LinkageException If the query to add run over the domain, cannot be applied to it, because it depends on
     *                          the existance of clauses which are not in the domain. Implementations may elect to raise
     *                          this as an error at the time the query is created, or during resolution, or simply to
     *                          fail to find a resolution.
     */

    /**
     * Resolves a query over a logical domain, or knowledge base and a query. The domain and query to resolve over must
     * be established by prior to invoking this method. There may be more than one set of bindings that make the query
     * provable over the domain, in which case subsequent calls to this method will return successive bindings until no
     * more can be found. If no proof can be found, this method will return <tt>null</tt>.
     *
     * @return A list of variable bindings, if the query can be satisfied, or <tt>null</tt> otherwise.
     */
    @Override
    public Set<HtVariable> resolve() {
        return engine.resolve();
    }

    /**
     * Notified whenever code is added to the machine.
     *
     * @param codeBuffer The code buffer.
     * @param codeOffset The start offset of the new code.
     * @param length     The length of the new code.
     */
    protected void codeAdded(ByteBuffer codeBuffer, int codeOffset, int length) {

    }

    /**
     * Dereferences an offset from the current BaseApp frame on the stack. Storage slots in the current BaseApp
     * may point to other BaseApp frames, but should not contain unbound variables, so ultimately this dereferencing
     * should resolve onto a structure or variable on the heap.
     *
     * @param a The offset into the current BaseApp stack frame to dereference.
     * @return The dereferences structure or variable.
     */
    protected int derefStack(int a) {
        return 0;
    }

    /**
     * Executes compiled code at the specified call point returning an indication of whether or not the execution was
     * successful.
     *
     * @param callPoint The call point of the compiled byte code to execute.
     * @return <tt>true</tt> iff execution succeeded.
     */
    public boolean execute(WAMCallPoint callPoint) {
        return false;
    }

    /**
     * Dereferences a heap pointer (or register), returning the address that it refers to after following all reference
     * chains to their conclusion. This method is also side effecting, in that the contents of the refered to heap cell
     * are also loaded into fields and made available through the {@link #getDerefTag()} and {@link #getDerefVal()}
     * methods.
     *
     * @param a The address to dereference.
     * @return The address that the reference refers to.
     */
    protected int deref(int a) {
        return 0;
    }

    /**
     * Gets the heap cell tag for the most recent dereference operation.
     *
     * @return The heap cell tag for the most recent dereference operation.
     */
    protected byte getDerefTag() {
        return 0;
    }

    /**
     * Gets the heap call value for the most recent dereference operation.
     *
     * @return The heap call value for the most recent dereference operation.
     */
    protected int getDerefVal() {
        return 0;
    }

    /**
     * Gets the value of the heap cell at the specified location.
     *
     * @param addr The address to fetch from the heap.
     * @return The heap cell at the specified location.
     */
    protected int getHeap(int addr) {
        return 0;
    }

    /**
     * Resets the resolver. This should clear any start and goal states, and leave the resolver in a state in which it
     * is ready to be run.
     */
    @Override
    public void reset() {

    }

    /**
     * Provides an iterator that generates all solutions on demand as a sequence of variable bindings.
     *
     * @return An iterator that generates all solutions on demand as a sequence of variable bindings.
     */
    @Override
    public Iterator<Set<HtVariable>> iterator() {
        return null;
    }//todo

    /**
     * Establishes an observer on the compiled forms that the compiler outputs.
     *
     * @param observer The compiler output observer.
     */
    public void setCompilerObserver(ChainedCompilerObserver<P, Q> observer) {
        this.observer = observer;
    }

    /**
     * Signal the end of a compilation scope, to trigger completion of the compilation of its contents.
     *
     * @throws HtSourceCodeException If there is an error in the source to be compiled that prevents its compilation.
     */
    public void endScope() throws HtSourceCodeException {

    }
//
//    /**
//     * @return
//     */
//    @Override
//    public IConfig getConfig() {
//        return config;
//    }

//    /**
//     * @param config
//     */
//    @Override
//    public void setConfig(IConfig config) {
//        this.config = config;
//    }

    public void toString0(StringBuilder sb) {

    }

    /**
     * Attaches a monitor to the abstract machine.
     *
     * @param monitor The machine monitor.
     */
    public void attachMonitor(WAMResolvingMachineDPIMonitor monitor) {

    }

    /**
     * Provides read access to the the machines data area.
     *
     * @return The requested portion of the machines data area.
     */
    public IntBuffer getDataBuffer() {
        return null;
    }

    /**
     * Provides the internal register file and flags for the machine.
     *
     * @return The internal register file and flags for the machine.
     */
    public WAMInternalRegisters getInternalRegisters() {
        return null;
    }

    /**
     * Provides the internal register set describing the memory layout of the machine.
     *
     * @return The internal register set describing the memory layout of the machine.
     */
    public WAMMemoryLayout getMemoryLayout() {
        return null;
    }

}
