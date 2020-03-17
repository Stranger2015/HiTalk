package org.ltc.hitalk.parser.rules;

import org.ltc.hitalk.parser.ParserState;

public class Rule {
    protected ParserState head;
    protected ParserState[] body;

    public Rule(ParserState head, ParserState... body) {
        this.head = head;
        this.body = body;
    }
}
