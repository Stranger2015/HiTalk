package org.ltc.hitalk.wam.compiler.prolog;

import com.thesett.aima.search.util.backtracking.DepthFirstBacktrackingSearch;
import org.ltc.hitalk.compiler.AbstractBaseMachine;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.IHitalkObject;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.io.HiTalkInputStream;
import org.ltc.hitalk.term.io.HtTermWriter;
import org.ltc.hitalk.wam.compiler.HtPositionAndOccurrenceVisitor;
import org.ltc.hitalk.wam.compiler.HtTermWalkers;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.ltc.hitalk.wam.compiler.hitalk.HtSymbolKeyTraverser;
import org.ltc.hitalk.wam.compiler.hitalk.HtTermWalker;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler.ClauseChainObserver;
import org.ltc.hitalk.wam.printer.HtBasePositionalVisitor;
import org.ltc.hitalk.wam.task.PreCompilerTask;
import org.ltc.hitalk.wam.task.StandardPreprocessingTask;
import org.ltc.hitalk.wam.task.TermExpansionTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

import static org.ltc.hitalk.core.BaseApp.appContext;
import static org.ltc.hitalk.core.BaseApp.getAppContext;
import static org.ltc.hitalk.parser.Directive.DirectiveKind.DK_IF;
import static org.ltc.hitalk.parser.HtPrologParser.BEGIN_OF_FILE;
import static org.ltc.hitalk.parser.HtPrologParser.END_OF_FILE;
import static org.ltc.hitalk.parser.PrologAtoms.IMPLIES;

/**
 *
 */
