package org.ltc.hitalk.parser.jp.segfault.prolog.parser;

import org.ltc.hitalk.term.ITerm;

public interface ISentence<T extends ITerm> {
    /**
     * Gets the wrapped sentence in the logical language over T.
     *
     * @return The wrapped sentence in the logical language.
     */
    T getT ();
}
