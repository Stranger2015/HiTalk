/*
 * Copyright The Sett Ltd, 2005 to 2014.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ltc.hitalk.interpreter;

import com.thesett.aima.attribute.impl.IdAttribute.IdAttributeFactory;
import com.thesett.aima.logic.fol.Parser;
import com.thesett.aima.logic.fol.wam.compiler.WAMCallPoint;
import com.thesett.aima.logic.fol.wam.machine.WAMInternalRegisters;
import com.thesett.aima.logic.fol.wam.machine.WAMMemoryLayout;
import com.thesett.aima.logic.fol.wam.machine.WAMResolvingMachineDPIMonitor;
import com.thesett.common.util.Filterator;
import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.*;
import org.ltc.hitalk.parser.PlToken.TokenKind;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.OpSymbolFunctor.Associativity;
import org.ltc.hitalk.term.io.HtTermReader;
import org.ltc.hitalk.wam.compiler.HtFunctorName;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.ltc.hitalk.wam.compiler.Language;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledQuery;
import org.ltc.hitalk.wam.compiler.prolog.ICompilerObserver;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler.ClauseChainObserver;
import org.ltc.hitalk.wam.machine.HiTalkWAMResolvingMachine;
import org.ltc.hitalk.wam.task.PreCompilerTask;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.stream.Collectors;

import static org.ltc.hitalk.core.BaseApp.appContext;

/**
 * ResolutionEngine combines together a logic {@link Parser}, a {@link IVafInterner} that acts as a symbol
 * table, a {@link ICompiler} and a {@link IResolver}, into a single unit.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * </table></pre>
 *
 * @author Rupert Smith
 */
