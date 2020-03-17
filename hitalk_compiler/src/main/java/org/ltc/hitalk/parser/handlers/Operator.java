package org.ltc.hitalk.parser.handlers;

import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.ParserState;
import org.ltc.hitalk.parser.PlToken;
import org.ltc.hitalk.term.IdentifiedTerm.Associativity;

import java.util.EnumSet;

import static java.util.EnumSet.of;
import static org.ltc.hitalk.parser.Directive.DirectiveKind.DK_IF;
import static org.ltc.hitalk.parser.ParserState.OP;
import static org.ltc.hitalk.parser.PlToken.TokenKind.TK_ATOM;

public class Operator extends ParserStateHandler {
    protected String name;
    protected Associativity assoc;
    protected int priority;

    public Operator(String name, Associativity assoc, int priority) throws Exception {
        this(OP, name, of(assoc), of(DK_IF), priority, PlToken.newToken(TK_ATOM));
    }

    public Operator(ParserState state,
                    String name,
                    EnumSet<Associativity> assocs,
                    EnumSet<DirectiveKind> dks,
                    int currPriority,
                    PlToken token) throws Exception {
        super(state, assocs, dks, currPriority, token);
        this.name = name;
    }

    public boolean test() {
        return false;
    }

    public String getName() {
        return name;
    }

    public Associativity getAssoc() {
        return assoc;
    }

    public int getPriority() {
        return priority;
    }
}
