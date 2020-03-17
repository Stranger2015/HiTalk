package org.ltc.hitalk.parser.handlers;

import org.ltc.hitalk.parser.ParserState;
import org.ltc.hitalk.parser.PlToken;

import java.util.EnumSet;

import static org.ltc.hitalk.parser.Directive.DirectiveKind;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity;

//
// exprB(n)::=
//      exprC(n-1)
//      {
//          op(xfx,n)  -> exprA(n-1) |
//          op(xfy,n) -> exprA(n) |
//          op(xf,n) -> true
//      }*
// exprC is called parseLeftSide in the code
//
public class ExprB extends ParserStateHandler {
//        create(EXPR_C.getRuleClass(),
//                EXPR_C,
//                sr.getAssocs(),
//                sr.getDks(),
//                sr.getCurrPriority() - 1,
//                sr.getToken());
//    }

//    @Override
//    public void doCompleteState(StateRecord sr) throws Exception {
//        PlToken token = parser.getLexer().readToken(true);
//        Set<IdentifiedTerm> ops = (token.kind == TK_ATOM) ?
//                tryOperators(token.image, sr) :
//                Collections.emptySet();
//        for (IdentifiedTerm op : ops) {
//            if (op.getPriority() == sr.getCurrPriority()) {
//                switch (op.getAssociativity()) {
//                    case xfx:
//                        create(new ExprAn(
//                                EXPR_AN,
//                                of(xfx),
//                                sr.getDks(),
//                                sr.getCurrPriority() - 1,
//                                token);
//                        break;
//                    case xfy:
//                        create(new ExprAn(
////                                EXPR_AN,
////                                of(xfy),
////                                sr.getDks(),
////                                sr.getCurrPriority() - 1,
////                                token);
//                        break;
//                    case xf:
//                        //fixme
//                        break;
//                    default:
//                        throw new IllegalStateException("Unexpected value: " + op.getAssociativity());
//                }
//            }
//            parser.setLastTerm(op);
//        }
//    }
//        //1. op(fx,n) exprA(n-1) | op(fy,n) exprA(n) | expr0
//        PlToken token = sr.getToken();
//        IdentifiedTerm left = (IdentifiedTerm) parseLeftSide(getCurrPriority(), token);
//        if (left == END_OF_FILE) {
//            throw new EOFException();
//        }
//        //2. left is followed by either xfx, xfy or xf operators, parse these
//        token = parser.getLexer().readToken(true);
//        for (; isOperator(token); token = parser.getLexer().readToken(true)) {
//            int priorityXFX = parser.getOptable().getPriority(token.image, xfx);
//            int priorityXFY = parser.getOptable().getPriority(token.image, xfy);
//            int priorityXF = parser.getOptable().getPriority(token.image, xf);
//            //check that no operator has a priority higher than permitted
//            //or a lower priority than the left side expression
//            if (priorityXFX > getCurrPriority() || priorityXFX < MIN_PRIORITY) {
//                priorityXFX = -1;
//            }
//            if (priorityXFY > getCurrPriority() || priorityXFY < MIN_PRIORITY) {
//                priorityXFY = -1;
//            }
//            if (priorityXF > getCurrPriority() || priorityXF < MIN_PRIORITY) {
//                priorityXF = -1;
//            }
//            //priorityXFX
//            boolean haveAttemptedXFX = false;
//            //priorityXFX has priority
//            if (priorityXFX >= priorityXFY && priorityXFX >= priorityXF && priorityXFX >= left.getPriority()) {
////                            IdentifiedTerm found = exprA(priorityXFX - 1, delims);
//                final IStateHandler h = ParserStateHandler.create(ExprAHandler.class);
////                h.prepareState(new StateRecord(EXPR_A, of(xfx), getDks(), priorityXFX - 1, token));//fixme
////                    continue;
//                if (parser.getLastTerm() != null) {
//                    left = new IdentifiedTerm(
//                            token.image,
//                            xfx,
//                            priorityXFX,
//                            left.getResult(),
//                            ((IdentifiedTerm) parser.getLastTerm()).getResult());
//                } else {
//                    haveAttemptedXFX = true;
//                }
//            }
//            //priorityXFY //priorityXFY has priority, or priorityXFX has failed
//            if ((priorityXFY >= priorityXF) && (priorityXFY >= left.getPriority())) {
//                final IStateHandler h = ParserStateHandler.create(ExprAHandler.class);
//                h.prepareState(new StateRecord(EXPR_A, of(xfx), getDks(), priorityXFX, token));
////                    continue;
//                if (parser.getLastTerm() != null) {
//                    left = new IdentifiedTerm(
//                            token.image,
//                            xfy,
//                            priorityXFY,
//                            left.getResult(),
//                            ((IdentifiedTerm) parser.getLastTerm()).getResult());
//                    continue;
//                }
//            } else
//                //priorityXF      //priorityXF has priority, or priorityXFX and/or priorityXFY has failed
//                if (priorityXF >= left.getPriority()) {
//                    parser.setLastTerm(new IdentifiedTerm(
//                            token.image,
//                            xf,
//                            priorityXF,
//                            left.getResult()));
//                }
//            //2XFX did not have top priority, but priorityXFY failed
//            if (!haveAttemptedXFX && priorityXFX >= left.getPriority()) {
//                final IStateHandler h = ParserStateHandler.create(ExprAHandler.class);
//                h.prepareState(new StateRecord(EXPR_A, of(xfx), getDks(), priorityXFX - 1, token));
////                    continue;
//                if (parser.getLastTerm() != null) {
//                    left = new IdentifiedTerm(
//                            token.image,
//                            xfx,
//                            priorityXFX,
//                            left.getResult(),
//                            ((IdentifiedTerm) parser.getLastTerm()).getResult());
//                    continue;
//                }
//            }
//            break;
//        }


    /**
     * @param state
     * @param assocs
     * @param dks
     * @param currPriority
     * @param token
     * @throws Exception
     */
    public ExprB(
            ParserState state,
            EnumSet<Associativity> assocs,
            EnumSet<DirectiveKind> dks,
            int currPriority,
            PlToken token) throws Exception {
        super(state, assocs, dks, currPriority, token);
    }
}