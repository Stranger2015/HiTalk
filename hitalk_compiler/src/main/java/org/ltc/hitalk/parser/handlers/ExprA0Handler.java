package org.ltc.hitalk.parser.handlers;


import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.IStateHandler;
import org.ltc.hitalk.parser.ParserState;
import org.ltc.hitalk.parser.PlToken;
import org.ltc.hitalk.parser.StateRecord;

import java.util.EnumSet;

import static org.ltc.hitalk.term.IdentifiedTerm.Associativity;

/**
 *
 */
public class ExprA0Handler extends ParserStateHandler {

    /**
     * @param state
     * @param assocs
     * @param dks
     * @param currPriority
     * @param token
     * @throws Exception
     */
    public ExprA0Handler(
            ParserState state,
            EnumSet<Associativity> assocs,
            EnumSet<DirectiveKind> dks,
            int currPriority,
            PlToken token) throws Exception {
        super(state, assocs, dks, currPriority, token);
    }

//    /**
//     * @param name
//     * @param sr
//     */
//    public Set<IdentifiedTerm> tryOperators(String name, StateRecord sr) {
//        return null;
//    }

    @Override
    public void doPrepareState(StateRecord sr) throws Exception {

    }

    /**
     * @param sr
     * @return
     */
    @Override
    public IStateHandler completeState(StateRecord sr) throws Exception {
        return null;
    }

    @Override
    public void doCompleteState(StateRecord sr) throws Exception {

    }
}