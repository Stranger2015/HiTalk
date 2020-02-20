package org.ltc.hitalk.parser;

import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.PlToken.TokenKind;

import java.util.EnumSet;

import static java.util.EnumSet.of;
import static org.ltc.hitalk.parser.Directive.DirectiveKind.*;
import static org.ltc.hitalk.parser.HtPrologParser.MAX_PRIORITY;
import static org.ltc.hitalk.parser.PlToken.TokenKind.TK_BOF;
import static org.ltc.hitalk.parser.PlToken.TokenKind.TK_DOT;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity.x;

/**
 *
 */
public class StateRecord {
    ParserState state;
    EnumSet<Associativity> assocs = of(x);
    EnumSet<DirectiveKind> dks = of(DK_IF, DK_ENCODING, DK_HILOG);
    EnumSet<TokenKind> rDelims = of(TK_DOT);
    int currPriority = MAX_PRIORITY;
    PlToken token = PlToken.newToken(TK_BOF);

    public StateRecord(ParserState state,
                       EnumSet<Associativity> assocs,
                       EnumSet<DirectiveKind> dks,
                       EnumSet<TokenKind> rDelims,
                       int currPriority,
                       PlToken token) {
        this.state = state;
        this.assocs = assocs;
        this.dks = dks;
        this.rDelims = rDelims;
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

    public EnumSet<TokenKind> getrDelims() {
        return rDelims;
    }

    public int getCurrPriority() {
        return currPriority;
    }

    public PlToken getToken() {
        return token;
    }

}
