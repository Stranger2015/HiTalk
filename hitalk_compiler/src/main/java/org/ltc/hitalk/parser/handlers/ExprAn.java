package org.ltc.hitalk.parser.handlers;

import org.ltc.hitalk.parser.ParserState;
import org.ltc.hitalk.parser.PlToken;
import org.ltc.hitalk.parser.StateRecord;
import org.ltc.hitalk.term.IdentifiedTerm;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static org.ltc.hitalk.parser.Directive.DirectiveKind;
import static org.ltc.hitalk.parser.ParserState.*;
import static org.ltc.hitalk.parser.PlToken.TokenKind.TK_ATOM;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity.yfx;

/

// */
public class ExprAn extends ParserStateHandler {

    /**
     * @param state
     * @param assocs
     * @param dks
     * @param currPriority
     * @param token
     */
    public ExprAn(
            ParserState state,
            EnumSet<Associativity> assocs,
            EnumSet<DirectiveKind> dks,
            int currPriority,
            PlToken token) throws Exception {
        super(state, assocs, dks, currPriority, token);
    }

    @Override
    public void doPrepareState(StateRecord handler) {
        if (handler.getCurrPriority() > 0) {
            create(EXPR_B.getRuleClass(),
                    EXPR_B,
                    handler.getAssocs(),
                    handler.getDks(),
                    handler.getCurrPriority(),
                    handler.getToken());
        } else {
            create(EXPR_A0.getRuleClass(),
                    EXPR_A0,
                    handler.getAssocs(),
                    handler.getDks(),
                    0,
                    handler.getToken());
        }
    }

    @Override
    public void doCompleteState(StateRecord handler) throws Exception {
        PlToken token = parser.getLexer().readToken(true);
        Set<IdentifiedTerm> ops = (token.kind == TK_ATOM) ?
                tryOperators(token.image, handler) :
                Collections.emptySet();
        for (IdentifiedTerm op : ops) {
            if (op.getPriority() == handler.getCurrPriority()) {
                switch (op.getAssociativity()) {
                    case yfx:
                        create(EXPR_AN.getRuleClass(),
                                EXPR_AN,
                                yfx,
                                handler.getDks(),
                                handler.getCurrPriority() - 1,
                                token);
                        break;
                    case yf:

                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + op.getAssociativity());
                }
                parser.setLastTerm(op);
            } else {

            }
        }
    }
}