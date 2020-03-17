package org.ltc.hitalk.parser.handlers;

import org.ltc.hitalk.parser.ParserState;
import org.ltc.hitalk.parser.PlToken;
import org.ltc.hitalk.parser.StateRecord;

import java.util.EnumSet;

import static org.ltc.hitalk.parser.Directive.DirectiveKind;
import static org.ltc.hitalk.parser.ParserState.EXPR_AN;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity;

/**
 *
 */
public class Brace extends SimpleSeq {

    /**
     * @param state
     * @param assocs
     * @param dks
     * @param currPriority
     * @param token
     * @throws Exception
     */
    public Brace(
            ParserState state,
            EnumSet<Associativity> assocs,
            EnumSet<DirectiveKind> dks,
            int currPriority,
            PlToken token) throws Exception {
        super(state, assocs, dks, currPriority, token);
    }

    public void doPrepareState(StateRecord sr) throws Exception {
        parser.setLastTerm(null);
        create(EXPR_AN.getRuleClass(),
                EXPR_AN,
                sr.getAssocs(),
                sr.getDks(),
                1200,
                sr.getToken());
    }

    public void doCompleteState(PlToken token) throws Exception {

    }
}
