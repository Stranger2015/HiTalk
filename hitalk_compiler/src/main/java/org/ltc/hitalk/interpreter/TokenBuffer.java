package org.ltc.hitalk.interpreter;


import com.thesett.common.util.Sink;
import org.ltc.hitalk.parser.ITokenSource;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.parser.PlToken;
import org.ltc.hitalk.term.io.HiTalkInputStream;

import java.beans.PropertyChangeEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Used to buffer tokens.
 */
public class TokenBuffer implements ITokenSource, Sink<PlToken> {
    private Deque<PlToken> tokens = new ArrayDeque<>();
    private PlLexer lexer;

    /**
     * Builds a token source around the specified token manager.
     *
     * @param lexer
     */
    public TokenBuffer(PlLexer lexer) throws FileNotFoundException {
//        super(lexer.getInputStream());
        this.lexer = lexer;
    }

    /**
     * @param o
     * @return
     */
    @Override
    public boolean offer (PlToken o ) {
        return tokens.add(o);
    }

    /**
     * @return
     */
    @Override
    public PlToken poll () {
        return tokens.poll();
    }

    /**
     * @return
     */
    @Override
    public PlToken peek () {
        return tokens.peek();
    }

    /**
     *
     */
    public void clear() {
        tokens.clear();
    }

    /**
     * @param encoding
     * @throws IOException
     */
    public void onEncodingChanged(String encoding) throws IOException {

    }

    /**
     * @param b
     */
    public void setEncodingPermitted(boolean b) {

    }

    /**
     * @return
     */
    public boolean isEncodingPermitted() {
        return false;
    }

    /**
     * @return
     */
    public HiTalkInputStream getInputStream() {
        return null;
    }

    /**
     * @return
     */
    public boolean isOpen() {
        return false;
    }

    public String getPath() {
        return null;
    }

    public void pushBack(PlToken token) {
        tokens.push(token);
    }

    /**
     * This method gets called when a bound property is changed.
     *
     * @param evt A PropertyChangeEvent object describing the event source
     *            and the property that has changed.
     */
    public void propertyChange(PropertyChangeEvent evt) {

    }

    public boolean isEmpty() {
        return tokens.isEmpty();
    }
}
