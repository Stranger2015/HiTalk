package org.ltc.hitalk.parser.handlers;

import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.ParserState;
import org.ltc.hitalk.parser.PlToken;
import org.ltc.hitalk.parser.PlToken.TokenKind;
import org.ltc.hitalk.parser.StateRecord;
import org.ltc.hitalk.term.IdentifiedTerm.Associativity;

import java.util.EnumSet;

import static org.ltc.hitalk.parser.ParserState.LIST_SEQUENCE;
import static org.ltc.hitalk.parser.PlToken.TokenKind.TK_LBRACKET;
import static org.ltc.hitalk.parser.PlToken.TokenKind.TK_RBRACKET;

public class List extends ParserStateHandler {
    protected TokenKind lDelim = TK_LBRACKET;
    protected TokenKind rDelim = TK_RBRACKET;

    /**
     * @param state
     * @param assocs
     * @param dks
     * @param currPriority
     * @param token
     * @throws Exception
     */
    public List(
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

    public void doPrepareState(StateRecord sr) throws Exception {

        PlToken token = parser.getLexer().readToken(true);
        if (token.kind == lDelim) {
            create(LIST_SEQUENCE.getRuleClass(),
                    LIST_SEQUENCE,
                    sr.getAssocs(),
                    sr.getDks(),
                    sr.getCurrPriority(),
                    sr.getToken());
        }
    }

    public void doCompleteState(StateRecord sr) throws Exception {

    }
}
