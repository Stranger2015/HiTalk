package org.ltc.hitalk.interpreter;


import org.ltc.hitalk.parser.PlToken;
import org.ltc.hitalk.term.io.HiTalkInputStream;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Used to buffer tokens.
 */
public class TokenBuffer/* extends PlLexer implements Sink<PlToken>*/ {
    private Deque<PlToken> tokens = new ArrayDeque<>();

    /**
     * Creates a stream tokenizer that parses the specified input
     * stream. The stream tokenizer is initialized to the following
     * default state:
     * <ul>
     * <li>All byte values {@code 'A'} through {@code 'Z'},
     *     {@code 'a'} through {@code 'z'}, and
     *     {@code '\u005Cu00A0'} through {@code '\u005Cu00FF'} are
     *     considered to be alphabetic.
     * <li>All byte values {@code '\u005Cu0000'} through
     *     {@code '\u005Cu0020'} are considered to be white space.
     * <li>{@code '/'} is a comment character.
     * <li>Single quote {@code '\u005C''} and double quote {@code '"'}
     *     are string quote characters.
     * <li>Numbers are parsed.
     * <li>Ends of lines are treated as white space, not as separate tokens.
     * <li>C-style and C++-style comments are not recognized.
     * </ul>
     *
     * @param is an input stream.
     * @deprecated As of JDK version 1.1, the preferred way to tokenize an
     * input stream is to convert it into a character stream, for example:
     * <blockquote><pre>
     *   Reader r = new BufferedReader(new InputStreamReader(is));
     *   StreamTokenizer st = new StreamTokenizer(r);
     * </pre></blockquote>
     */
    public TokenBuffer(InputStream is) throws FileNotFoundException {
    }

    /**
     * Create a tokenizer that parses the given character stream.
     *
     * @param r a Reader object providing the input stream.
     * @since JDK1.1
     */
    public TokenBuffer(Reader r) throws FileNotFoundException {
    }

    public TokenBuffer(HiTalkInputStream stream, String path) throws FileNotFoundException {
    }

    /**
     * @param inputStream
     * @throws FileNotFoundException
     */
    public TokenBuffer(HiTalkInputStream inputStream) throws FileNotFoundException {
    }

    /**
     * @param o
     * @return
     */
    public boolean offer(PlToken o) {
        return tokens.add(o);
    }

    /**
     * @return
     */
    public PlToken poll () {
        return tokens.poll();
    }

    /**
     * @return
     */
    public PlToken peek() {
        return tokens.peek();
    }

    /**
     *
     */
    public void clear() {
        tokens.clear();
    }


    public void pushBack(PlToken token) {
        tokens.push(token);
    }

    public boolean isEmpty() {
        return tokens.isEmpty();
    }
}
