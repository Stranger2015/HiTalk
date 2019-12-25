package org.ltc.hitalk.parser;

/**
 * Describes the input token stream.
 */
public class PlToken implements ISourceCodePosition {

    public boolean quote;
    public String image;

    public void setBeginLine ( int beginLine ) {
        this.beginLine = beginLine;
    }

    public void setBeginColumn ( int beginColumn ) {
        this.beginColumn = beginColumn;
    }

    public void setEndLine ( int endLine ) {
        this.endLine = endLine;
    }

    public void setEndColumn ( int endColumn ) {
        this.endColumn = endColumn;
    }

    public int getBeginLine () {
        return beginLine;
    }

    public int getBeginColumn () {
        return beginColumn;
    }

    public int getEndLine () {
        return endLine;
    }

    public int getEndColumn () {
        return endColumn;
    }

    /**
     * @param kind
     */
    public PlToken ( TokenKind kind ) {
        this.kind = kind;
        image = kind.getImage();
        if (!image.isEmpty()) {
            endColumn = beginColumn + image.length();
        }
        if (kind == TokenKind.BOF) {
            endLine = beginLine = 0;
            endColumn = beginColumn = 0;
        }
    }

    /**
     * @param kind
     * @param image
     */
    public PlToken ( TokenKind kind, String image ) {
        this(kind);
        this.image = image;
    }

    /**
     * @param kind
     * @param number
     * @param quote
     */
    public PlToken ( TokenKind kind, String number, boolean quote ) {
        this(kind, number);
        this.quote = quote;
    }

    public String getNumber () {
        return image;
    }

    /**
     *
     */
    public enum TokenKind {
        BOF(""),
        EOF(""),
        DOT("."),
        LPAREN("("),
        RPAREN(")"),
        LBRACKET("["),
        RBRACKET("]"),
        LBRACE("{"),
        RBRACE("}"),

        D_QUOTE("\""),
        S_QUOTE("'"),
        B_QUOTE("`"),
        INTEGER_LITERAL(""),
        DECIMAL_LITERAL(""),
        HEX_LITERAL(""),
        FLOATING_POINT_LITERAL(""),
        DECIMAL_EXPONENT(""),
        CHARACTER_LITERAL(""),
        STRING_LITERAL(""),
        VAR(""),
        FUNCTOR_BEGIN(""),
        ATOM(""),
        NAME(""),
        SYMBOLIC_NAME(""),
        DIGIT(""),
        ANY_CHAR(""),
        LOWERCASE(""),
        UPPERCASE(""),
        SYMBOL(""),
        COMMA(","),
        SEMICOLON(";"),
        COLON(":"),
        CONS("|");

        TokenKind ( String image ) {
            this.image = image;
        }

        private String image;

        public String getImage () {
            return image;
        }

        public char getChar () {
            return image == null || image.isEmpty() ? 0 : image.charAt(0);
        }
    }

    /**
     * An integer that describes the kind of this token.  This numbering
     * system is determined by JavaCCParser("*"), and a table of these numbers is
     * stored in the file ...Constants.java.
     */
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
     * The string image of the token.
     */
//    public String image;

    /**
     * A reference to the next regular (non-special) token from the input
     * stream.  If this is the last token from the input stream, or if the
     * token manager has not read tokens beyond this one, this field is
     * set to "".  This is true only if this token is also a regular
     * token.  Otherwise, see below for a description of the contents of
     * this field.
     */
    public PlToken next;

    /**
     * This field is used to access special tokens that occur prior to this
     * token, but after the immediately preceding regular (non-special) token.
     * If there are no such special tokens, this field is set to "".
     * When there are more than one such special token, this field refers
     * to the last of these special tokens, which in turn refers to the next
     * previous special token through its specialToken field, and so on
     * until the first special token (whose specialToken field is "").
     * The next fields of special tokens refer to other special tokens that
     * immediately follow it (without an intervening regular token).  If there
     * is no such token, this field is "".
     */
    public PlToken specialToken;

    /**
     * Returns the image.
     */
    public String toString () {
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
    public static PlToken newToken ( TokenKind tokenKind ) {
        return new PlToken(tokenKind, tokenKind.getImage());
    }
}
