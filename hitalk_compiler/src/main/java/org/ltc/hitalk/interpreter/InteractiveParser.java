package org.ltc.hitalk.interpreter;

import org.ltc.hitalk.parser.IParser;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;

public class InteractiveParser implements IParser {
    protected PlPrologParser parser;

    public InteractiveParser ( PlPrologParser parser ) {
        this.parser = parser;
    }

    public InteractiveParser () {
        super();
    }


    @Override
    public String language () {
        return parser.language();
    }

    @Override
    public void initializeBuiltIns () {
        parser.initializeBuiltIns();
    }

    public PlPrologParser getParser () {
        return parser;
    }
}
