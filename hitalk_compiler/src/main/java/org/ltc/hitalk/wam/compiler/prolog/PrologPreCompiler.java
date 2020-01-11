package org.ltc.hitalk.wam.compiler.prolog;

import com.thesett.aima.search.util.backtracking.DepthFirstBacktrackingSearch;
import org.ltc.hitalk.compiler.AbstractBaseMachine;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtSourceCodeException;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.parser.PlTokenSource;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.HtTermWalkers;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.ltc.hitalk.wam.compiler.hitalk.HtSymbolKeyTraverser;
import org.ltc.hitalk.wam.compiler.hitalk.HtTermWalker;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler.ClauseChainObserver;
import org.ltc.hitalk.wam.task.ExecutionTask;
import org.ltc.hitalk.wam.task.PreCompilerTask;
import org.ltc.hitalk.wam.task.TermExpansionTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.*;

import static java.util.EnumSet.noneOf;
import static org.ltc.hitalk.core.BaseApp.appContext;
import static org.ltc.hitalk.core.BaseApp.getAppContext;
import static org.ltc.hitalk.parser.Directive.DirectiveKind.ENCODING;
import static org.ltc.hitalk.parser.PlPrologParser.BEGIN_OF_FILE_ATOM;
import static org.ltc.hitalk.parser.PlPrologParser.END_OF_FILE_ATOM;

/**
 *
 */
public
class PrologPreCompiler<T extends HtClause, P, Q> extends AbstractBaseMachine implements IPreCompiler {
    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    final protected PlPrologParser parser;
    final protected PrologDefaultBuiltIn defaultBuiltIn;

    /**
     * Holds the built in transformation.
     */
    protected final PrologBuiltInTransform <T, P, Q> builtInTransform;
    protected final IResolver <HtPredicate, HtClause> resolver;
    protected ICompilerObserver <P, Q> observer;

    //    protected final Deque <CompilerTask> compilerTaskQueue = new ArrayDeque <>();
    protected ClauseChainObserver clauseChainObserver;
    protected final Deque <PreCompilerTask> taskQueue = new ArrayDeque <>();


    /**
     * @param symbolTable
     * @param interner
     * @param builtInTransform
     * @param resolver
     */
    public PrologPreCompiler ( ISymbolTable <Integer, String, Object> symbolTable,
                               IVafInterner interner,
                               PrologDefaultBuiltIn defaultBuiltIn,
                               PrologBuiltInTransform <T, P, Q> builtInTransform,
                               IResolver <HtPredicate, HtClause> resolver,
                               PlPrologParser parser
    ) {
        super(symbolTable, interner);

        this.defaultBuiltIn = defaultBuiltIn;//instructionCompiler.getDefaultBuiltIn();
        this.builtInTransform = builtInTransform;
        this.resolver = resolver;
        this.parser = parser;
    }

    public PrologPreCompiler () throws FileNotFoundException {
        this(getAppContext().getSymbolTable(),
                getAppContext().getInterner(),
                getAppContext().getDefaultBuiltIn(),
                getAppContext().getBuiltInTransform(),
                getAppContext().getResolverPre(),
                getAppContext().getParser());
    }

    /**
     * @return
     */
    public Deque <PreCompilerTask> getTaskQueue () {
        return taskQueue;
    }

    /**
     * @return
     */
    @Override
    public PlPrologParser getParser () {
        return parser;
    }

    @Override
    public boolean isDirective ( HtClause clause ) {
        return false;
    }

    /**
     *
     */
    @Override
    public void endScope () {

    }

    @Override
    public void setCompilerObserver ( ClauseChainObserver clauseChainObserver ) {
        this.clauseChainObserver = clauseChainObserver;
    }

