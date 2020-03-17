package org.ltc.hitalk.parser.handlers;

import org.ltc.hitalk.parser.ParserState;
import org.ltc.hitalk.parser.PlToken;
import org.ltc.hitalk.parser.StateRecord;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;

import java.util.EnumSet;

import static org.ltc.hitalk.parser.Directive.DirectiveKind;
import static org.ltc.hitalk.parser.ParserState.EXPR_AN;
import static org.ltc.hitalk.parser.ParserState.HEADS;
import static org.ltc.hitalk.parser.PlToken.TokenKind.TK_COMMA;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity;

/**
 *
 */
public class HeadsHandler extends ParserStateHandler {
    ListTerm listTerm = new ListTerm(0);

    /**
     * @param state
     * @param assocs
     * @param dks
     * @param currPriority
     * @param token
     */
    public HeadsHandler(
            ParserState state,
            EnumSet<Associativity> assocs,
            EnumSet<DirectiveKind> dks,
            int currPriority,
            PlToken token) throws Exception {
        super(state, assocs, dks, currPriority, token);
    }

    @Override
    public void doPrepareState(StateRecord sr) throws Exception {
        parser.setLastTerm(null);
        create(EXPR_AN.getRuleClass(),
                EXPR_AN,
                sr.getAssocs(),
                sr.getDks(),
                1200,
                sr.getToken());
    }

    @Override
    public void doCompleteState(StateRecord sr) throws Exception {
        final ITerm lt = parser.getLastTerm();
        if (lt == null) {
            return;
        }

        PlToken token = parser.getLexer().readToken(true);
        if (token.kind == TK_COMMA) {
            create(HEADS.getHandlerClass(),
                    HEADS,
                    sr.getAssocs(),
                    sr.getDks(),
                    sr.getCurrPriority(),
                    sr.getToken());
        }
    }
}