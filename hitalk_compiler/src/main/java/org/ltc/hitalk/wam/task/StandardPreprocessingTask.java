package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.core.utils.TermUtilities.FlattenTermVisitor;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.term.ITerm;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 *
 */
public class StandardPreprocessingTask<T extends HtClause> extends PreCompilerTask<T> {

    /**
     * @param preCompiler
     * @param tokenSource
     * @param kind
     */
    public StandardPreprocessingTask(IPreCompiler<T, PreCompilerTask<T>, ?, ?, ?, ?> preCompiler,
                                     PlLexer tokenSource,
                                     EnumSet<DirectiveKind> kind) {
        super(preCompiler, tokenSource, kind);
    }

    /**
     * @param term
     * @return
     */
    @Override
    protected List<ITerm> invoke0(ITerm term) throws Exception {
        List<ITerm> list = new ArrayList<>();
        final List<ITerm> l = super.invoke0(term);
        for (ITerm t : l) {
            getLogger().info("Standard preprocessing... \n" + t);
            list.addAll(stdPreprocess(t));
        }

        output = list;
        return output;
    }

    private List<ITerm> stdPreprocess(ITerm t) throws Exception {
        return t.accept(new FlattenTermVisitor());
    }

    /**
     * @param sb
     */
    public void toString0(StringBuilder sb) {

    }
}