public
class PrologPreCompiler<T extends HtClause, TT extends PreCompilerTask<T>, P, Q, PC, QC> extends AbstractBaseMachine
        implements IPreCompiler<T, TT, P, Q, PC, QC>, IHitalkObject {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    final protected HtPrologParser parser;
    final protected PrologDefaultBuiltIn defaultBuiltIn;

    /**
     * Holds the built in transformation.
     */
    protected final PrologBuiltInTransform<T, P, Q, PC, QC> builtInTransform;
    protected final IResolver<PC, QC> resolver;
    protected ICompilerObserver<P, Q> observer;

    protected ClauseChainObserver clauseChainObserver;
    protected final Deque<TT> taskQueue = new ArrayDeque<TT>();

    protected boolean isBOFPassed;
    protected boolean isEOFPassed;

    /**
     * @param symbolTable
     * @param interner
     * @param builtInTransform
     * @param resolver
     */
    public PrologPreCompiler(ISymbolTable<Integer, String, Object> symbolTable,
                             IVafInterner interner,
                             PrologDefaultBuiltIn defaultBuiltIn,
                             PrologBuiltInTransform<T, P, Q, PC, QC> builtInTransform,
                             IResolver<PC, QC> resolver,
                             HtPrologParser parser
    ) {
        super(symbolTable, interner);

        this.defaultBuiltIn = defaultBuiltIn;
        this.builtInTransform = builtInTransform;
        this.resolver = resolver;
        this.parser = parser;
    }

    public PrologPreCompiler() throws Exception {
        this(getAppContext().getSymbolTable(),
                getAppContext().getInterner(),
                getAppContext().getDefaultBuiltIn(),
                getAppContext().getBuiltInTransform(),
                (IResolver<PC, QC>) getAppContext().getResolverIC(),
                getAppContext().getParser());
    }

    /**
     * @return
     */
    public Deque<TT> getTaskQueue() {
        return taskQueue;
    }

    /**
     * @return
     */
    @Override
    public HtPrologParser getParser() throws Exception {
        return appContext.getParser();
    }

    @Override
    public boolean isDirective(T clause) throws Exception {
        return clause.getHead() == null &&
                clause.getBody().size() == 1 &&
                ((IFunctor) clause).getName() == interner.internFunctorName(IMPLIES, 1);
    }

    /**
     *
     */
    @Override
    public void endScope() {

    }

    @Override
    public void setCompilerObserver(ClauseChainObserver clauseChainObserver) {
        this.clauseChainObserver = clauseChainObserver;
    }

    /**
     * @param tokenSource
     * @param delims
     * @return
     * @throws Exception
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<T> preCompile(PlLexer tokenSource, EnumSet<DirectiveKind> delims) throws Exception {
        getLogger().info("Precompiling " + tokenSource.getPath() + " ...");
        List<T> list = new ArrayList<>();
        while (tokenSource.isOpen()) {
            ITerm t = getParser().termSentence();
            if (!isBOFPassed) {
                checkBOF(t);
            }
            if (!isEOFPassed) {
                checkEOF(t);
            }
            getLogger().info("term: " + t);
            for (ITerm term : preCompile(t)) {
                getLogger().info("precompiled: " + term);
                T clause = (T) parser.convert(term);
                getLogger().info("converted: " + clause);
                list.add(clause);
            }
        }

        return list;
    }

    @Override
    public void checkEOF(ITerm t) {
        if (t == END_OF_FILE) {
            if (!isEOFPassed) {
                logger.info("'end_of_file' is being passed.");
                if (parser.getTokenSource().atEOF) {
                    parser.popTokenSource();
                    isEOFPassed = true;
                } else {
                    parser.getTokenSource().atEOF = true;
                }
            } else {
                logger.info("'end_of_file' has been already passed. This one will be ignored...");
            }
        }
    }

    @Override
    public void checkBOF(ITerm t) throws IOException {
        if (t == BEGIN_OF_FILE) {
            if (!isBOFPassed) {
                logger.info("'begin_of_file' is being passed.");
                if (parser.getTokenSource().atBOF) {
                    isBOFPassed = true;
                    parser.getTokenSource().atBOF = false;
                } else {
                    final PlLexer ts = parser.getTokenSource();
                    ts.setLastOffset(ts.getLastOffset() + ts.getPrevOffset());
                    ts.atBOF = true;
                    ts.isBOFGenerated = true;
                    logger.info("'begin of file' has been reset to " + ts.getLastOffset());
                }
            } else {
                logger.info("Warning: 'begin_of_file' has been already reset ...");
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<ITerm> preCompile(ITerm term) throws Exception {
        logger.info("Precompiling ( " + term + ") ...");

        List<ITerm> clauses = preProcess(term);
        for (ITerm clause : clauses) {
//            substituteBuiltIns(clause);
//            initializeSymbolTable(clause);
//            topLevelCheck(clause);

            if (observer != null) {
                if (clause.isQuery()) {
                    observer.onQueryCompilation((Q) clause);
                } else {
                    observer.onCompilation((P) clause);
                }
            }
        }

        return clauses;
    }

    ;

    private List<ITerm> preProcess(ITerm clause) throws Exception {
        final PrologPreprocessor<T, TT, P, Q, PC, QC> preprocessor = new PrologPreprocessor<>(
                symbolTable,
                interner,
                defaultBuiltIn,
                builtInTransform,
                resolver,
                parser);
        final List<ITerm> l = new ArrayList<>();
        initTasks(preprocessor);

        for (int i = 0; i < taskQueue.size(); i++) {
            final TT task = taskQueue.remove();
            l.addAll(task.invoke(clause));
        }

        return l;
    }

    @SuppressWarnings("unchecked")
    protected void initTasks(PrologPreprocessor<T, TT, P, Q, PC, QC> preprocessor) {
        taskQueue.add((TT) new StandardPreprocessingTask<>(
                (IPreCompiler<T, PreCompilerTask<T>, ?, ?, ?, ?>) preprocessor,
                parser.getTokenSource(),
                EnumSet.of(DK_IF)));
        taskQueue.add((TT) new TermExpansionTask<>(
                (IPreCompiler<T, PreCompilerTask<T>, ?, ?, ?, ?>) preprocessor,
                parser.getTokenSource(),
                EnumSet.of(DK_IF)));
    }

    /**
     * Runs a symbol key traverser over the clause to be compiled, to ensure that all of its terms and sub-terms have
     * their symbol keys initialized.
     *
     * @param clause The clause to initialise the symbol keys of.
     */
    private void initializeSymbolTable(ITerm clause) {
        logger.info("Initializing symbol table ( " + clause + " ) ...");
        // Run the symbol key traverser over the clause, to ensure that all terms have their symbol keys correctly
        // set up.
        HtSymbolKeyTraverser symbolKeyTraverser = new HtSymbolKeyTraverser(interner, symbolTable, null);
        symbolKeyTraverser.setContextChangeVisitor(symbolKeyTraverser);
        HtBasePositionalVisitor visitor = new HtPositionAndOccurrenceVisitor(symbolTable, interner, symbolKeyTraverser);

        HtTermWalker symWalker =
                new HtTermWalker(new DepthFirstBacktrackingSearch<>(),
                        symbolKeyTraverser, visitor
                );
        symWalker.walk(clause);
    }

    /**
     * Finds and marks all functors within the clause that are considered to be top-level.
     *
     * @param clause The clause to top-level check.
     */
    private void topLevelCheck(ITerm clause) {
        logger.info("TopLevel checking ( " + clause + " ) ...");
        HtTermWalker walk = HtTermWalkers.positionalWalker(
                new HtTopLevelCheckVisitor(
                        symbolTable,
                        interner,
                        null));
        walk.walk(clause);
    }

//    /**
//     * Substitutes built-ins within a clause, with their built-in definitions.
//     *
//     * @param clause The clause to transform.
//     */
//    @SuppressWarnings("unchecked")
//    private void substituteBuiltIns(ITerm clause) {
//        logger.info("Built-in's substitution ( " + clause + " ) ...");
//        HtTermWalker walk =
//                HtTermWalkers.positionalWalker(
//                        new HtBuiltInTransformVisitor(
//                                symbolTable,
//                                interner,
//                                null,
//                                appContext.getBuiltInTransform()));
//        walk.walk(clause);
//    }

    /**
     * expand_term(+Term1, -Term2)
     * This predicate is normally called by the compiler on terms read from the input to perform preprocessing.
     * It consists of four steps, where each step processes the output of the previous step.
     * ========================================================================================================
     * <p>
     * 1. Test conditional compilation directives and translate all input to [] if we are in a `false branch' of
     * the conditional compilation. See section 4.3.1.2.
     * <p>
     * <p>
     * 2. Call term_expansion/2.
     * This predicate is first tried in the module that is being compiled and then
     * in modules from which this module inherits according to default_module/2. The output of the expansion in
     * a module is used as input for the next module. Using the default setup and when compiling a normal
     * application module M, this implies expansion is executed in M, user and finally in system. Library modules
     * inherit directly from system and can thus not be re-interpreted by term expansion rules in user.
     * <p>
     * <p>
     * 3. Call DCG expansion (dcg_translate_rule/2).
     * <p>
     * <p>
     * 4. Call expand_goal/2 on each body term that appears in the output of the previous steps.
     *
     * @param term
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<ITerm> expandTerm(ITerm term) throws Exception {
        final EnumSet<DirectiveKind> kinds = EnumSet.noneOf(DirectiveKind.class);
        final PlLexer ts = getTokenSourceForTerm(term);
//        getTaskQueue().add((TT) new TermExpansionTask(this, ts, kinds));

        return Collections.singletonList(term);
    }

    private PlLexer getTokenSourceForTerm(ITerm term) throws Exception {
        HtTermWriter writer = new HtTermWriter(term);
        String s = writer.writeTerm(term);
        byte[] in = s.getBytes();
        HiTalkInputStream stream = new HiTalkInputStream(new ByteArrayInputStream(in), null);
        return new PlLexer(stream, null);
    }

    /**
     * @param term
     * @return
     */
    public List<ITerm> callTermExpansion(ITerm term) throws Exception {
        return expandTerm0(term);
    }

    private List<ITerm> expandTerm0(ITerm term) {
        return Collections.singletonList(term);
    }

    /**
     * @param goal
     * @return
     */
    public List<ITerm> expandGoal(ITerm goal) {
        return callGoalExpansion(goal);
    }

    /**
     * @param goal
     * @return
     */
    private List<ITerm> callGoalExpansion(ITerm goal) {
        return expandGoal0(goal);
    }

    private List<ITerm> expandGoal0(ITerm goal) {
        return Collections.singletonList(goal);
    }

    public Logger getLogger() {
        return logger;
    }

    /**
     * @return
     */
    public Deque<TT> getQueue() {
        return taskQueue;
    }

    /**
     * @param item
     */
    public void remove(TT item) {
        taskQueue.remove(item);
    }

    public void toString0(StringBuilder sb) {

    }
}