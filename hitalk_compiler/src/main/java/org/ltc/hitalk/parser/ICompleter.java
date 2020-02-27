package org.ltc.hitalk.parser;

/**
 *
 */
public interface ICompleter {
    /**
     *
     */
    void complete();

    /**
     * @return
     */
    ParserState getState();

    /**
     *
     */
    void tryOperators();
}
