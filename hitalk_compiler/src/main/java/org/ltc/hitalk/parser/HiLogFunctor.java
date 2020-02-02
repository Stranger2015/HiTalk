package org.ltc.hitalk.parser;

import com.thesett.common.util.doublemaps.SymbolKey;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.compiler.HtFunctor;

import static org.ltc.hitalk.parser.HiLogParser.HILOG_APPLY_INT;

/**
 *
 */
public class HiLogFunctor extends HtFunctor {
    /**
     * @param name
     * @param args
     */
    public HiLogFunctor(ITerm name, ListTerm args) {
        super(HILOG_APPLY_INT, args.addHead(name));
    }

    /**
     * @return
     */
    public int getHeadsOffset() {
        return 1;
    }

    /**
     *
     */
    public HiLogFunctor(ListTerm args) {
        super(HILOG_APPLY_INT, args);
    }

    /**
     * @return
     */
    public int getName() {
        return HILOG_APPLY_INT;
    }

    public void setSymbolKey(SymbolKey key) {
        super.setSymbolKey(key);
    }
}
