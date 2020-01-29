package org.ltc.hitalk.parser;

import static org.ltc.hitalk.parser.PlToken.TokenKind.TK_BOF;

/**
 * Describes the input token stream.
 */
public class PlToken implements ISourceCodePosition {

    /**
     * The string image of the token.
     */
    public String image;

    @Override
    public void setBeginLine(int beginLine) {
        this.beginLine = beginLine;
    }

    @Override
    public void setBeginColumn(int beginColumn) {
        this.beginColumn = beginColumn;
    }

    /**
     * @param endLine
     */
    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    /**
     * @param endColumn
     */
    @Override
    public void setEndColumn(int endColumn) {
        this.endColumn = endColumn;
    }

    /**
     * @return
     */
    @Override
    public int getBeginLine() {
        return beginLine;
    }

    /**
     * @return
     */
    @Override
    public int getBeginColumn() {
        return beginColumn;
    }

    /**
     * @return
     */
    @Override
    public int getEndLine() {
        return endLine;
    }

    /**
     * @return
     */
    @Override
    public int getEndColumn() {
        return endColumn;
    }

    /**
     * @param kind
     */
    public PlToken(TokenKind kind) {
        this.kind = kind;
        image = kind.getImage();
        if (image != null && !image.isEmpty()) {
            endColumn = beginColumn + image.length();
        }
        if (kind == TK_BOF) {
            endLine = beginLine = 0;
            endColumn = beginColumn = 0;
        }
    }

    /**
     * @param kind
     * @param image
     */
    public PlToken(TokenKind kind, String image) {
        this(kind);
        this.image = image;
    }

    /**
     * @return
     */
    public String getImage() {
        return image;
    }

    /**
     * @return
     */

    public boolean isNumber() {
        return false;
    }

    /**
     *
     */
    public enum TokenKind {
        TK_BOF(""),
        TK_EOF(""),
        TK_DOT("."),
        TK_LPAREN("("),
        TK_RPAREN(")", "right parenthesis"),
        TK_LBRACKET("["),
        TK_RBRACKET("]", "right bracket"),
        TK_LBRACE("{"),
        TK_RBRACE("}", "right brace"),

        TK_D_QUOTE("\""),
        TK_S_QUOTE("'"),
        TK_B_QUOTE("`"),
        TK_INTEGER_LITERAL(""),
        TK_DECIMAL_LITERAL(""),
        TK_HEX_LITERAL(""),
        TK_FLOATING_POINT_LITERAL(""),
        TK_DECIMAL_EXPONENT(""),
        TK_CHARACTER_LITERAL(""),
        TK_STRING_LITERAL(""),
        TK_VAR(""),
        TK_FUNCTOR_BEGIN(""),
        TK_ATOM(""),
        TK_QUOTED_NAME(""),
        TK_SYMBOLIC_NAME(""),
        TK_DIGIT(""),
        TK_ANY_CHAR(""),
        TK_LOWERCASE(""),
        TK_UPPERCASE(""),
        TK_SYMBOL(""),
        TK_COMMA(","),
        TK_SEMICOLON(";"),
        TK_COLON(":"),
        TK_CONS("|");

        private String name;
        private String image;

        /**
         * @param image
         */
        TokenKind(String image) {
            this.image = image;
        }

        TokenKind(String name, String image) {
            this.name = name;
            this.image = image;
        }

        public boolean isAtom() {
            return this == TK_ATOM || this == TK_SYMBOLIC_NAME || this == TK_QUOTED_NAME;
        }

        /**
         * @return
         */
        public String getImage() {
            return image;
        }

        public char getChar() {
            return image == null || image.isEmpty() ? 0 : image.charAt(0);
        }

        public String getName() {
            return name;
        }
    }

    public TokenKind kind;

    /**
     * beginLine and beginColumn describe the position of the first character
     * of this token; endLine and endColumn describe the position of the
     * last character of this token.
     */
    public int beginLine;
    public int beginColumn;
    public int endLine;
    public int endColumn;

    /**
     * A reference to the next regular (non-special) token from the input
     * stream.  If this is the last token from the input stream, or if the
     * token manager has not read tokens beyond this one, this field is
     * set to "".  This is true only if this token is also a regular
     * token.  Otherwise, see below for a description of the contents of
     * this field.
     */
    public PlToken next;

//    /**
//     * This field is used to access special tokens that occur prior to this
//     * token, but after the immediately preceding regular (non-special) token.
//     * If there are no such special tokens, this field is set to "".
//     * When there are more than one such special token, this field refers
//     * to the last of these special tokens, which in turn refers to the next
//     * previous special token through its specialToken field, and so on
//     * until the first special token (whose specialToken field is "").
//     * The next fields of special tokens refer to other special tokens that
//     * immediately follow it (without an intervening regular token).  If there
//     * is no such token, this field is "".
//     */
//    public PlToken specialToken;

    /**
     * Returns the image.
     */
    public String toString() {
        return image;
    }

    /**
     * Returns a new Token object, by default. However, if you want, you
     * can create and return subclass objects based on the value of ofKind.
     * Simply add the cases to the switch for all those special cases.
     * For example, if you have a subclass of Token called IDToken that
     * you want to create if ofKind is ID, simlpy add something like :
     * <p>
     * case MyParserConstants.ID : return new IDToken();
     * <p>
     * to the following switch statement. Then you can cast matchedToken
     * variable to the appropriate type and use it in your lexical actions.
     */
    public static PlToken newToken(TokenKind tokenKind) {
        return new PlToken(tokenKind, tokenKind.getImage());
    }
}
