package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HiLogParser;
import org.ltc.hitalk.parser.HiTalkParser;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.hilog.HiLogWAMCompiler;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiler;
import org.ltc.hitalk.wam.compiler.logtalk.LogtalkWAMCompiler;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler;

public enum Language {
    PROLOG("Prolog",
            new PlPrologParser(),
            new PrologWAMCompiler <>()),
    HITALK("HiTalk",
            new HiTalkParser(),
            new HiTalkWAMCompiler()),
    HILOG("HiLog",
            new HiLogParser(),
            new HiLogWAMCompiler <>()),
    LOGTALK("Logtalk",
            new LogtalkParser(),
            new LogtalkWAMCompiler());

    private final String name;
    public final PlPrologParser parser;
    public final PrologWAMCompiler <HtClause, HtPredicate, HtClause,
            HiTalkWAMCompiledPredicate, HiTalkWAMCompiledClause> compiler;

    /**
     * @param name
     */
    Language ( String name,
               PlPrologParser parser,
               PrologWAMCompiler <HtClause, HtPredicate, HtClause,
                       HiTalkWAMCompiledPredicate, HiTalkWAMCompiledClause> compiler ) {
        this.name = name;
        this.parser = parser;
        this.compiler = compiler;
    }

    /**
     * @return
     */
    public String getName () {
        return name;
    }

    public PlPrologParser getParser () {
        return parser;
    }

    public PrologWAMCompiler <HtClause, HtPredicate, HtClause,
            HiTalkWAMCompiledPredicate, HiTalkWAMCompiledClause> getCompiler () {

        return compiler;
    }
}
