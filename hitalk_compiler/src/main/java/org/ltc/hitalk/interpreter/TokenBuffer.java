package org.ltc.hitalk.interpreter;


import com.thesett.common.util.Sink;
import com.thesett.common.util.Source;
import org.ltc.hitalk.wam.compiler.HtPrologParserTokenManager;
import org.ltc.hitalk.wam.compiler.HtToken;
import org.ltc.hitalk.wam.compiler.HtTokenSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Used to buffer tokens.
 */
public class TokenBuffer extends HtTokenSource implements Source <HtToken>, Sink <HtToken> {
    private List <HtToken> tokens = new ArrayList <>();

    /**
     * Builds a token source around the specified token manager.
     *
     * @param tokenManager The token manager to use to feed this source.
     * @param input
     */
    public TokenBuffer ( HtPrologParserTokenManager tokenManager, InputStream input ) throws IOException {
        super(tokenManager, input);
    }

    /**
     * @param o
     * @return
     */
    public boolean offer ( HtToken o ) {
        return tokens.add(o);
    }

    /**
     * @return
     */
    public HtToken poll () {
        return tokens.remove(0);
    }

    /**
     * @return
     */
    public HtToken peek () {
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
