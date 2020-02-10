package org.ltc.hitalk.wam.compiler.hilog;

import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.Language;
import org.ltc.hitalk.wam.compiler.prolog.PrologCompilerApp;

/**
 * @param <T>
 * @param <P>
 * @param <Q>
 */
public class HiLogCompilerApp<T extends HtClause, P, Q, PC, QC> extends PrologCompilerApp <T, P, Q, PC, QC> {

//    private final static Language language = HILOG;

    /**
     * @param fn
     */
    public HiLogCompilerApp(String fn) throws Exception {
        super(fn);
    }

    /**
     * @return
     */
    public Language getLanguage () {
        return language;
    }
}
