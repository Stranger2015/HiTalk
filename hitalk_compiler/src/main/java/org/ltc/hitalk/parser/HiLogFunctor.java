package org.ltc.hitalk.parser;

import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.compiler.HtFunctor;
import org.ltc.hitalk.wam.compiler.HtFunctorName;

import static org.ltc.hitalk.core.BaseApp.getAppContext;
import static org.ltc.hitalk.parser.HiLogParser.HILOG_APPLY;
import static org.ltc.hitalk.parser.HiLogParser.HILOG_APPLY_STRING;

/**
 *
 */
public class HiLogFunctor extends HtFunctor {
    private HtFunctorName name;
    private ListTerm args;

    //    /**
//     * @param hilogApply
//     * @param arity
//     * @param arityDelta
//     */
//    public HiLogFunctor(int hilogApply, int arity, int arityDelta ) {
//        super(hilogApply, arity, arityDelta);
//    }
//
    public HiLogFunctor(ITerm name, ListTerm args) {
        super(HILOG_APPLY, name, args);
    }

    /**
     * @param name
     */
    public HiLogFunctor(HtFunctorName name, ListTerm args) {
        super(
                getAppContext().getInterner().internFunctorName(HILOG_APPLY_STRING, 1),
                getAppContext().getInterner().internFunctorName(name),
                args);
    }

    /**
     * @return
     */
    public ListTerm getArguments() {
        return super.getArguments();
    }

    /**
     * @return
     */
    public int getName() {
        return super.getName();
    }

    /**
     * @return
     */
    public boolean isHiLog() {
        return super.isHiLog();
    }
}
