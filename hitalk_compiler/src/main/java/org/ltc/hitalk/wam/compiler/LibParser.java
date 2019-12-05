package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Sentence;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.IParser;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;

public class LibParser implements IParser {
    protected PlPrologParser parser = instance().getParser(instance().getLanguage());

    @Override
    public PlPrologParser getParser () {
        return parser;
    }

    @Override
    public Language language () {
        return parser.language();
    }

    @Override
    public void initializeBuiltIns () {
        parser.initializeBuiltIns();
    }

    @Override
    public Sentence <HtClause> parseClause () {
        return parser.parseClause();
    }
}
