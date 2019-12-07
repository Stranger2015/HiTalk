package org.ltc.hitalk.wam.compiler;

import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.core.BaseApp;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.IParser;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.ParseException;

import java.io.IOException;

/**
 *
 */
public class LibParser implements IParser {
    /**
     *
     */
    protected PlPrologParser parser = BaseApp.getAppContext().getParser();

    /**
     * @return
     */
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
    public HtClause parseClause () throws ParseException, IOException, SourceCodeException {
        return parser.parseClause();
    }
}
