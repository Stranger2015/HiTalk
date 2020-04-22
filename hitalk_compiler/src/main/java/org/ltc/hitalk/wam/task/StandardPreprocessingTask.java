package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.term.ITerm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
    protected List<ITerm> invoke0(ITerm term) throws IOException {
        List<ITerm> list = new ArrayList<>();
        final List<ITerm> l = super.invoke0(term);
        for (ITerm t : l) {
            getLogger().info("standard preprocessing... \n" + t);
//            assert stdPreprocess((IFunctor) t) != null;
            list.addAll(stdPreprocess(t));
        }

        output = list;
        return output;

    }

    private List<ITerm> stdPreprocess(ITerm t) {
        return Collections.singletonList(t);
    }
//
//    protected List<ListTerm> expandTerm(IFunctor f) {
//        final ListTerm lt = new ListTerm(asList(f.getArgument(0), f.getArgument(1)));
//        return EXPAND_TERM.getBuiltInDef().apply(lt);
//    }

    /**
     * @param sb
     */
    public void toString0(StringBuilder sb) {

    }
}