    /**
     * @param tokenSource
     * @param delims
     * @return
     * @throws Exception
     */
    @Override
    public List <HtClause> preCompile ( PlTokenSource tokenSource, EnumSet <DirectiveKind> delims ) throws Exception {
        getLogger().info("Precompiling " + tokenSource.getPath() + " ...");
        final List <HtClause> list = new ArrayList <>();
        while (tokenSource.isOpen()) {
            ITerm t = getParser().next();
            if (t.equals(BEGIN_OF_FILE_ATOM)) {
                getLogger().info("begin_of_file");
                getQueue().push(new TermExpansionTask(this, tokenSource, EnumSet.of(ENCODING)));
//                continue;
            } else if (t.equals(END_OF_FILE_ATOM)) {
                getLogger().info("end_of_file");
                getQueue().push(new TermExpansionTask(this, tokenSource, noneOf(DirectiveKind.class)));
                getParser().popTokenSource();
            } else if (t != null) {//?????????????
                preCompile(t);
                HtClause c = getParser().convert(t);
                if (!checkDirective(c, delims)) {
                    list.add(c);
                }
            } else {
                logger.info("no term found!!");
            }
        }

        return list;
    }

    /**
     * @param clause
     * @throws HtSourceCodeException
     */
    public void preCompile ( T clause ) throws HtSourceCodeException {
        logger.debug("Precompiling " + "( " + clause + ") ...");
        if (clause.getT().getHead() == null) {
            final IFunctor goal = (IFunctor) clause.getBody().getHead(0);
//            if (checkDirective(goal, )) {
//                parser.getTokenSource().setEncodingPermitted(false);
//            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void preCompile ( ITerm clause ) throws Exception {
        logger.debug("Precompiling " + "( " + clause + ") ...");
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

    /**
     * Runs a symbol key traverser over the clause to be compiled, to ensure that all of its terms and sub-terms have
     * their symbol keys initialized.
     *
     * @param clause The clause to initialise the symbol keys of.
     */

    private void initializeSymbolTable ( ITerm clause ) {
        logger.debug("Initializing symbol table " + "( " + clause + ") ...");
        // Run the symbol key traverser over the clause, to ensure that all terms have their symbol keys correctly
        // set up.
        HtSymbolKeyTraverser symbolKeyTraverser = new HtSymbolKeyTraverser(interner, symbolTable, null);
        symbolKeyTraverser.setContextChangeVisitor(symbolKeyTraverser);

        HtTermWalker symWalker =
                new HtTermWalker(new DepthFirstBacktrackingSearch <>(),
                        symbolKeyTraverser,
                        symbolKeyTraverser);
        symWalker.walk(clause);
    }

    /**
     * Finds and marks all functors within the clause that are considered to be top-level.
     *
     * @param clause The clause to top-level check.
     */
    private void topLevelCheck ( ITerm clause ) {
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
    private void substituteBuiltIns ( ITerm clause ) {
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

//
//    private boolean checkEncodingDirective ( IFunctor goal ) {
//        final FunctorName functorName = interner.getDeinternedFunctorName(goal.getName());
//        return Objects.equals(functorName.getName(), PrologAtoms.ENCODING) && functorName.getArity() == 1;
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
    public List <ITerm> expandTerm ( ITerm term ) {
        getQueue().push(new ExecutionTask(this));
        return Objects.requireNonNull(getQueue().peek()).invoke(term);
    }

    /**
     * @param term
     * @return
     */
    public List <ITerm> callTermExpansion ( ITerm term ) {
        final List <ITerm> l = new ArrayList <>();

        return l;
    }

    /**
     * @param goal
     * @return
     */
    public List <IFunctor> expandGoal ( IFunctor goal ) {
        return callGoalExpansion(goal);
    }

    /**
     * @param goal
     * @return
     */
    private List <IFunctor> callGoalExpansion ( IFunctor goal ) {
        final List <IFunctor> l = new ArrayList <>();
        return l;
    }

    public Logger getLogger () {
        return logger;
    }

    /**
     * @return
     */
    public Deque <PreCompilerTask> getQueue () {
        return taskQueue;
    }

    /**
     * @param item
     */
    public void push ( PreCompilerTask item ) {

    }

    /**
     * @param item
     */
    public void remove ( PreCompilerTask item ) {

    }

    public void toString0 ( StringBuilder sb ) {
    }
}