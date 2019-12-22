package org.ltc.hitalk.term.io;

import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.term.ITerm;

public class HtTermReader {

    HiTalkInputStream input;
    HtProperty[] properties;//options
    PlPrologParser parser;

    public ITerm readTerm () {
        return readTerm(input, properties);
    }

    public ITerm readTerm ( HiTalkInputStream input, HtProperty... properties ) {
        return null;
    }
}
