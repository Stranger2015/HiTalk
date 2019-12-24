package org.ltc.hitalk.core;

import com.thesett.aima.logic.fol.FunctorName;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.parser.Directive;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.parser.PlTokenSource;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler;
import org.ltc.hitalk.wam.task.PreCompilerTask;
import org.ltc.hitalk.wam.task.TermExpansionTask;

import java.util.*;

import static java.util.EnumSet.noneOf;
import static org.ltc.hitalk.parser.Directive.DirectiveKind.ENCODING;

/**
 *
 */
public interface IPreCompiler extends IQueueHolder <PreCompilerTask>, IHitalkObject {

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
//
//    }
//
//    @Override
//    public Logger getConsole () {
//        return logger;
//    }

    /**
     * @param tokenSource
     * @return
     * @throws Exception
     */
    default List <HtClause> preCompile ( PlTokenSource tokenSource, EnumSet <DirectiveKind> delims ) throws Exception {
        final List <HtClause> list = new ArrayList <>();
        getParser().setTokenSource(tokenSource);
        while (true) {
            ITerm t = getParser().next();
            if (t == PlPrologParser.BEGIN_OF_FILE_ATOM) {
                getQueue().push(new TermExpansionTask(this, tokenSource,
                        EnumSet.of(ENCODING))); //read until
                continue;
            }
            if (t == PlPrologParser.END_OF_FILE_ATOM) {
                getQueue().push(new TermExpansionTask(this,
                        tokenSource,
                        noneOf(DirectiveKind.class)));
                getParser().popTokenSource();
                break;
            }
            HtClause c = getParser().convert(t);
            if (!checkDirective(c, delims)) {
                list.add(c);
                break;
            }
        }

        return list;
    }

    Deque <PreCompilerTask> getQueue ();

    PlPrologParser getParser ();

    boolean isDirective ( HtClause clause );

    /**
     * @param clause
     * @param delims
     * @return
     */
    default boolean checkDirective ( HtClause clause, EnumSet <DirectiveKind> delims ) {
        final IVafInterner interner = getInterner();
        if (!isDirective(clause)) {
            return false;
        }
        final Directive directive = (Directive) clause;
        if (delims.contains(directive.getKind())) {
            return true;
        }
        final FunctorName functorName = interner.getDeinternedFunctorName(clause.getGoal(0).getName());
        return Objects.equals(functorName.getName(), directive.toString())/* && functorName.getArity() == 1*/;//fixme
    }

//        parser.getTokenSource().setEncodingPermitted(false);

//        /**
//         * @param resolver
//         */
//        public void setResolver (IResolver < P, Q > resolver ){

    /**
     *
     */
    void endScope ();

    default IVafInterner getInterner () {
        return BaseApp.getAppContext().getInterner();
    }

    void setCompilerObserver ( PrologWAMCompiler.ClauseChainObserver clauseChainObserver );


    /**
     * expand_term(+Term1, -Term2)
     * This predicate is normally called by the compiler on terms read from the input to perform preprocessing.
     * It consists of four steps, where each step processes the output of the previous step.
     * ========================================================================================================
     *
     * 1. Test conditional compilation directives and translate all input to [] if we are in a `false branch' of
     * the conditional compilation. See section 4.3.1.2.
     * <p>
     *
     * 2. Call term_expansion/2.
     * This predicate is first tried in the module that is being compiled and then
     * in modules from which this module inherits according to default_module/2. The output of the expansion in
     * a module is used as input for the next module. Using the default setup and when compiling a normal
     * application module M, this implies expansion is executed in M, user and finally in system. Library modules
     * inherit directly from system and can thus not be re-interpreted by term expansion rules in user.
     * <p>
     *
     * 3. Call DCG expansion (dcg_translate_rule/2).
     * <p>
     *
     * 4. Call expand_goal/2 on each body term that appears in the output of the previous steps.
     *
     * @param term
     * @return
     */
}
