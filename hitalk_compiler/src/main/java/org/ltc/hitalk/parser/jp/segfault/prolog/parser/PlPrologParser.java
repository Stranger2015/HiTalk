package org.ltc.hitalk.parser.jp.segfault.prolog.parser;

import com.thesett.aima.logic.fol.*;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.Source;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.Operator.Kind;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlToken.TokenKind;
import org.ltc.hitalk.term.DottedPair;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlToken.TokenKind.*;
import static org.ltc.hitalk.term.Atom.EMPTY_TERM_ARRAY;
import static org.ltc.hitalk.term.DottedPair.Kind.BYPASS;
import static org.ltc.hitalk.term.DottedPair.Kind.LIST;

/**
 * @author shun
 */
public class PlPrologParser implements TermParser <Term>, Parser <Term, PlToken> {
    public static final String BEGIN_OF_FILE = "begin_of_file";
    public static final String END_OF_FILE = "end_of_file";

    final protected static int HILOG_COMPOUND = 38;//todo wtf??

    private final CountingReader reader;
    private final PlLexer lexer;
    private final VariableAndFunctorInterner interner;
    private final TermFactory <Term> factory;
    private final OperatorTable optable;

    /**
     * @param reader
     * @param factory
     * @param optable
     */
    public PlPrologParser ( Reader reader, VariableAndFunctorInterner interner, TermFactory <Term> factory, OperatorTable optable ) {
        lexer = new PlLexer((this.reader = new CountingReader(reader)));
        this.interner = interner;
        this.factory = factory;
        this.optable = optable;
    }

