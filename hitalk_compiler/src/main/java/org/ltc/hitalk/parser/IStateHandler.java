package org.ltc.hitalk.parser;

import org.ltc.hitalk.term.IdentifiedTerm;

import java.util.Deque;
import java.util.Set;
import java.util.function.Consumer;

import static org.ltc.hitalk.core.BaseApp.appContext;

/**
 *
 */
public interface IStateHandler {
    StateRecord newState();

    /**
     * @return
     */
    StateRecord getStateRecord();

    /**
     * @return
     * @throws Exception
     */
    default Deque<StateRecord> getStates() throws Exception {
        return ((HtPrologParser) appContext.getParser()).states;
    }

    /**
     *
     */
    default void prepareState() throws Exception {
        getStates().push(getStateRecord());
    }

    /**
     *
     */
    default void completeState() throws Exception {
        getStates().pop();
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
