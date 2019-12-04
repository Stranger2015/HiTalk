package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.parser.HiLogParser;
import org.ltc.hitalk.parser.HiTalkParser;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.term.io.HiLogWAMCompiler;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiler;
import org.ltc.hitalk.wam.compiler.hitalk.logtalk.LogtalkCompiler;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler;

public enum Language {
    PROLOG("Prolog", PlPrologParser.class, PrologWAMCompiler.class),
    HILOG("HiLog", HiLogParser.class, HiLogWAMCompiler.class),
    HITALK("HiTalk", HiTalkParser.class, HiTalkWAMCompiler.class),
    LOGTALK("Logtalk", LogtalkParser.class, LogtalkCompiler.class);

    private final String name;
    private final Class <?> parserClass;
    private final Class <?> compilerClass;

    /**
     * @param name
     * @param parserClass
     * @param compilerClass
     */
    Language ( String name, Class <?> parserClass, Class <?> compilerClass ) {
        this.name = name;
        this.parserClass = parserClass;
        this.compilerClass = compilerClass;
    }

    /**
     * @return
     */
    public String getName () {
        return name;
    }


    public Class <?> getCompilerClass () {
        return compilerClass;
    }

    public Class <?> getParserClass () {
        return parserClass;
    }
}
