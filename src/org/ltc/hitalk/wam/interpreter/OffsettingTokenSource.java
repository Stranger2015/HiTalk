package org.ltc.hitalk.wam.interpreter;

import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.aima.logic.fol.isoprologparser.TokenSource;
import com.thesett.common.util.Source;

public
class OffsettingTokenSource implements Source <Token> {
    public
    OffsettingTokenSource ( TokenSource tokenSource, int lineNo ) {
    }

    /**
     * Retrieves and removes the head of this queue, or <tt>null</tt> if this queue is empty.
     *
     * @return The head of this queue, or <tt>null</tt> if this queue is empty.
     */
    @Override
    public
    Token poll () {
        return null;
    }

    /**
     * Retrieves, but does not remove, the head of this queue, returning <tt>null</tt> if this queue is empty.
     *
     * @return The head of this queue, or <tt>null</tt> if this queue is empty.
     */
    @Override
    public
    Token peek () {
        return null;
    }
}
