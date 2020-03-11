package org.ltc.hitalk.parser;

import org.ltc.hitalk.parser.Directive.DirectiveKind;

import java.util.EnumSet;

import static java.util.EnumSet.of;
import static org.ltc.hitalk.parser.Directive.DirectiveKind.*;
import static org.ltc.hitalk.parser.HtPrologParser.MAX_PRIORITY;
import static org.ltc.hitalk.parser.PlToken.TokenKind.TK_BOF;
import static org.ltc.hitalk.parser.PlToken.newToken;
import static org.ltc.hitalk.parser.StateRecord.State.PREPARING;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity.x;

/**
 *
 */
public class StateRecord {
    /**
     * @param stateRecordState
     */
    public void setStateRecordState(State stateRecordState) {
        this.stateRecordState = stateRecordState;
    }

    public enum State {
        PREPARING,
        COMPLETING
    }

    protected ParserState state;

    public State getStateRecordState() {
        return stateRecordState;
    }

    protected State stateRecordState = PREPARING;
    protected EnumSet<Associativity> assocs = of(x);
    protected EnumSet<DirectiveKind> dks = of(DK_IF, DK_ENCODING, DK_HILOG);
    protected int currPriority = MAX_PRIORITY;
    protected PlToken token = newToken(TK_BOF);

    public StateRecord(ParserState state,
                       State stateRecordState,
                       EnumSet<Associativity> assocs,
                       EnumSet<DirectiveKind> dks,
                       int currPriority,
                       PlToken token) {
        this.state = state;
        this.stateRecordState = stateRecordState;
        this.assocs = assocs;
        this.dks = dks;
        this.currPriority = currPriority;
        this.token = token;
    }

    public ParserState getParserState() {
        return state;
    }

    public EnumSet<Associativity> getAssocs() {
        return assocs;
    }

    public EnumSet<DirectiveKind> getDks() {
        return dks;
    }

    public int getCurrPriority() {
        return currPriority;
    }

    public PlToken getToken() {
        return token;
    }
}
