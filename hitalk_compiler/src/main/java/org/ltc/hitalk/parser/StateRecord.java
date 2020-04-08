package org.ltc.hitalk.parser;

import org.ltc.hitalk.parser.Directive.DirectiveKind;

import java.util.EnumSet;

import static java.util.EnumSet.of;
import static org.ltc.hitalk.parser.Directive.DirectiveKind.*;
import static org.ltc.hitalk.parser.HtPrologParser.MAX_PRIORITY;
import static org.ltc.hitalk.parser.PlToken.TokenKind.TK_BOF;
import static org.ltc.hitalk.parser.PlToken.newToken;
import static org.ltc.hitalk.term.OpSymbolFunctor.Associativity;
import static org.ltc.hitalk.term.OpSymbolFunctor.Associativity.x;

/**
 *
 */
public class StateRecord {
    protected ParserState state;
    protected EnumSet<Associativity> assocs = of(x);
    protected EnumSet<DirectiveKind> dks = of(DK_IF, DK_ENCODING, DK_HILOG);
    protected int currPriority = MAX_PRIORITY;
    protected PlToken token = newToken(TK_BOF);

    public ParserState getState() {
        return state;
    }

    public StateRecord(ParserState state,
                       EnumSet<Associativity> assocs,
                       EnumSet<DirectiveKind> dks,
                       int currPriority,
                       PlToken token) {
        this.state = state;
        this.assocs = assocs;
        this.dks = dks;
        this.currPriority = currPriority;
        this.token = token;
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

    public String toString() {
        final StringBuilder sb = new StringBuilder("StateRecord{");
        sb.append("state=").append(state);
        sb.append(", assocs=").append(assocs);
        sb.append(", dks=").append(dks);
        sb.append(", currPriority=").append(currPriority);
        sb.append(", token=").append(token);
        sb.append('}');
        return sb.toString();
    }
}
