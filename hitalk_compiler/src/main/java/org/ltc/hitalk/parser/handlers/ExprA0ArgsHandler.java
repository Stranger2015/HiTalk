package org.ltc.hitalk.parser.handlers;

import org.ltc.hitalk.parser.ParserState;
import org.ltc.hitalk.parser.PlToken;

import java.util.EnumSet;

import static org.ltc.hitalk.parser.Directive.DirectiveKind;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity;

public class ExprA0ArgsHandler extends ParserStateHandler {
    /**
     * @param state
     * @param assocs
     * @param dks
     * @param currPriority
     * @param token
     * @throws Exception
     */
    public ExprA0ArgsHandler(ParserState state,
                             EnumSet<Associativity> assocs,
                             EnumSet<DirectiveKind> dks,
                             int currPriority,
                             PlToken token) throws Exception {
        super(state, assocs, dks, currPriority, token);
    }
}
