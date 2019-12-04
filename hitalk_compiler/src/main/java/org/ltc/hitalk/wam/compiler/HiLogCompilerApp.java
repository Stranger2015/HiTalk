package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.parser.HtClause;

import static org.ltc.hitalk.wam.compiler.Language.HILOG;

/**
 * @param <T>
 * @param <P>
 * @param <Q>
 */
public class HiLogCompilerApp<T extends HtClause, P, Q> extends PrologCompilerApp <T, P, Q> {

    private final static Language language = HILOG;

    /**
     * @param fn
     */
    public HiLogCompilerApp ( String fn ) {
        super(fn);

    }

    /**
     * @return
     */
    public Language getLanguage () {
        return language;
    }
}
