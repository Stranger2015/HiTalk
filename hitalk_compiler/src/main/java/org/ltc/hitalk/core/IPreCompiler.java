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

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import static org.ltc.hitalk.core.BaseApp.getAppContext;

/**
 *
 */
public interface IPreCompiler<T extends HtClause, TT extends PreCompilerTask<T>, P, Q, PC, QC>
        extends IQueueHolder<T, TT>, IHitalkObject {
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
    void checkBOF(ITerm term) throws IOException;

    /**
     * @param term
     * @return
     */
    void checkEOF(ITerm term) throws IOException;

}