public
class HtResolutionEngine<T extends HtClause, TT extends PreCompilerTask<T>, P, Q,
        PC extends HiTalkWAMCompiledPredicate,
        QC extends HiTalkWAMCompiledQuery>

        extends HiTalkWAMResolvingMachine<PC, QC>
        implements
        IVafInterner,
        ICompiler<T, P, Q, PC, QC>,
        IResolver<PC, QC>,
        IParser<T>,
        IPreCompiler<T, TT, P, Q, PC, QC> {

    /**
     * Holds the parser.
     */
    protected HtPrologParser parser;
    protected HtTermReader termReader;
    protected final IPreCompiler<T, TT, P, Q, PC, QC> preCompiler;

    /**
     * Holds the variable and functor symbol table.
     */
    protected IVafInterner interner;

    private final ISymbolTable<Integer, String, Object> symbolTable;
    /**
     * Holds the compiler.
     */
    protected ICompiler<T, P, Q, PC, QC> compiler;

    /**
     * Holds the observer for compiler outputs.
     */
    protected ICompilerObserver<P, Q> observer = new ChainedCompilerObserver<>();

    protected Q currentQuery;
    protected final List<Set<HtVariable>> vars = new ArrayList<>();
    protected IResolver<PC, QC> resolver;
    protected ITermFactory termFactory;

    /**
     * Creates a prolog parser using the specified interner.
     *
     * @param parser
     */
    public HtResolutionEngine(ISymbolTable<Integer, String, Object> symbolTable,
                              ITermFactory termFactory,
                              ICompiler<T, P, Q, PC, QC> compiler,
                              IResolver<PC, QC> resolver,
                              HtPrologParser parser,
                              IPreCompiler<T, TT, P, Q, PC, QC> preCompiler) {
        super(symbolTable);
        this.symbolTable = symbolTable;
        this.termFactory = termFactory;
        this.compiler = compiler;
        this.resolver = resolver;
        this.parser = parser;
        this.preCompiler = preCompiler;
    }

    @SuppressWarnings("unchecked")
    public HtResolutionEngine() throws Exception {
        super(appContext.getSymbolTable());
        symbolTable = appContext.getSymbolTable();

        termFactory = appContext.getTermFactory();
        compiler = (ICompiler<T, P, Q, PC, QC>) appContext.getApp().getLanguage().getWamCompilerClass().newInstance();
        resolver = (IResolver<PC, QC>) appContext.getResolverIC();
        parser = appContext.getParser();
        preCompiler = (IPreCompiler<T, TT, P, Q, PC, QC>) appContext.getApp().getLanguage().getPreCompilerClass().newInstance();
    }

    /**
     * Resets the engine to its default state. This will typically load any
     * bootstrapping libraries of built-ins that
     * the engine requires, but otherwise set its domain to empty.
     */
    public void reset() throws Exception {
        super.reset();
    }

    /**
     * Provides the resolution engines interner.
     *
     * @return The resolution engines interner.
     */
    @Override
    public IVafInterner getInterner() {
        return interner;
    }

    /**
     * @param clauseChainObserver
     */
    @Override
    public void setCompilerObserver(ClauseChainObserver clauseChainObserver) {
        preCompiler.setCompilerObserver(clauseChainObserver);
    }

    /**
     * @param term
     * @return
     */
    public void checkBOF(ITerm term) throws IOException {
        preCompiler.checkBOF(term);
    }

    /**
     * @param term
     * @return
     */
    public void checkEOF(ITerm term) throws IOException {
        preCompiler.checkEOF(term);
    }

    /**
     * @param interner
     */
    public void setInterner(IVafInterner interner) {
        this.interner = interner;
    }

    /**
     * @return
     */
    public ITermFactory getFactory() {
        return termFactory;
    }

    /**
     * @return
     */
    public IOperatorTable getOptable() {
        return parser.getOptable();
    }

    /**
     * @param optable
     */
    public void setOptable(IOperatorTable optable) {
        parser.setOptable(optable);
    }

    /**
     * @return
     */
    public Language language() {
        return parser.language();
    }

    /**
     * @return
     * @throws HtSourceCodeException
     */
    public ITerm parse() throws Exception {
        return parser.parse();
    }

    /**
     *
     */
    public void initializeBuiltIns() {
        parser.initializeBuiltIns();
    }

    /**
     * @param rdelim
     * @return
     */
    public ITerm expr(TokenKind rdelim) throws Exception {
        return parser.expr(rdelim);
    }

//    /**
//     * @return
//     */
//    @SuppressWarnings("unchecked")
//    public T parseClause() throws Exception {
//        return (T) parser.parseClause();
//    }

    /**
     * Provides the resolution engines compiler.
     *
     * @return The resolution engines compiler.
     */
    public ICompiler<T, P, Q, PC, QC> getCompiler() {
        return compiler;
    }

    /**
     * Consults an input stream, reading first order logic clauses from it, and inserting them into the resolvers
     * knowledge base.
     *
     * @param stream The input stream to consult.
     * @throws HtSourceCodeException If any code read from the input stream fails to parse, compile or link.
     */
//    public void consultInputStream(HiTalkInputStream stream) throws Exception {
//         Create a token source to read from the specified input stream.
//        PlLexer tokenSource = stream.getTokenSource();
//        getParser().setTokenSource(tokenSource);
//         Consult the type checking rules and add them to the knowledge base.
//        while (tokenSource.isOpen()) {
//            ITerm term = termReader.readTerm();
//            if (term == BEGIN_OF_FILE) {
//                preCompiler.checkBOF(term);
//            }
//            if (term == END_OF_FILE) {
//                preCompiler.checkEOF(term);
//                parser.popTokenSource();
//                    break;//
//            }
//            final List<ITerm> l = preprocess(term);
    //todo
//        }
//    }

//    private List<ITerm> preprocess(ITerm clause) throws IOException {
//        final List<ITerm> clauses = new ArrayList<>();
//        clauses.add(clause);
//        final TermExpansionTask task = new TermExpansionTask(
//                preCompiler,
//                getTokenSource(),///fixme
//                EnumSet.of(DK_ELSE, DK_ELIF, DK_ENDIF));
//        clauses.addAll(task.invoke(clause));
//        return clauses;//getTaskQueue().peek().invoke(clause);
//    }

//    private ITerm readTerm() throws Exception {
//        new HtTermReader(IPreCompiler.) ;
//        return
//    }

    /**
     * Prints all of the logic variables in the results of a query.
     *
     * @param solution An iterable over the variables in the solution.
     * @return All the variables printed as a string, one per line.
     */
    public String printSolution(Iterable<HtVariable> solution) {
        StringBuilder result = new StringBuilder();

        for (HtVariable var : solution) {
            result.append(printVariableBinding(var)).append("\n");
        }

        return result.toString();
    }

    /**
     * Prints all of the logic variables in the results of a query.
     *
     * @param variables An iterable over the variables in the solution.
     * @return All the variables printed as a string, one per line.
     */
    public String printSolution(Map<String, HtVariable> variables) {

        return variables.values().stream().map(variable ->
                printVariableBinding(variable) + "\n").collect(Collectors.joining());
    }

    /**
     * Prints a variable binding in the form 'Var = value'.
     *
     * @param var The variable to print.
     * @return The variable binding in the form 'Var = value'.
     */
    public String printVariableBinding(ITerm var) {
        return var.toString(getInterner(), true, false) + " = " + var.getValue().toString(getInterner(), false, true);
    }

    /**
     * Transforms an iterator over sets of variable bindings, resulting from a query, to an iterator over a map from the
     * string name of variables to their bindings, for the same sequence of query solutions.
     *
     * @param solutions The resolution solutions to convert to map form.
     * @return An iterator over a map from the string name of variables to their bindings, for the solutions.
     */
    public Iterable<Map<String, HtVariable>> expandResultSetToMap(Iterator<Set<HtVariable>> solutions) {
        return new Filterator<>(solutions, this::apply);
    }

    /**
     * {@inheritDoc}
     */
    public IdAttributeFactory<String> getVariableInterner() {
        return interner.getVariableInterner();
    }

    /**
     * {@inheritDoc}
     */
    public IdAttributeFactory<HtFunctorName> getFunctorInterner() {
        return interner.getFunctorInterner();
    }

    /**
     * {@inheritDoc}
     */
    public int internFunctorName(String name, int numArgs) {
        return interner.internFunctorName(name, numArgs);
    }

    /**
     * {@inheritDoc}
     */
    public int internFunctorName(HtFunctorName name) {
        return interner.internFunctorName(name);
    }

    /**
     * {@inheritDoc}
     */
    public int internVariableName(String name) {
        return interner.internVariableName(name);
    }

    /**
     * {@inheritDoc}
     */
    public String getVariableName(int name) {
        return interner.getVariableName(name);
    }

    /**
     * {@inheritDoc}
     */
    public String getVariableName(HtVariable variable) {
        return interner.getVariableName(variable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HtFunctorName getDeinternedFunctorName(int name) {
        return interner.getDeinternedFunctorName(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFunctorName(int name) {
        return interner.getFunctorName(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getFunctorArity(int name) {
        return interner.getFunctorArity(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HtFunctorName getFunctorFunctorName(IFunctor functor) throws Exception {
        return interner.getFunctorFunctorName(functor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFunctorName(IFunctor functor) throws Exception {
        return interner.getFunctorName(functor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getFunctorArity(IFunctor functor) throws Exception {
        return interner.getFunctorArity(functor);
    }

    /**
     * {@inheritDoc}
     */
    public void setOperator(String operatorName, int priority, Associativity associativity) {
        parser.setOperator(operatorName, priority, associativity);
    }

    /**
     * {@inheritDoc}
     */
    public Set<HtVariable> resolve() {
        // Check that a query has been set to resolve.
        if (currentQuery == null) {
            throw new IllegalStateException("No query set to resolve.");
        }

        // Execute the byte code, starting from the first functor of the query.
        return executeAndExtractBindings(currentQuery);
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

    private Set<HtVariable> executeAndExtractBindings(Q query) {
        return Collections.emptySet();
    }//todo

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Set<HtVariable>> iterator() {
        return vars.iterator();
    }

    public void compile(T sentence) throws Exception {
        compiler.compile(sentence);
    }

    public void setCompilerObserver(ICompilerObserver<P, Q> observer) {
        compiler.setCompilerObserver(observer);
    }

    /**
     * {@inheritDoc}
     */
    public void endScope() throws Exception {
        compiler.endScope();
    }

    private Map<String, HtVariable> apply(Set<HtVariable> variables) {
        Map<String, HtVariable> results = new HashMap<>();

        for (HtVariable var : variables) {
            String varName = getInterner().getVariableName(var.getName());
            results.put(varName, var);
        }

        return results;
    }

    @Override
    public List<T> compile(PlLexer tokenSource, HtProperty... flags) throws Exception {
        return compiler.compile(tokenSource, flags);
    }

    /**
     * @return
     */
    @Override
    public Logger getConsole() {
        return logger;
    }

    /**
     * @return
     */
    public Deque<PlLexer> getTokenSourceStack() {
        return parser.getTokenSourceStack();
    }

    /**
     * @return
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * @param tokenSource
     * @param delims
     * @return
     */
    public List<T> preCompile(PlLexer tokenSource, EnumSet<DirectiveKind> delims) throws Exception {
        return preCompiler.preCompile(tokenSource, delims);
    }

    /**
     * @return
     */
    public HtPrologParser getParser() {
        return parser;
    }

    public boolean isDirective(T clause) throws Exception {
        return preCompiler.isDirective(clause);
    }

    /**
     * @param clause
     * @param flags
     * @throws HtSourceCodeException
     */
    @Override
    public void compile(T clause, HtProperty... flags) throws HtSourceCodeException {
        compiler.compile(clause, flags);
    }

    /**
     * @param rule
     */
    @Override
    public void compileDcgRule(DcgRule rule) throws HtSourceCodeException {//fixme
        compiler.compileDcgRule(rule);
    }

    /**
     * @param query
     */
    @Override
    public void compileQuery(Q query) throws HtSourceCodeException {
        compiler.compileQuery(query);
    }

    @Override
    public void setResolver(IResolver<PC, QC> resolver) {
        this.resolver = resolver;
    }

    @Override
    public List<T> compile(String fileName, HtProperty... flags) throws Exception {
        return compiler.compile(fileName, flags);
    }

    /**
     * Attaches a monitor to the abstract machine.
     *
     * @param monitor The machine monitor.
     */
    public void attachMonitor(WAMResolvingMachineDPIMonitor monitor) {

    }

    /*
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

    /**
     * @return
     */
    public Deque<TT> getTaskQueue() {
        return null;
    }

    /**
     * ChainedCompilerObserver implements the compiler observer for this resolution engine. Compiled programs are added
     * to the resolvers domain. Compiled queries are executed.
     * <p>
     * <p/>If a chained observer is set up, all compiler outputs are forwarded onto it.
     */
    class ChainedCompilerObserver<P, Q> implements ICompilerObserver<P, Q> {

        /**
         * Accepts notification of the completion of the compilation of a sentence into a (binary) form.
         *
         * @param sentence The compiled form of the sentence.
         * @throws HtSourceCodeException If there is an error in the compiled code that prevents its further processing.
         */
        @Override
        public void onCompilation(P sentence) throws HtSourceCodeException {
            //todo
        }

        /**
         * {@inheritDoc}
         *
         * @param sentence
         */
        public void onQueryCompilation(Q sentence) throws HtSourceCodeException {
//todo
        }
    }
}