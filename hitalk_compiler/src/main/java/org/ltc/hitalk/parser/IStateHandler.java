package org.ltc.hitalk.parser;

import org.ltc.hitalk.term.IdentifiedTerm;

import java.util.Set;
import java.util.function.Consumer;

/**
 *
 */
public interface IStateHandler {
    /**
     * @return
     */
    StateRecord newState();

    /**
     * @return
     */
    StateRecord getStateRecord();

    /**
     * @param handler
     */
    void push(IStateHandler handler);

    /**
     * @return
     */
    IStateHandler pop();

    IStateHandler handleState() throws Exception;

    /**
     *
     * @return
     */
    default IStateHandler prepareState() throws Exception {
        push(this);
        return ParserStateHandler.create(getStateRecord());
    }

    /**
     * @return
     */
    default IStateHandler completeState() throws Exception {
        return pop();
    }

    /**
     * @return
     */
    ParserState getParserState();

    /**
     *
     */
    Set<IdentifiedTerm> tryOperators(String name);

    /**
     * @param action
     */
    void repeat(Consumer<IStateHandler> action);
}
