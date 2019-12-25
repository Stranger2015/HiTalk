package org.ltc.hitalk.wam.compiler.prolog;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import org.ltc.hitalk.compiler.AbstractBaseMachine;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtSourceCodeException;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler.ClauseChainObserver;
import org.ltc.hitalk.wam.task.ExecutionTask;
import org.ltc.hitalk.wam.task.PreCompilerTask;

import java.util.*;

import static org.ltc.hitalk.core.BaseApp.getAppContext;

/**
 *
 */
public
class PrologPreCompiler<T extends HtClause, P, Q> extends AbstractBaseMachine
        implements IPreCompiler {

    final protected PlPrologParser parser;
    final protected PrologDefaultBuiltIn defaultBuiltIn;

    /**
     * Holds the built in transformation.
     */
    protected final PrologBuiltInTransform <T, P, Q> builtInTransform;
    protected final IResolver <HtPredicate, HtClause> resolver;
    protected LogicCompilerObserver <P, Q> observer;

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

    public PrologPreCompiler () {
        this(getAppContext().getSymbolTable(),
                getAppContext().getInterner(),
                getAppContext().getDefaultBuiltIn(),
                getAppContext().getBuiltInTransform(),
                getAppContext().getResolverPre(),
                getAppContext().getParser());
    }

    public Deque <PreCompilerTask> getTaskQueue () {
        return taskQueue;
    }

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
     * @param clause
     * @throws HtSourceCodeException
     */
    public void preCompile ( T clause ) throws HtSourceCodeException {
        logger.debug("Precompiling " + clause);
        if (clause.getT().getHead() == null) {
            final IFunctor goal = (IFunctor) clause.getBody().get(0);
//            if (checkDirective(goal, )) {
//                parser.getTokenSource().setEncodingPermitted(false);
//            }
        }
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