package org.ltc.hitalk.parser.handlers;

import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.ParserState;
import org.ltc.hitalk.parser.PlToken;
import org.ltc.hitalk.parser.StateRecord;

import java.util.EnumSet;

import static org.ltc.hitalk.term.IdentifiedTerm.Associativity;

/**
 *
 */
public class ExprA0BracketHandler extends ParserStateHandler {

    @Override
    public void doPrepareState(StateRecord sr) throws Exception {
        super.doPrepareState(sr);
    }

    @Override
    public void doCompleteState(StateRecord sr) throws Exception {
        super.doCompleteState(sr);
    }

    /**
     * @param state
     * @param assocs
     * @param dks
     * @param currPriority
     * @param token
     * @throws Exception
     */
    public ExprA0BracketHandler(
            ParserState state,
            EnumSet<Associativity> assocs,
            EnumSet<DirectiveKind> dks,
            int currPriority,
            PlToken token) throws Exception {
        super(state, assocs, dks, currPriority, token);
    }
}