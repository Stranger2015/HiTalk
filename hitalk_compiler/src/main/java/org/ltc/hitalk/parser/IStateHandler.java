package org.ltc.hitalk.parser;

import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.term.IdentifiedTerm;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 *
 */
public interface IStateHandler {

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

    /**
     * @return
     * @throws Exception
     */
    IStateHandler handleState(PlToken token) throws Exception;

    /**
     * @return
     */
    default void prepareState(ParserState state) throws Exception {
        push(this);
        doPrepareState(state);
    }

    default void prepareState(StateRecord sr) throws Exception {
        prepareState(sr.getParserState());
    }

    void doPrepareState(ParserState state) throws Exception;

    default void doPrepareState(StateRecord sr) throws Exception {
        doPrepareState(sr.getParserState());
    }

    /**
     * @return
     */
    default IStateHandler completeState(PlToken token) throws Exception {
        doCompleteState(token);
        return pop();
    }

    void doCompleteState(PlToken token) throws Exception;

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

    void setCurrPriority(int currPriority);

    void setToken(PlToken token);

    void setDks(EnumSet<DirectiveKind> dks);
}
