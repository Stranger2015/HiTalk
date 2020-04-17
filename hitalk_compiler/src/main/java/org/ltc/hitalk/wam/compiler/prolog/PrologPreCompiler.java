package org.ltc.hitalk.wam.compiler.prolog;

import com.thesett.aima.search.util.backtracking.DepthFirstBacktrackingSearch;
import org.ltc.hitalk.compiler.AbstractBaseMachine;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.core.utils.TermUtilities;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.io.HtTermWriter;
import org.ltc.hitalk.wam.compiler.HtTermWalkers;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.ltc.hitalk.wam.compiler.hitalk.HtSymbolKeyTraverser;
import org.ltc.hitalk.wam.compiler.hitalk.HtTermWalker;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler.ClauseChainObserver;
import org.ltc.hitalk.wam.task.PreCompilerTask;
import org.ltc.hitalk.wam.task.TermExpansionTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import static org.ltc.hitalk.core.BaseApp.appContext;
import static org.ltc.hitalk.core.BaseApp.getAppContext;
import static org.ltc.hitalk.parser.Directive.DirectiveKind.*;
import static org.ltc.hitalk.parser.HtPrologParser.BEGIN_OF_FILE;
import static org.ltc.hitalk.parser.HtPrologParser.END_OF_FILE;
import static org.ltc.hitalk.parser.PrologAtoms.IMPLIES;

/**
 *
 */
public
class PrologPreCompiler<T extends HtClause, P, Q, PC, QC> extends AbstractBaseMachine implements IPreCompiler<T> {

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
    protected final Deque<PreCompilerTask> taskQueue = new ArrayDeque<>();
    protected boolean isBOFPassed;


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

        this.defaultBuiltIn = defaultBuiltIn;//instructionCompiler.getDefaultBuiltIn();
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
    public Deque<PreCompilerTask> getTaskQueue() {
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
            if (checkBOF(t)) {
                getLogger().info("begin_of_file");
                break;
            }
            if (checkEOF(t)) {
                getLogger().info("end_of_file");

            } else {
                list.add((T) TermUtilities.convertToClause(t, interner));
            }
        }
        return list;
    }

    @Override
    public boolean checkEOF(ITerm t) {
        boolean result = false;
        if (t == END_OF_FILE) {
            if (!isBOFPassed) {
                isBOFPassed = true;
                parser.popTokenSource();
                logger.info("'end_of_file' is being passed.");
                result = true;
            } else {
                logger.info("'end_of_file' has been already passed. This one will be ignored...");
            }
        }
        return result;
    }

    @Override
    public boolean checkBOF(ITerm t) {
        boolean result = true;
        if (t == BEGIN_OF_FILE) {
            if (!isBOFPassed) {
                isBOFPassed = true;
                logger.info("'begin_of_file' is being passed.");
            } else {
                logger.info("'begin_of_file' has been already passed. This one will be ignored...");
                result = false;
            }
        }
        return result;
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
            substituteBuiltIns(clause);
            initializeSymbolTable(clause);
            topLevelCheck(clause);

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

    private List<ITerm> preProcess(ITerm clause) throws Exception {
        final TermExpansionTask task = new TermExpansionTask(
                this,
                getParser().getTokenSource(),
                EnumSet.of(DK_IF, DK_ENCODING, DK_HILOG)
        );

        return task.invoke(clause);
    }

    /**
     * Runs a symbol key traverser over the clause to be compiled, to ensure that all of its terms and sub-terms have
     * their symbol keys initialized.
     *
     * @param clause The clause to initialise the symbol keys of.
     */
    private void initializeSymbolTable(ITerm clause) {
        logger.info("Initializing symbol table ( " + clause + ") ...");
        // Run the symbol key traverser over the clause, to ensure that all terms have their symbol keys correctly
        // set up.
        HtSymbolKeyTraverser symbolKeyTraverser = new HtSymbolKeyTraverser(interner, symbolTable, null);
        symbolKeyTraverser.setContextChangeVisitor(symbolKeyTraverser);

        HtTermWalker symWalker =
                new HtTermWalker(new DepthFirstBacktrackingSearch<>(),
                        symbolKeyTraverser,
                        symbolKeyTraverser);
        symWalker.walk(clause);
    }

    /**
     * Finds and marks all functors within the clause that are considered to be top-level.
     *
     * @param clause The clause to top-level check.
     */
    private void topLevelCheck(ITerm clause) {
        logger.info("TopLevel checking " + "( " + clause + ") ...");
        HtTermWalker walk = HtTermWalkers.positionalWalker(
                new HtTopLevelCheckVisitor(
                        symbolTable,
                        interner,
                        null));
        walk.walk(clause);
    }

    /**
     * Substitutes built-ins within a clause, with their built-in definitions.
     *
     * @param clause The clause to transform.
     */
    @SuppressWarnings("unchecked")
    private void substituteBuiltIns(ITerm clause) {
        logger.debug("Built-in's substitution " + "( " + clause + ") ...");
        HtTermWalker walk =
                HtTermWalkers.positionalWalker(
                        new HtBuiltInTransformVisitor(
                                symbolTable,
                                interner,
                                null,
                                appContext.getBuiltInTransform()));
        walk.walk(clause);
    }

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
    public List<ITerm> expandTerm(ITerm term) throws Exception {
        final EnumSet<DirectiveKind> kinds = EnumSet.noneOf(DirectiveKind.class);
        final PlLexer ts = getTokenSourceForTerm(term);
        getQueue().push(new TermExpansionTask(this, ts, kinds));

        return Collections.singletonList(term);
    }

    private PlLexer getTokenSourceForTerm(ITerm term) throws FileNotFoundException {
        HtTermWriter writer = new HtTermWriter(term);
        return null;
    }

    /**
     * @param term
     * @return
     */
    public List<ITerm> callTermExpansion(ITerm term) throws IOException {
        return getQueue().pop().invoke(term);
    }

    /**
     * @param goal
     * @return
     */
    public List<IFunctor> expandGoal(IFunctor goal) {
        return callGoalExpansion(goal);
    }

    /**
     * @param goal
     * @return
     */
    private List<IFunctor> callGoalExpansion(IFunctor goal) {
        final List<IFunctor> l = new ArrayList<>();

        return l;
    }

    public Logger getLogger() {
        return logger;
    }

    /**
     * @return
     */
    public Deque<PreCompilerTask> getQueue() {
        return taskQueue;
    }

    /**
     * @param item
     */
    public void remove(PreCompilerTask item) {
        taskQueue.remove(item);
    }

    public void toString0(StringBuilder sb) {
    }
}