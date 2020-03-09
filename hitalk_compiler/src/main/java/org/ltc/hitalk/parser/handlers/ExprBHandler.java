package org.ltc.hitalk.parser.handlers;


import org.ltc.hitalk.parser.IStateHandler;
import org.ltc.hitalk.parser.ParserState;
import org.ltc.hitalk.parser.PlToken;
import org.ltc.hitalk.parser.StateRecord;
import org.ltc.hitalk.term.IdentifiedTerm;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.EnumSet.of;
import static org.ltc.hitalk.parser.Directive.DirectiveKind;
import static org.ltc.hitalk.parser.HtPrologParser.END_OF_FILE;
import static org.ltc.hitalk.parser.HtPrologParser.MIN_PRIORITY;
import static org.ltc.hitalk.parser.ParserState.EXPR_A;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity.*;

/**
 *
 */
public class ExprBHandler extends ParserStateHandler {

    public void doPrepareState(ParserState state) throws Exception {
        super.doPrepareState(state);
    }

    public void doCompleteState(PlToken token) throws Exception {
        //1. op(fx,n) exprA(n-1) | op(fy,n) exprA(n) | expr0
        IdentifiedTerm left = (IdentifiedTerm) parseLeftSide(getCurrPriority(), token);
        if (left == END_OF_FILE) {
//                lastTerm = left;
        }
        //2. left is followed by either xfx, xfy or xf operators, parse these
        token = parser.getLexer().readToken(true);
        for (; parser.isOperator(token); token = parser.getLexer().readToken(true)) {
            int priorityXFX = parser.getOptable().getPriority(token.image, xfx);
            int priorityXFY = parser.getOptable().getPriority(token.image, xfy);
            int priorityXF = parser.getOptable().getPriority(token.image, xf);
            //check that no operator has a priority higher than permitted
            //or a lower priority than the left side expression
            if (priorityXFX > getCurrPriority() || priorityXFX < MIN_PRIORITY) {
                priorityXFX = -1;
            }
            if (priorityXFY > getCurrPriority() || priorityXFY < MIN_PRIORITY) {
                priorityXFY = -1;
            }
            if (priorityXF > getCurrPriority() || priorityXF < MIN_PRIORITY) {
                priorityXF = -1;
            }
            //priorityXFX
            boolean haveAttemptedXFX = false;
            //priorityXFX has priority
            if (priorityXFX >= priorityXFY && priorityXFX >= priorityXF && priorityXFX >= left.getPriority()) {
//                            IdentifiedTerm found = exprA(priorityXFX - 1, delims);
                final IStateHandler h = ParserStateHandler.create(ExprAHandler.class);
                h.prepareState(new StateRecord(EXPR_A, of(xfx), getDks(), priorityXFX - 1, token));//fixme
//                    continue;
                if (parser.getLastTerm() != null) {
                    left = new IdentifiedTerm(
                            token.image,
                            xfx,
                            priorityXFX,
                            left.getResult(),
                            ((IdentifiedTerm) parser.getLastTerm()).getResult());
                } else {
                    haveAttemptedXFX = true;
                }
            }
            //priorityXFY //priorityXFY has priority, or priorityXFX has failed
            if ((priorityXFY >= priorityXF) && (priorityXFY >= left.getPriority())) {
                final IStateHandler h = ParserStateHandler.create(ExprAHandler.class);
                h.prepareState(new StateRecord(EXPR_A, of(xfx), getDks(), priorityXFX, token));
//                    continue;
                if (parser.getLastTerm() != null) {
                    left = new IdentifiedTerm(
                            token.image,
                            xfy,
                            priorityXFY,
                            left.getResult(),
                            ((IdentifiedTerm) parser.getLastTerm()).getResult());
                    continue;
                }
            } else
                //priorityXF      //priorityXF has priority, or priorityXFX and/or priorityXFY has failed
                if (priorityXF >= left.getPriority()) {
                    parser.setLastTerm(new IdentifiedTerm(
                            token.image,
                            xf,
                            priorityXF,
                            left.getResult()));
                }
            //2XFX did not have top priority, but priorityXFY failed
            if (!haveAttemptedXFX && priorityXFX >= left.getPriority()) {
                final IStateHandler h = ParserStateHandler.create(ExprAHandler.class);
                h.prepareState(new StateRecord(EXPR_A, of(xfx), getDks(), priorityXFX - 1, token));
//                    continue;
                if (parser.getLastTerm() != null) {
                    left = new IdentifiedTerm(
                            token.image,
                            xfx,
                            priorityXFX,
                            left.getResult(),
                            ((IdentifiedTerm) parser.getLastTerm()).getResult());
                    continue;
                }
            }
            break;
        }
    }


    public ExprBHandler(
            ParserState state,
            EnumSet<Associativity> assocs,
            EnumSet<DirectiveKind> dks,
            int currPriority,
            PlToken token) throws Exception {
        super(state, assocs, dks, currPriority, token);
    }

    /**
     * @param name
     */
    public Set<IdentifiedTerm> tryOperators(String name) {
        return null;
    }


    public void repeat(Consumer<IStateHandler> action) {

    }
}