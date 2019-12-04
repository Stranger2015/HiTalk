package org.ltc.hitalk.wam.compiler;

import static org.ltc.hitalk.wam.compiler.Language.LOGTALK;

public class LogtalkCompilerApp<T extends HtMethod, P, Q> extends PrologCompilerApp <T, P, Q> {
    private final static Language language = LOGTALK;

    /**
     * @param fn
     */
    public LogtalkCompilerApp ( String fn ) {
        super(fn);
    }

    public Language getLanguage () {
        return language;
    }
}
