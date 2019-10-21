package org.ltc.hitalk.parser.jp.segfault.prolog.parser;

import com.thesett.aima.logic.fol.Term;

import java.io.IOException;

/**
 *
 */
public interface TermParser<T extends Term> {

    /**
     *
     * @throws ParseException
     * @throws IOException
     * @return
     */
    T next () throws IOException, ParseException;
}
