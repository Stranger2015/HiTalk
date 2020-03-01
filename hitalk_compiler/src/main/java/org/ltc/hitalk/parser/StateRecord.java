package org.ltc.hitalk.parser;

import org.ltc.hitalk.parser.Directive.DirectiveKind;

import java.util.EnumSet;
import java.util.function.Predicate;

import static java.util.EnumSet.of;
import static org.ltc.hitalk.parser.Directive.DirectiveKind.*;
import static org.ltc.hitalk.parser.HtPrologParser.MAX_PRIORITY;
import static org.ltc.hitalk.parser.ParserState.START;
import static org.ltc.hitalk.parser.PlToken.TokenKind.TK_BOF;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity.x;

/**
 *
 */
public class StateRecord {
    ParserState state = START;
    EnumSet<Associativity> assocs = of(x);
    EnumSet<DirectiveKind> dks = of(DK_IF, DK_ENCODING, DK_HILOG);
    Predicate<IStateHandler> rDelimCondition;
    int currPriority = MAX_PRIORITY;
    PlToken token = PlToken.newToken(TK_BOF);

    protected StateRecord(ParserState state,
                          EnumSet<Associativity> assocs,
                          EnumSet<DirectiveKind> dks,
                          Predicate<IStateHandler> rDelimCondition,
                          int currPriority,
                          PlToken token) {
        this.state = state;
        this.assocs = assocs;
        this.dks = dks;
        this.rDelimCondition = rDelimCondition;
        this.currPriority = currPriority;
        this.token = token;
    }

    /**
     *
     */
    protected StateRecord() {

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

    public Predicate<IStateHandler> getrDelimCondition() {
        return rDelimCondition;
    }

    public int getCurrPriority() {
        return currPriority;
    }

    public PlToken getToken() {
        return token;
    }

}
