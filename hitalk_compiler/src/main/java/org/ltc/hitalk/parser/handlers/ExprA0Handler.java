package org.ltc.hitalk.parser.handlers;


import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.IStateHandler;
import org.ltc.hitalk.parser.ParserState;
import org.ltc.hitalk.parser.PlToken;

import java.util.EnumSet;
import java.util.function.Consumer;

import static org.ltc.hitalk.term.IdentifiedTerm.Associativity;

/**
 *
 */
public class ExprA0Handler extends ExprAHandler {

    public ExprA0Handler(
            ParserState state,
            EnumSet<Associativity> assocs,
            EnumSet<DirectiveKind> dks,
            int currPriority,
            PlToken token) throws Exception {
        super(state, assocs, dks, currPriority, token);
    }

    public void repeat(Consumer<IStateHandler> action) {

    }

    public void doCompleteState(PlToken token) throws Exception {
        super.doCompleteState(token);
    }

    public void doPrepareState(ParserState state) throws Exception {
        super.doPrepareState(state);
    }
}