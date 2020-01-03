package org.ltc.hitalk.interpreter;


import com.thesett.common.util.Sink;
import com.thesett.common.util.Source;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.parser.PlToken;
import org.ltc.hitalk.parser.PlTokenSource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Used to buffer tokens.
 */
public class TokenBuffer extends PlTokenSource implements Source <PlToken>, Sink <PlToken> {
    private List <PlToken> tokens = new ArrayList <>();

    /**
     * Builds a token source around the specified token manager.
     *
     * @param lexer
     */
    public TokenBuffer ( PlLexer lexer ) throws FileNotFoundException {
        super(lexer.getInputStream());
    }

    /**
     * @param o
     * @return
     */
    public boolean offer ( PlToken o ) {
        return tokens.add(o);
    }

    /**
     * @return
     */
    public PlToken poll () {
        return tokens.remove(0);
    }

    /**
     * @return
     */
    public PlToken peek () {
        return tokens.get(0);
    }

    /**
     *
     */
    public void clear () {
        tokens.clear();
    }

    public void close () throws IOException {
        super.close();
    }
}
