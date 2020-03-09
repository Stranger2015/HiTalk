package org.ltc.hitalk.parser.handlers;

import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.parser.*;
import org.ltc.hitalk.term.IdentifiedTerm;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

import static org.ltc.hitalk.core.BaseApp.appContext;
import static org.ltc.hitalk.parser.Directive.DirectiveKind;
import static org.ltc.hitalk.parser.HtPrologParser.MIN_PRIORITY;
import static org.ltc.hitalk.parser.ParserState.EXPR_B;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity.yf;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity.yfx;

/**
 *
 */
public class ExprAnHandler extends ParserStateHandler {
    IdentifiedTerm leftSide;

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

    public IStateHandler doPrepareState() {
        final StateRecord sr = getStateRecord(EXPR_B);
        return create(EXPR_B, sr);
    }

    private StateRecord getStateRecord(ParserState state) {

        return null;
    }

    private IStateHandler create(ParserState state, StateRecord stateRecord) {

        return null;
    }

    public void doPrepareState(ParserState state) throws Exception {
        super.doPrepareState(state);
    }

    public void doCompleteState(PlToken token) throws Exception {
        PlLexer lexer = appContext.getTokenSource();
        if (token == null) {
            token = lexer.readToken(true);
        }
        IOperatorTable operatorTable = appContext.getOpTable();
        for (; isOperator(token); token = lexer.readToken(true)) {
            int priorityYFX = operatorTable.getPriority(token.image, yfx);
            int priorityYF = operatorTable.getPriority(token.image, yf);
            //YF and priorityYFX has a higher priority than the left side expr and less then top limit
            // if (YF < leftSide.getPriority() && YF > PlDynamicOperatorParser.OP_HIGH) YF = -1;

            if (priorityYF < leftSide.getPriority() || priorityYF > getCurrPriority()) {
                priorityYF = -1;
            }
            // if (priorityYFX < leftSide.getPriority() && priorityYFX > MAX_PRIORITY) priorityYFX = -1;
            if (priorityYFX < leftSide.getPriority() || priorityYFX > getCurrPriority()) {
                priorityYFX = -1;
            }
            //priorityYFX has getPriority() over YF
            if (priorityYFX >= priorityYF && priorityYFX >= MIN_PRIORITY) {
//                    IStateHandler h=() ;
//                    handlerAn.prepareState();
//                            of(x),
//                            dks,
//                            priorityYFX - 1,
//                            token);
                if (parser.getLastTerm() != null) {
                    leftSide = new IdentifiedTerm(
                            token.image,
                            yfx,
                            priorityYFX,
                            leftSide.getResult(),
                            ((IdentifiedTerm) parser.getLastTerm()).getResult());
                }
            } else {
                //either YF has priority over priorityYFX or priorityYFX failed
                if (priorityYF >= MIN_PRIORITY) {
                    leftSide = new IdentifiedTerm(
                            token.image,
                            yf,
                            priorityYF,
                            leftSide.getResult());
                }
            }
        }
    }

    /**
     * @param name
     */
    public Set<IdentifiedTerm> tryOperators(String name) {
        return null;
    }

    /**
     * @param action
     */
    public void repeat(Consumer<IStateHandler> action) {

    }

    /**
     * @param token
     * @return
     */
    private boolean isOperator(PlToken token) {
        final String name = token.getImage();
        final Set<IdentifiedTerm> ops = appContext.getOpTable().getOperators(name);

        return ops.stream().anyMatch(op -> op.getTextName().equals(name));
    }

}