    public PlPrologParser () {
        this(new InputStreamReader(System.in), new VariableAndFunctorInternerImpl("XXx", "YYT"), new TermFactory()) {
        }

        /**
         * 次のProlog節を解析して返します。
         */
        @Override public Term next () throws IOException, ParseException {
            try {
                PlToken token = lexer.next(true);
                if (token == null) {
                    return null;
                }
                return newTerm(token, false, EnumSet.of(DOT));//fixme EOF
            } catch (ParseException e) {
                int row = reader.getRow();
                int col = reader.getCol();
                throw new ParseException(e.getMessage() + " [" + row + ":" + col + "]", e, row, col);
            }
        }

        //    private Term term () throws IOException, ParseException {
//        return term(lexer.next(true), false, EnumSet.of(RBRACKET));
//    }
//
        private Term newTerm (PlToken token,boolean nullable, EnumSet <TokenKind > terminators ) throws
        IOException, ParseException {
            OperatorJoiner <Term> joiner = new OperatorJoiner <Term>() {
                @Override
                protected Term join ( String notation, List <Term> args ) {
                    return factory.newFunctor(notation, args);
                }
            };
            outer:
            for (int i = 0; ; ++i, token = lexer.next(joiner.accept(Kind.x))) {
                if (token == null) {
                    throw new ParseException("Premature EOF");//不正な位置でEOFを検出しました。
                }
                if (!token.quote) {
                    if (nullable && i == 0 || joiner.accept(Kind.xf)) {
                        for (TokenKind terminator : terminators) {
                            if (token.kind == terminator) {
                                return i > 0 ? joiner.complete() : null;
                            }
                        }
                    }
                    if (token.kind == ATOM) {
                        for (Operator right : optable.getOperator(token.image)) {
                            if (joiner.accept(right.kind)) {
                                joiner.push(right);
                                continue outer;
                            }
                        }
                    }
                }
                if (!joiner.accept(Kind.x)) {
                    throw new ParseException("Impossible to resolve operator! token=" + token);
                }
                joiner.push(literal(token));
            }
        }

        private Term literal (PlToken token ) throws IOException, ParseException {
            Term term = new ErrorTerm();
            switch (token.kind) {
                case VAR:
                    return factory.newVariable(token.image);
                case FUNCTOR:
                    return complex(token.image);
                case ATOM:
                    return factory.newAtom(token.image);
                case INTEGER_LITERAL:
                    return factory.newAtom(Integer.parseInt(token.image));
                case FLOATING_POINT_LITERAL:
                    return factory.newAtom(Double.parseDouble(token.image));
                case BOF:
                    term = new Functor(interner.internFunctorName(BEGIN_OF_FILE, 0), EMPTY_TERM_ARRAY);
                    break;
                case EOF:
                    term = new Functor(interner.internFunctorName(END_OF_FILE, 0), EMPTY_TERM_ARRAY);
                    break;
//            case DOT:
//                end-of_term
//                break;
                case LPAREN:
                    return readSequence(token.kind, RPAREN, true);//blocked sequence
                case LBRACKET:
                    return readSequence(token.kind, RBRACKET, false);
                case LBRACE:
                    return readSequence(token.kind, RBRACE, false);
                case RBRACE:
                case RPAREN:
                case RBRACKET:
                    break;
                case D_QUOTE:
                    break;
                case S_QUOTE:
                    break;
                case B_QUOTE:
                    break;
                case CONS:
                    break;
                case DECIMAL_LITERAL:
                    break;
                case HEX_LITERAL:
                    break;
                case DECIMAL_FLOATING_POINT_LITERAL:
                    break;
                case DECIMAL_EXPONENT:
                    break;
                case CHARACTER_LITERAL:
                    break;
                case STRING_LITERAL:
                    break;
                case NAME:
                    break;
                case SYMBOLIC_NAME:
                    break;
                case DIGIT:
                    break;
                case ANY_CHAR:
                    break;
                case LOWERCASE:
                    break;
                case UPPERCASE:
                    break;
                case SYMBOL:
                    break;
                case INFO:
                    break;
                case TRACE:
                    break;
                case USER:
                    break;
                case COMMA:
                    break;
                default:
                    throw new ParseException("Unexpected token: " + token);//不明なトークン
            }

            return term;
        }
//PSEUDO COMPOUND == (terms)
        private Term readSequence (TokenKind ldelim, TokenKind rdelim,boolean isBlocked ) throws
        IOException, ParseException {
            List <Term> elements = new ArrayList <>();
            EnumSet <TokenKind> rdelims = isBlocked ? EnumSet.of(COMMA, rdelim) : EnumSet.of(COMMA, CONS, rdelim);
            DottedPair.Kind kind = LIST;
            switch (ldelim) {
                case LPAREN:
                    if (isBlocked) {
                        kind = DottedPair.Kind.CALLABLE;
                    } else {
                        kind = LIST;//compound args
                    }
                    break;
                case LBRACKET:
                    kind = LIST;
                    break;
                case LBRACE:
                    kind = BYPASS;
                    break;
                default:
                    break;
            }
            Term term = newTerm(lexer.getNextToken(), true, rdelims);//,/2 ????????
            if (term == null) {
                if (rdelim != lexer.peek().kind) {
                    throw new ParseException("No (more) elements");//要素がありません・・・。
                }
//					return factory.newAtom(interner,ldelim,rdelim);
                elements.add(term);
                while (COMMA == lexer.peek().kind) {
                    elements.add(newTerm(lexer.getNextToken(), false, rdelims));
                }
                Term[] headTail = new Term[0];// = factory.newAtom("[]");
                if (CONS == lexer.peek().kind) {
                    newTerm(lexer.getNextToken(), true, rdelims);
                }
                for (Term e : elements) {
                    term = new DottedPair(kind, headTail);
//                head = factory.newFunctor(DOT, (asList(e, head)));
                }
                return term;
            }
            return term;
        }


        private Term complex (String name ) throws IOException, ParseException {
            Term args = readSequence(LPAREN, RPAREN, false);
//        do {
//            args.add(newTerm(false, ",", ")"));
//        } while (",".equals(lexer.peek().image));
            return factory.newFunctor(name, null);
        }

        @Override public void setTokenSource (Source < PlToken > source) {
//		setTokenSource(source);
        }

        @Override public Sentence <Term> parse () throws SourceCodeException {
            return null;
        }

        @Override public void setOperator (String operatorName,int priority, Associativity associativity ){

        }
    }
