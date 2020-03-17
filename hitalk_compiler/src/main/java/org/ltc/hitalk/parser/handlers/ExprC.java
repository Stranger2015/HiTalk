package org.ltc.hitalk.parser.handlers;

import org.ltc.hitalk.parser.ParserState;
import org.ltc.hitalk.parser.PlToken;

import java.util.EnumSet;

import static org.ltc.hitalk.parser.Directive.DirectiveKind;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity;

/**
 * exprC(n) ::=
 * // *    '-' integer | '-' float |
 * // *    op( fx,n ) exprA(n-1) |
 * // *    op( hx,n ) exprA(n-1) |
 * // *    op( fy,n ) exprA(n) |
 * // *    op( hy,n ) exprA(n) |
 * // *    exprA(n)
 */
public class ExprC extends ParserStateHandler {

    /**
     * @param state
     * @param assocs
     * @param dks
     * @param currPriority
     * @param token
     */
    public ExprC(
            ParserState state,
            EnumSet<Associativity> assocs,
            EnumSet<DirectiveKind> dks,
            int currPriority,
            PlToken token) throws Exception {
        super(state, assocs, dks, currPriority, token);
    }

//    @Override
//    public void doCompleteState(StateRecord sr) throws Exception {
//        PlToken token = parser.getLexer().readToken(true);
//        Set<IdentifiedTerm> ops = (token.kind == TK_ATOM) ?
//                tryOperators(token.image, handler) :
//                Collections.emptySet();
//        for (IdentifiedTerm op : ops) {
//            if (op.getPriority() == sr.getCurrPriority()) {
//                switch (op.getAssociativity()) {
//                    case fx:
//                        create(EXPR_AN.getRuleClass(),
//                                EXPR_AN,
//                                fx,
//                                sr.getDks(),
//                                sr.getCurrPriority() - 1,
//                                token);
//                        break;
//                    case fy:
//                        create(EXPR_AN.getRuleClass(),
//                                EXPR_AN,
//                                fy,
//                                sr.getDks(),
//                                sr.getCurrPriority(),
//                                token);
//                        break;
//                    case hx:
//                        create(EXPR_AN.getRuleClass(),
//                                EXPR_AN,
//                                hx,
//                                sr.getDks(),
//                                sr.getCurrPriority() - 1,
//                                token);
//                        break;
//                    case hy:
//                        create(EXPR_AN.getRuleClass(),
//                                EXPR_AN,
//                                hy,
//                                sr.getDks(),
//                                sr.getCurrPriority(),
//                                token);
//                        break;
//                    default:
//                        throw new IllegalStateException("Unexpected value: " + op.getAssociativity());
//                }
//            }
//            parser.setLastTerm(op);
//        }
//    }
}