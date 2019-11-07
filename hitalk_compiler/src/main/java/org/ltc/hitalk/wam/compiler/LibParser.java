package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.IParser;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.term.io.Environment;

public class LibParser implements IParser {
    protected PlPrologParser parser = Environment.instance().getParser();

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

    @Override
    public HtClause convert ( Term t ) {
        return null;
    }
}
