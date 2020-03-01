package org.ltc.hitalk.parser;

import java.util.function.Consumer;

/**
 *
 */
public interface IStateHandler {

    /**
     * @param state
     */
    void enterState(ParserState state);

    /**
     * @param state
     */
    void exitState(ParserState state);

    /**
     * @return
     */
    ParserState getParserState();

    /**
     *
     */
    void tryOperators();

    /**
     * @param action
     */
    void repeat(Consumer<IStateHandler> action);
}
