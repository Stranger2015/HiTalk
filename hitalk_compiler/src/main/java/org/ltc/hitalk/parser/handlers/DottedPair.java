package org.ltc.hitalk.parser.handlers;

import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.ParserState;
import org.ltc.hitalk.parser.PlToken;
import org.ltc.hitalk.parser.PlToken.TokenKind;
import org.ltc.hitalk.term.IdentifiedTerm.Associativity;

import java.util.EnumSet;

public class DottedPair extends ParserStateHandler {
    protected final TokenKind lDelim/* = TK_LBRACKET*/;
    protected final TokenKind rDelim /*= TK_RBRACKET*/;

    /**
     * @param state
     * @param assocs
     * @param dks
     * @param currPriority
     * @param token
     * @throws Exception
     */
    public DottedPair(
            TokenKind lDelim,
            TokenKind rDelim,
            ParserState state,
            EnumSet<Associativity> assocs,
            EnumSet<DirectiveKind> dks,
            int currPriority,
            PlToken token) throws Exception {
        super(state, assocs, dks, currPriority, token);
        this.lDelim = lDelim;
        this.rDelim = rDelim;
    }
}
