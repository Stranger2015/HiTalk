package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.parser.HiLogParser;
import org.ltc.hitalk.parser.HiTalkParser;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.hilog.HiLogInstructionCompiler;
import org.ltc.hitalk.wam.compiler.hilog.HiLogPreCompiler;
import org.ltc.hitalk.wam.compiler.hilog.HiLogWAMCompiler;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkInstructionCompiler;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkPreCompiler;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiler;
import org.ltc.hitalk.wam.compiler.logtalk.LogtalkInstructionCompiler;
import org.ltc.hitalk.wam.compiler.logtalk.LogtalkPreCompiler;
import org.ltc.hitalk.wam.compiler.logtalk.LogtalkWAMCompiler;
import org.ltc.hitalk.wam.compiler.prolog.PrologInstructionCompiler;
import org.ltc.hitalk.wam.compiler.prolog.PrologPreCompiler;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler;

public enum Language {
    PROLOG("LTC-Prolog",
            PlPrologParser.class,
            PrologWAMCompiler.class,
            PrologPreCompiler.class,
            PrologInstructionCompiler.class),
    HITALK("HiTalk",
            HiTalkParser.class,
            HiTalkWAMCompiler.class,
            HiTalkPreCompiler.class,
            HiTalkInstructionCompiler.class),
    HILOG("HiLog",
            HiLogParser.class,
            HiLogWAMCompiler.class,
            HiLogPreCompiler.class,
            HiLogInstructionCompiler.class),
    LOGTALK("Logtalk",
            LogtalkParser.class,
            LogtalkWAMCompiler.class,
            LogtalkPreCompiler.class,
            LogtalkInstructionCompiler.class);;

    private final String name;
    //    public final PlPrologParser parser;
    private Class <?> parserClass;
    private Class <?> wamCompilerClass;
    private Class <?> preCompilerClass;
    private Class <?> instrCompilerClass;

    /**
     * @param name
     * @param parserClass
     * @param wamCompilerClass
     * @param preCompilerClass
     * @param instrCompilerClass
     */
    Language ( String name, Class <?> parserClass, Class <?> wamCompilerClass, Class <?> preCompilerClass, Class <?> instrCompilerClass ) {
        this.name = name;
        this.parserClass = parserClass;
        this.wamCompilerClass = wamCompilerClass;
        this.preCompilerClass = preCompilerClass;
        this.instrCompilerClass = instrCompilerClass;
    }

    /**
     * @return
     */
    public String getName () {
        return name;
    }

    public Class <?> getParserClass () {
        return parserClass;
    }

    public Class <?> getWamCompilerClass () {
        return wamCompilerClass;
    }

    public Class <?> getPreCompilerClass () {
        return preCompilerClass;
    }

    public Class <?> getInstrCompilerClass () {
        return instrCompilerClass;
    }
}
