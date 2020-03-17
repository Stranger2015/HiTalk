package org.ltc.hitalk.parser.handlers;

import org.ltc.hitalk.parser.ParserState;
import org.ltc.hitalk.parser.PlToken;

import java.util.EnumSet;

import static org.ltc.hitalk.parser.Directive.DirectiveKind;
import static org.ltc.hitalk.parser.PlToken.TokenKind.TK_LPAREN;
import static org.ltc.hitalk.parser.PlToken.TokenKind.TK_RPAREN;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity;

public class Args extends List {
    /**
     * @param state
     * @param assocs
     * @param dks
     * @param currPriority
     * @param token
     * @throws Exception
     */
    public Args(ParserState state,
                EnumSet<Associativity> assocs,
                EnumSet<DirectiveKind> dks,
                int currPriority,
                PlToken token) throws Exception {
        super(TK_LPAREN, TK_RPAREN, state, assocs, dks, currPriority, token);
    }
}
