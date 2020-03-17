package org.ltc.hitalk.parser;

import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.term.IdentifiedTerm;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.ltc.hitalk.term.IdentifiedTerm.Associativity;

/**
 *
 */
public interface IStateHandler {
    /**
     * @return
     */
    boolean isPushed();

    /**
     * @return
     */
    HtPrologParser getParser();

//    /**
//     * @return
//     */
//    StateRecord getStateRecord();

    /**
     * @param handler
     */
    void push(IStateHandler handler);

    /**
     * @return
     */
    IStateHandler pop();

//    default IStateHandler handleState(IStateHandler handler) throws Exception {
//        return null;
//    }

//    default void prepareState(StateRecord sr, IStateHandler result) throws Exception {
//        if (result != null && !isPushed()) {
//            push(result);
//        }
//        doPrepareState(sr);
//    }

//    void doPrepareState(StateRecord sr) throws Exception;

//    /**
//     * @return
//     */
//    default IStateHandler completeState(StateRecord sr) throws Exception {
//        doCompleteState(sr);
//        return pop();//fixme
//    }

//    void doCompleteState(StateRecord sr) throws Exception;

    /**
     * @return
     */
    ParserState getParserState();

    /**
     *
     */
    default Set<IdentifiedTerm> tryOperators(String name, IStateHandler handler) {
        final Set<IdentifiedTerm> result = new HashSet<>();
        for (Associativity assoc : handler.getAssocs()) {
            result.addAll(getParser().getOptable().getOperators(name, assoc, handler.getCurrPriority()));
        }

        return result;
    }

    /**
     * @return
     */
    EnumSet<Associativity> getAssocs();

    /**
     * @param action
     */
//    default void repeat(Consumer<IStateHandler> action) {
//        action.accept((IStateHandler) getStateRecord());
//    }

    /**
     * @param currPriority
     */
    void setCurrPriority(int currPriority);

    /**
     * @param token
     */
    void setToken(PlToken token);

    /**
     * @param dks
     */
    void setDks(EnumSet<DirectiveKind> dks);

    /**
     * @return
     */
    int getCurrPriority();

    /**
     * @return
     */
    EnumSet<DirectiveKind> getDks();

    /**
     * @return
     */
    PlToken getToken();
}
