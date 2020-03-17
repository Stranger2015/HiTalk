package org.ltc.hitalk.parser.rules;

import org.ltc.hitalk.parser.ParserState;

public class RuleInterpreterVisitor {
    public void apply(ParserState head, Object... params) throws ReflectiveOperationException {
        head.getRuleClass().getConstructor().newInstance(params);

    }
}