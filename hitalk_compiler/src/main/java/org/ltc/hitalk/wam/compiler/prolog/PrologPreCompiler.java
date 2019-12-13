package org.ltc.hitalk.wam.compiler.prolog;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.compiler.AbstractBaseMachine;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.parser.PlTokenSource;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.ltc.hitalk.wam.task.CompilerTask;
import org.ltc.hitalk.wam.task.TermExpansionTask;

import java.util.*;

import static org.ltc.hitalk.core.BaseApp.getAppContext;
import static org.ltc.hitalk.parser.Directive.DirectiveKind.IF;

/**
 *
 */
public
class PrologPreCompiler<T extends HtClause, P, Q> extends AbstractBaseMachine implements IPreCompiler /*implements ICompiler <T, P, Q> */ {

    final protected PlPrologParser parser;
    final protected PrologDefaultBuiltIn defaultBuiltIn;

    /**
     * Holds the built in transformation.
     */
    protected final PrologBuiltInTransform <T, P, Q> builtInTransform;
    protected final IResolver <HtPredicate, HtClause> resolver;
    protected LogicCompilerObserver <P, Q> observer;

    ///    protected final Deque<ITerm> condCompilationQueue=new ArrayDeque <>();
//    protected final Deque<ITerm> execQueue=new ArrayDeque <>();
//    protected final Deque<ITerm> termExpansionQueue=new ArrayDeque <>();
    protected final Deque <CompilerTask> compilerTaskQueue = new ArrayDeque <>();


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
//
//    /**
//     * @param string
//     * @param flags
//     * @throws Exception
//     */
//    @Override
//    public void compileString ( String string, HtProperty... flags ) throws Exception {
//        ICompiler.super.compileString(string, flags);
//    }

//    /**
//     * @param tokenSource
//     * @param flags
//     * @throws IOException
//     * @throws SourceCodeException
//     */
//    @Override
//    public void compile ( PlTokenSource tokenSource, HtProperty... flags )
//            throws IOException, SourceCodeException, ParseException {
//        getConsole().info("Compiling " + tokenSource.getPath() + "... ");
//        /*
//        // Set up a parser on the token source.
//        LibParser libParser = new LibParser();
//        libParser.setTokenSource(tokenSource);
//
//        // Load the built-ins into the domain
//        while (true) {
//            ISentence <ITerm> sentence = libParser.parse();
//            final ITerm term = sentence.getT();
//            //TODO  GLOBAL WITHOUT SPECIAL CASES
//            if (term == PlPrologParser.BEGIN_OF_FILE_ATOM) {//ignore
//                //    final List <ITerm> l = preCompiler.expandTerm(term);
//                continue;
//            }
//            if (term == PlPrologParser.END_OF_FILE_ATOM) {
//                if (!libParser.getTokenSource().isEofGenerated()) {
//                    parser.popTokenSource();
//                    break;
//                }
//            }
//            //            compiler.compile(sentence);
//            HtClause clause = libParser.convert(sentence.getT());
//            preCompiler.compile(clause);
//        }
//        preCompiler.endScope();
//        *
//        * */
//        parser.setTokenSource(tokenSource);
//        while (true) {
//            ITerm t = parser.next();
//            if (t == PlPrologParser.BEGIN_OF_FILE_ATOM) {
////                termExpansionQueue.push(t);
//                compilerTaskQueue.push(new TermExpansionTask(t));
//                continue;
//            }
//            if (t == PlPrologParser.END_OF_FILE_ATOM) {
////                termExpansionQueue.push(t);
//                if (!parser.getTokenSource().isEofGenerated()) {
//                    parser.popTokenSource();
//                    break;
//                }
//            }
//            T c = (T) parser.convert(t);//FIXME
//            compile(c, flags);
//        }
//    }

    public List <HtClause> preCompile ( PlTokenSource tokenSource ) {
        return null;
    }

    //    @Override
    public PlPrologParser getParser () {
        return parser;
    }

    /**
     *
     */
    public void endScope () {

    }

    public void preCompile ( T clause ) throws SourceCodeException {
        logger.debug("precompiling " + clause);
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
        getTaskQueue().push(new TermExpansionTask(this, term1 -> {

            return null;
        }, IF));
        return Objects.requireNonNull(getTaskQueue().peek()).invoke(term);
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
}