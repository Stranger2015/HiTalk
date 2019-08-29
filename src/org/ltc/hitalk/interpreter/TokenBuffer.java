package org.ltc.hitalk.interpreter;


import com.thesett.aima.logic.fol.isoprologparser.PrologParserTokenManager;
import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.common.util.Sink;
import com.thesett.common.util.Source;
import org.ltc.hitalk.wam.compiler.HtTokenSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to buffer tokens.
 */
class TokenBuffer extends HtTokenSource implements Source <Token>, Sink <Token> {
    private List <Token> tokens = new ArrayList <>();

    /**
     * Builds a token source around the specified token manager.
     *
     * @param tokenManager The token manager to use to feed this source.
     */
    public
    TokenBuffer ( PrologParserTokenManager tokenManager ) {
        super(tokenManager, inputStream);
    }

    /**
     * @param o
     * @return
     */
    public
    boolean offer ( Token o ) {
        return tokens.add(o);
    }

    /**
     * @return
     */
    public
    Token poll () {
        return tokens.remove(0);
    }

    /**
     * @return
     */
    public
    Token peek () {
        return tokens.get(0);
    }

    /**
     *
     */
    public
    void clear () {
        tokens.clear();
    }
}
