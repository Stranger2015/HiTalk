package org.ltc.hitalk.interpreter;

import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.IParser;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.ParseException;
import org.ltc.hitalk.wam.compiler.Language;

import java.io.IOException;

/**
 *
 */
public class InteractiveParser implements IParser {
    protected PlPrologParser parser;

    /**
     * @param parser
     */
    public InteractiveParser ( PlPrologParser parser ) {
        this.parser = parser;
    }

    /**
     *
     */
    public InteractiveParser () {
        super();
    }

    /**
     * @return
     */
    @Override
    public Language language () {
        return parser.language();
    }

    /**
     *
     */
    @Override
    public void initializeBuiltIns () {
        parser.initializeBuiltIns();
    }

    /**
     * @return
     */
    @Override
    public HtClause parseClause () throws ParseException, IOException, SourceCodeException {
        return parser.parseClause();
    }

    /**
     * @return
     */
    public PlPrologParser getParser () {
        return parser;
    }
}
