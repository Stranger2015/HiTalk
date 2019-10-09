package org.ltc.hitalk.parser.jp.segfault.prolog.parser;

import com.thesett.aima.logic.fol.Term;

import java.io.IOException;

/**
 * Prologテキストを読み込んで、項に変換するパーサーです。
 *
 * @author shun
 */
public interface TermParser<T extends Term> {

    /**
     * ストリーム上の次の項を解析して返します。
     *
     * @throws ParseException
     * @throws IOException
     * @return
     */
    T next () throws IOException, ParseException;
}
