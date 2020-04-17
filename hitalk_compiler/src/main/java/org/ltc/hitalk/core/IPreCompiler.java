package org.ltc.hitalk.core;

import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.parser.Directive;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.HtFunctorName;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler.ClauseChainObserver;
import org.ltc.hitalk.wam.task.PreCompilerTask;
import org.slf4j.Logger;

import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import static org.ltc.hitalk.core.BaseApp.getAppContext;

/**
 *
 */
public interface IPreCompiler<T extends HtClause> extends IQueueHolder<PreCompilerTask>, IHitalkObject {
    /**
     * @return
     */
    Logger getLogger();

    /**
     * @param tokenSource
     * @return
     * @throws Exception
     */
    List<T> preCompile(PlLexer tokenSource, EnumSet<DirectiveKind> delims) throws Exception;

    Deque<PreCompilerTask> getQueue();

    HtPrologParser getParser() throws Exception;

    boolean isDirective(T clause) throws Exception;

    /**
     * @param clause
     * @param delims
     * @return
     */
    default boolean checkDirective(T clause, EnumSet<DirectiveKind> delims) throws Exception {
        final IVafInterner interner = getInterner();
        if (!isDirective(clause)) {
            return false;
        }
        final Directive directive = (Directive) clause;
        if (delims.contains(directive.getKind())) {
            return true;
        }

        HtFunctorName functorName = interner.getDeinternedFunctorName(clause.getGoal(0).getName());
        return Objects.equals(functorName.getName(), directive.toString());//fixme
    }

    /**
     *
     */
    void endScope() throws Exception;

    default IVafInterner getInterner() {
        return getAppContext().getInterner();
    }

    void setCompilerObserver(ClauseChainObserver clauseChainObserver);

    /**
     * @param term
     * @return
     */
    boolean checkBOF(ITerm term);

    /**
     * @param term
     * @return
     */
    boolean checkEOF(ITerm term);

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
