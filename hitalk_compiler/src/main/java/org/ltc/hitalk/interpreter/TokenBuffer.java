package org.ltc.hitalk.interpreter;

import org.ltc.hitalk.parser.PlToken;
import org.ltc.hitalk.term.io.HiTalkInputStream;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.ltc.hitalk.parser.HtPrologParser.END_OF_FILE_STRING;
import static org.ltc.hitalk.parser.PlToken.TokenKind.TK_ATOM;

/**
 * Used to buffer tokens.
 */
public class TokenBuffer/* extends PlLexer implements Sink<PlToken>*/ {
    private final HiTalkInputStream stream;
    //    private final String path;
    private Deque<PlToken> tokens = new ArrayDeque<>();
    private Deque<PlToken> history = new ArrayDeque<>();
    private Deque<PlTokenStatus> statuses = new ArrayDeque<PlTokenStatus>();

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
     * @deprecated As of JDK version 1.1, the preferred way to tokenize an
     * input stream is to convert it into a character stream, for example:
     * <blockquote><pre>
     *   Reader r = new BufferedReader(new InputStreamReader(is));
     *   StreamTokenizer st = new StreamTokenizer(r);
     * </pre></blockquote>
     */
    public TokenBuffer(HiTalkInputStream inputStream) {
        this.stream = inputStream;
        pushBack(PlToken.newToken(TK_ATOM, END_OF_FILE_STRING));
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
        final PlToken t = tokens.poll();
        //  history.push(PlToken.newToken(t.kind));
        // statuses.push(POLLED);
        return t;
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


    /**
     * @param token
     */
    public void pushBack(PlToken token) {
        tokens.push(token);
//        history.push(token);
//        statuses.push(PUSHED_BACK);
    }

    /**
     * @return
     */
    public boolean isEmpty() {
        return tokens.isEmpty();
    }
}
