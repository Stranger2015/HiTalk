package org.ltc.hitalk.wam.interpreter;


import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.common.util.Sink;
import com.thesett.common.util.Source;

import java.util.LinkedList;

/**
 * Used to buffer tokens.
 */

class TokenBuffer implements Source <Token>, Sink <Token> {
    private LinkedList <Token> tokens = new LinkedList <>();

    public
    boolean offer ( Token o ) {
        return tokens.offer(o);
    }

    public
    Token poll () {
        return tokens.poll();
    }

    public
    Token peek () {
        return tokens.peek();
    }

    public
    void clear () {
        tokens.clear();
    }
}
