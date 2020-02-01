package org.ltc.hitalk.parser;

import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.compiler.HtFunctor;

import static org.ltc.hitalk.parser.HiLogParser.HILOG_APPLY;

/**
 *
 */
public class HiLogFunctor extends HtFunctor {
    /**
     * @param name
     * @param args
     */
    public HiLogFunctor(ITerm name, ListTerm args) {
        super(args.addHead(HILOG_APPLY).addHead(name));
    }

    /**
     * @return
     */
    public int getHeadsOffset() {
        return 2;
    }

    /**
     */
    public HiLogFunctor(ListTerm args) {
        super(args);
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
}
