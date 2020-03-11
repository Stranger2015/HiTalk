package org.ltc.hitalk.parser;

import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.term.IdentifiedTerm;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static org.ltc.hitalk.term.IdentifiedTerm.Associativity;

/**
 *
 */
public interface IStateHandler {
    /**
     * @return
     */
    HtPrologParser getParser();

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

    IStateHandler handleState(StateRecord sr) throws Exception;

//    /**
//     * @return
//     */
//    default void prepareState(ParserState state) throws Exception {
//        push(this);
//        doPrepareState(state);
//    }


    default void prepareState(StateRecord sr) throws Exception {
        doPrepareState(sr);
    }

    void doPrepareState(StateRecord sr) throws Exception;

    /**
     * @return
     */
    default IStateHandler completeState(StateRecord sr) throws Exception {
        doCompleteState(sr);
        return pop();
    }

    void doCompleteState(StateRecord sr) throws Exception;

    /**
     * @return
     */
    ParserState getParserState();

    /**
     *
     */
    default Set<IdentifiedTerm> tryOperators(String name, StateRecord sr) {
        final Set<IdentifiedTerm> result = new HashSet<>();
        for (Associativity assoc : sr.getAssocs()) {
            result.addAll(getParser().getOptable().getOperators(name, assoc, sr.getCurrPriority()));
        }

        return result;
    }

    /**
     * @param action
     */
    default void repeat(Consumer<IStateHandler> action) {
        action.accept((IStateHandler) getStateRecord());
    }

    void setCurrPriority(int currPriority);

    void setToken(PlToken token);

    void setDks(EnumSet<DirectiveKind> dks);
}
