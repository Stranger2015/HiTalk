package org.ltc.hitalk.parser.jp.segfault.prolog.parser;

import org.ltc.hitalk.term.ITerm;

import java.io.IOException;

/**
 *
 */
public interface TermParser<T extends ITerm> {

    /**
     *
     * @throws ParseException
     * @throws IOException
     * @return
     */
    T next () throws IOException, ParseException;
}
