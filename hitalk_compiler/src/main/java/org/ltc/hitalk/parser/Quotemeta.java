package org.ltc.hitalk.parser;


/**
 * 文字列リテラルのエスケープシーケンスの変換処理を行います。
 *
 * @author shun
 */
public final class Quotemeta {

    private Quotemeta () {
    }

    /**
     *
     */
    public static String decode ( CharSequence value ) {
        StringBuilder result = new StringBuilder();
        int j = 0;
        for (int i = 0; i < value.length(); ++i) {
            if (value.charAt(i) == '\\') {
                if (j < i) {
                    result.append(value.subSequence(j, i));
                }
                switch (value.charAt(i)) {
                    case 'b':
                        result.append("\b");
                        break;
                    case 't':
                        result.append("\t");
                        break;
                    case 'n':
                        result.append("\n");
                        break;
                    case 'f':
                        result.append("\f");
                        break;
                    case 'r':
                        result.append("\r");
                        break;
                    case '0':
                        result.append("\0");
                        break;
                    case 'x':
                        String hex = "";
                        hex += value.charAt(++i);
                        hex += value.charAt(++i);
                        result.append((char) Integer.parseInt(hex, 16));
                        break;
                    default:
                        result.append(value.charAt(i));
                        break;
                }
                j = i /*+ 1*/;
            }
        }
        if (j < value.length()) {
            result.append(value.subSequence(j, value.length()));
        }

        return result.toString();
    }

    /**
     * Replaces unprintable characters by their escaped (or unicode escaped)
     * equivalents in the given string
     */
    protected static String addEscapes(String str) {
        StringBuilder retval = new StringBuilder();
        char ch;
        for (int i = 0; i < str.length(); i++) {
            switch (str.charAt(i)) {
                case 0:
                    continue;
                case '\b':
                    retval.append("\\b");
                    continue;
                case '\t':
                    retval.append("\\t");
                    continue;
                case '\n':
                    retval.append("\\n");
                    continue;
                case '\f':
                    retval.append("\\f");
                    continue;
                case '\r':
                    retval.append("\\r");
                    continue;
                case '\"':
                    retval.append("\\\"");
                    continue;
                case '\'':
                    retval.append("\\'");
                    continue;
                case '\\':
                    retval.append("\\\\");
                    continue;
                default:
                    if ((ch = str.charAt(i)) < 0x20 || ch > 0x7e) {
                        String s = "0000" + Integer.toString(ch, 16);
                        retval.append("\\u" + s.substring(s.length() - 4, s.length()));
                    } else {
                        retval.append(ch);
                    }
                    continue;
            }
        }
        return retval.toString();
    }


    /**
     * エスケープします。
     */
    public static String encode(CharSequence value) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); ++i) {
            char c = value.charAt(i);
            switch (c) {
                case '\b':
                    result.append("\\b");
                    continue;
                case '\t':
                    result.append("\\t");
                    continue;
                case '\n':
                    result.append("\\n");
                    continue;
                case '\f':
                    result.append("\\f");
                    continue;
                case '\r':
                    result.append("\\r");
                    continue;
                case '\0':
                    result.append("\\0");
                    continue;
                case '\'':
                    result.append("\\'");
                    continue;
                default:
                    if (Character.isISOControl(c)) {
                        result.append("\\x");
                        for (int j = 0; j < 2; ++j) {
                            result.append("0123456789abcdef".charAt(c >>> j * 4 & 0x0f));
                        }
                        continue;
                    }
                    break;
            }
            result.append(c);
        }
        return result.toString();
    }

    /**
     * 必要であればクオートします。
     */
    public static String quote ( CharSequence value ) {
        if (value.length() == 0) {
            return "''";
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isWhitespace(c) || Character.isISOControl(c) || "()[]{}'".indexOf(c) != -1) {
                return "'" + encode(value) + "'";
            }
        }
        return value.toString();
    }
}
