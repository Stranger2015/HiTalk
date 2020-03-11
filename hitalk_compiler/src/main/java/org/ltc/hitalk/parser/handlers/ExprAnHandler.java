package org.ltc.hitalk.parser.handlers;

import org.ltc.hitalk.parser.IStateHandler;
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

///**
// * exprA(n) ::=
//PREPARING
// *     n > 0
// *     exprB(n)
//COMPLETING
// *    {
//            PREPARING
//          op(yfx,n) exprA(n-1) | op(yf,n)
//            COMPLETING
//      }*

// */
public class ExprAnHandler extends ParserStateHandler {
    protected IdentifiedTerm leftSide;

    /**
     * @param state
     * @param assocs
     * @param dks
     * @param currPriority
     * @param token
     */
    public ExprAnHandler(
            ParserState state,
            EnumSet<Associativity> assocs,
            EnumSet<DirectiveKind> dks,
            int currPriority,
            PlToken token) throws Exception {
        super(state, assocs, dks, currPriority, token);
    }

    public void doPrepareState(StateRecord sr) {
        IStateHandler h = sr.getCurrPriority() > 0 ?
                create(EXPR_B.getHandlerClass(),
                        EXPR_B,
                        sr.getAssocs(),
                        sr.getDks(),
                        sr.getCurrPriority(),
                        sr.getToken()) :
                create(EXPR_A0.getHandlerClass(),
                        EXPR_A0,
                        sr.getAssocs(),
                        sr.getDks(),
                        0,
                        sr.getToken());
        push(h);
    }

    public void doCompleteState(StateRecord sr) throws Exception {
        PlToken token = parser.getLexer().readToken(true);
        Set<IdentifiedTerm> ops = (token.kind == TK_ATOM) ?
                tryOperators(token.image, sr) :
                Collections.emptySet();
        for (IdentifiedTerm op : ops) {
            if (op.getPriority() == sr.getCurrPriority()) {
                switch (op.getAssociativity()) {
                    case yfx:
                        final IStateHandler h = create(EXPR_AN.getHandlerClass(),
                                EXPR_AN,
                                yfx,
                                sr.getDks(),
                                sr.getCurrPriority() - 1,
                                token);
                        push(h);
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