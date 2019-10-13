package org.ltc.hitalk.parser.jp.segfault.prolog.parser;


import com.thesett.aima.logic.fol.*;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.Source;
import org.ltc.hitalk.compiler.bktables.PlOperatorTable;
import org.ltc.hitalk.compiler.bktables.TermFactory;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.parser.PrologAtoms;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.Operator.Associativity;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlToken.TokenKind;
import org.ltc.hitalk.term.DottedPair;
import org.ltc.hitalk.term.io.HiTalkStream;
import org.ltc.hitalk.wam.compiler.HtFunctor;
import org.ltc.hitalk.wam.compiler.HtFunctorName;

import java.io.IOException;
import java.util.*;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;
import static org.ltc.hitalk.parser.jp.segfault.prolog.parser.Operator.Associativity.*;
import static org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlToken.TokenKind.*;
import static org.ltc.hitalk.term.Atom.EMPTY_TERM_ARRAY;
import static org.ltc.hitalk.term.DottedPair.Kind.*;

/**
 * @author shun
 */
public class PlPrologParser implements TermParser <Term>, Parser <Term, PlToken> {

    public static final String BEGIN_OF_FILE = "begin_of_file";
    public static final String END_OF_FILE = "end_of_file";

    public final static int HILOG_COMPOUND = -128;

    protected final HiTalkStream stream;
    protected final PlLexer lexer;
    protected final VariableAndFunctorInterner interner;
    protected final TermFactory factory;
    protected final PlOperatorTable operatorTable;
    protected final Deque <PlTokenSource> tokenSourceStack = new ArrayDeque <>();
    /**
     * Holds the variable scoping context for the current sentence.
     */
    protected Map <Integer, Variable> variableContext = new HashMap <>();

    /**
     * @param stream
     * @param factory
     * @param optable
     */
    public PlPrologParser ( HiTalkStream stream, VariableAndFunctorInterner interner, TermFactory factory, PlOperatorTable optable ) {
        this.stream = stream;
        lexer = new PlLexer(stream);
        this.interner = interner;
        this.factory = factory;
        this.operatorTable = optable;
    }

    /**
     * @return
     */
    public String language () {
        return "Prolog";
    }

    /**
     * @return
     */
    @Override
    public Term next () throws IOException {// 次のProlog節を解析して返します。
        try {
            PlToken token = lexer.next(true);
            if (token == null) {
                token = PlToken.newToken(EOF);
            }
            return newTerm(token, false, EnumSet.of(TokenKind.DOT));//fixme EOF
        } catch (ParseException e) {
//            int row = ;
//            int col = reader.getCol();
//            throw new ParseException(e.getMessage(); + " [" + row + ":" + col + "]", e, row, col);
            throw new ExecutionError(PERMISSION_ERROR, null);//e.getMessage(); + " [" + row + ":" + col + "]", e, row, col);
        }
    }

    public PlTokenSource getTokenSource () {
        return tokenSourceStack.peek();
    }

    public PlTokenSource popTokenSource () {
        return tokenSourceStack.pop();
    }

    protected Term newTerm ( PlToken token, boolean nullable, EnumSet <TokenKind> terminators ) throws IOException, ParseException {
        OperatorJoiner <Term> joiner = new OperatorJoiner <Term>() {
            @Override
            protected Term join ( int notation, List <Term> args ) {
                return new HtFunctor(notation, args.toArray(new Term[args.size()]));
            }
        };
        outer:
        for (int i = 0; ; ++i, token = lexer.next(joiner.accept(x))) {
            if (token == null) {
                throw new ParseException("Premature EOF");//不正な位置でEOFを検出しました。
            }
            if (!token.quote) {
                if (nullable && i == 0 || joiner.accept(xf)) {
                    for (TokenKind terminator : terminators) {
                        if (token.kind == terminator) {
                            return i > 0 ? joiner.complete() : null;
                        }
                    }
                }
                if (token.kind == ATOM) {
                    for (Operator right : operatorTable.getOperatorsMatchingNameByFixity(token.image).values()) {
                        if (joiner.accept(right.associativity)) {
                            joiner.push(right);
                            continue outer;
                        }
                    }
                }
            }
            if (!joiner.accept(x)) {
                throw new ParseException("Impossible to resolve operator! token=" + token);
            }
            joiner.push(literal(token));
        }
    }

    protected Term literal ( PlToken token ) throws IOException, ParseException {
        Term term = new ErrorTerm();
        switch (token.kind) {
            case VAR:
                term = factory.newVariable(token.image);
                break;
            case FUNCTOR_BEGIN:
                term = compound(token.image);
                break;
            case ATOM:
                term = factory.newAtom(token.image);
                break;
            case INTEGER_LITERAL:
                term = factory.newAtomic(Integer.parseInt(token.image));
                break;
            case FLOATING_POINT_LITERAL:
                term = factory.newAtomic(Double.parseDouble(token.image));
                break;
            case BOF:
                term = new HtFunctor(interner.internFunctorName(BEGIN_OF_FILE, 0), EMPTY_TERM_ARRAY);
                break;
            case EOF:
                term = new HtFunctor(interner.internFunctorName(END_OF_FILE, 0), EMPTY_TERM_ARRAY);
                break;
//            case DOT:
//                end-of_term
//                break;
            case LPAREN:
                DottedPair dottedPair = readSequence(token.kind, RPAREN, true);//blocked sequence
                break;
            case LBRACKET:
                dottedPair = readSequence(token.kind, RBRACKET, false);
                break;
            case LBRACE:
                dottedPair = readSequence(token.kind, RBRACE, false);
                break;
//            case RBRACE:
//            case RPAREN:
//            case RBRACKET:
//                break;
            case D_QUOTE:
                break;
            case S_QUOTE:
                break;
            case B_QUOTE:
                break;
//            case CONS:
//                break;
//            case DECIMAL_LITERAL:
//                break;
//            case HEX_LITERAL:
//                break;
//            case DECIMAL_FLOATING_POINT_LITERAL:
//                break;
//            case DECIMAL_EXPONENT:
//                break;
//            case CHARACTER_LITERAL:
//                break;
//            case STRING_LITERAL:
//                break;
//            case NAME:
//                break;
//            case SYMBOLIC_NAME:
//                break;
//            case DIGIT:
//                break;
//            case ANY_CHAR:
//                break;
//            case LOWERCASE:
//                break;
//            case UPPERCASE:
//                break;
//            case SYMBOL:
//                break;
//            case INFO:
//                break;
//            case TRACE:
//                break;
//            case USER:
//                break;
//            case COMMA:
//                break;
            default:
                throw new ParseException("Unexpected token: " + token);//不明なトークン
        }

        return term;
    }

    //PSEUDO COMPOUND == (terms)
    protected DottedPair readSequence ( TokenKind ldelim, TokenKind rdelim, boolean isBlocked ) throws IOException, ParseException {
        List <Term> elements = new ArrayList <>();
        EnumSet <TokenKind> rdelims = isBlocked ? EnumSet.of(COMMA, rdelim) :
                ///
                EnumSet.of(COMMA, CONS, rdelim);
        DottedPair.Kind kind = LIST;
        switch (ldelim) {
            case LPAREN:
                if (isBlocked) {
                    kind = AND;
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
            elements.add(term);
            while (COMMA == lexer.peek().kind) {//todo flatten
                elements.add(newTerm(lexer.getNextToken(), false, rdelims));
            }
            if (CONS == lexer.peek().kind) {
                newTerm(lexer.getNextToken(), true, rdelims);
            }

            return factory.newDottedPair(kind, elements.toArray(new Term[elements.size()]));
        }

        return (DottedPair) term;
    }

    /**
     * @param name
     * @return
     * @throws IOException
     * @throws ParseException
     */
    protected Functor compound ( String name ) throws IOException, ParseException {
        DottedPair args = readSequence(LPAREN, RPAREN, false);
        return compound(name, args);
    }

    protected Functor compound ( String name, DottedPair args ) throws IOException, ParseException {
        return factory.newFunctor(name, args);
    }

    /**
     * @param source
     */
    public void setTokenSource ( PlTokenSource source ) {
        tokenSourceStack.push(source);
    }

    /**
     * @param source
     */
    @Override
    public void setTokenSource ( Source <PlToken> source ) {
        setTokenSource((PlTokenSource) source);
    }

    /**
     * @return
     */
    @Override
    public Sentence <Term> parse () {
        try {
            return new SentenceImpl <>(termSentence());
        } catch (SourceCodeException | IOException | ParseException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void setOperator ( String operatorName, int priority, OpSymbol.Associativity associativity ) {

    }

    /**
     * Parses a single terms, or atom (a name with arity zero), as a sentence in first order logic. The sentence will
     * be parsed in a fresh variable context, to ensure its variables are scoped to within the term only. The sentence
     * does not have to be terminated by a full stop. This method is not generally used by Prolog, but is provided as a
     * convenience to languages over terms, rather than clauses.
     *
     * @return A term parsed in a fresh variable context.
     * @throws SourceCodeException If the token sequence does not parse into a valid term sentence.
     */
    public Term termSentence () throws SourceCodeException, IOException, ParseException {
        // Each new sentence provides a new scope in which to make variables unique.
        variableContext.clear();

        return literal(lexer.getNextToken());

    }

//    /**
//     * Parses multiple sequential terms, and if more than one is encountered then the flat list of terms encountered
//     * must contain operators in order to be valid Prolog syntax. In that case the flat list of terms is passed to the
//     * {@link DynamicOperatorParser#parseOperators(Term[])} method for 'deferred decision parsing' of dynamic operators.
//     *
//     * @return A single first order logic term.
//     * @throws SourceCodeException If the sequence of tokens does not form a valid syntactical construction as a first
//     *                             order logic term.
//     */
//    public Term term () throws SourceCodeException, IOException, ParseException {
//        List <Term> terms = literal(lexer.getNextToken());
//
//        Term[] flatTerms = terms.toArray(new Term[terms.size()]);
//
//        if (flatTerms.length > 1) {
//            return operatorParser.parseOperators(flatTerms);
//        } else {
//            Term result = flatTerms[0];
//
//            // If a single candidate op name has been parsed, promote it to a constant.
//            if (result instanceof CandidateOpSymbol) {
//                CandidateOpSymbol candidate = (CandidateOpSymbol) result;
//
//                int nameId = interner.internFunctorName(candidate.getTextName(), 0);
//                result = new name(nameId, null);
//            }
//
//            return result;
//        }
//    }

    /**
     * Sets up a custom operator symbol on the parser.
     *
     * @param operatorName  The name of the operator to create.
     * @param priority      The priority of the operator, zero unsets it.
     * @param associativity The operators associativity.
     */
//    @Override
    public void setOperator ( String operatorName, int priority, Associativity associativity ) {
        EnumMap <Operator.Fixity, Operator> ops = operatorTable.getOperatorsMatchingNameByFixity(operatorName);
        if (ops == null || ops.isEmpty()) {
            int arity = calcArity(associativity);
            operatorTable.setOperator(interner.internFunctorName(operatorName, arity), operatorName, priority, associativity);
        }
    }

    private int calcArity ( Associativity associativity ) {
        return associativity.arity;
    }
//
//    /**
//     * Recursively parses terms, which may be names, atoms, variables, literals or operators, into a flat list in the
//     * order in which they are encountered.
//     *
//     * @param terms A list of terms to accumulate in.
//     * @return The list of terms encountered in order.
//     * @throws SourceCodeException If the sequence of tokens does not form a valid syntactical construction as a list of
//     *                             first order logic terms.
//     */
//    public List <Term> terms ( List <Term> terms ) throws SourceCodeException {
//        Term term = null;
//        PlToken nextToken = getTokenSource().peek();
//        switch (nextToken.kind) {
//            case BOF:
//                if (getTokenSource().isBofGenerated()) {
//                    throw new IllegalStateException("The term begin_of_file is reserved.");
//                }
////                consumeToken(BOF);
//                term = new name(interner.internFunctorName(BEGIN_OF_FILE, 0), null);
//                break;
//            case EOF:
////                consumeToken(EOF);
//                term = new name(interner.internFunctorName(END_OF_FILE, 0), null);
//                break;
//            case name:
//                term = name();
//                break;
//            case LBRACKET:
////                term = listname(consumeToken(LBRACKET));//ConsType.DOT_PAIR_CONS);
//                // Mark the term as bracketed to ensure that this is its final parsed form. In particular the
//                // #arglist method will not break it up if it contains commas.
//                term.setBracketed(true);
////                consumeToken(RBRACKET);
////                break;
//            case LBRACE:
////                term = listname(consumeToken(LBRACE), ConsType.BYPASS_CONS);
//                // Mark the term as bracketed to ensure that this is its final parsed form. In particular the
//                // #arglist method will not break it up if it contains commas.
//                term.setBracketed(true);
////                consumeToken(RBRACE);
////                break;
//            case VAR:
//                term = variable();
//                break;
//            case INTEGER_LITERAL:
//                term = intLiteral();
//                break;
//            case FLOATING_POINT_LITERAL:
//                term = doubleLiteral();
//                break;
//            case STRING_LITERAL:
//                term = stringLiteral();
//                break;
//            case ATOM:
//                term = atom();
//                break;
//            case LPAREN:
//                consumeToken(LPAREN);
//                term = term();
//                // Mark the term as bracketed to ensure that this is its final parsed form. In particular the
//                // #arglist method will not break it up if it contains commas.
//                term.setBracketed(true);
//                consumeToken(RPAREN);
//                break;
//            default:
//                throw new SourceCodeException("Expected one of " + BEGIN_TERM_TOKENS + " but got " + tokenImage[nextToken.kind.ordinal()] + ".", null, null, null,
//                        new SourceCodePositionImpl(nextToken.beginLine, nextToken.beginColumn, nextToken.endLine, nextToken.endColumn));
//        }
//
//        terms.add(term);
//
//        switch (getTokenSource().peek().kind) {
//            case LPAREN:
//            case LBRACKET:
//            case LBRACE:
//            case INTEGER_LITERAL:
//            case FLOATING_POINT_LITERAL:
//            case STRING_LITERAL:
//            case VAR:
//            case name:
//            case ATOM:
//                terms(terms);
//                break;
//            default:
//        }
//
//        return terms;
//    }
//    /**
//     * @param operatorName
//     * @param priority
//     * @param associativity
//     */
//    public void setOperator ( String operatorName, int priority, Associativity associativity ) {
////        new Operator(operatorName,priority,associativity);
//     operatorTable.setOperator(operatorName,priority,associativity);
//    }

    /**
     * Interns an operators name as a name of appropriate arity for the operators fixity, and sets the operator in
     * the operator table.
     *
     * @param operatorName  The name of the operator to create.
     * @param priority      The priority of the operator, zero unsets it.
     * @param associativity The operators associativity.
     */
    public void internOperator ( String operatorName, int priority, Associativity associativity ) {
        int arity;

        if ((associativity == xfy) | (associativity == yfx) | (associativity == xfx)) {
            arity = 2;
        } else {
            arity = 1;
        }

        int name = interner.internFunctorName(operatorName, arity);
        operatorTable.setOperator(name, operatorName, priority, associativity);
    }

    /**
     * Interns and inserts into the operator table all of the built in operators and names in Prolog.
     */
    protected void initializeBuiltIns () {
        // Initializes the operator table with the standard ISO prolog built-in operators.
        internOperator(PrologAtoms.IMPLIES, 1200, xfx);
        internOperator(PrologAtoms.IMPLIES, 1200, fx);
        internOperator(PrologAtoms.DCG_IMPLIES, 1200, xfx);
        internOperator(PrologAtoms.QUERY, 1200, fx);

        internOperator(PrologAtoms.SEMICOLON, 1100, xfy);
        internOperator(PrologAtoms.IF, 1050, xfy);
        internOperator(PrologAtoms.IF_STAR, 1050, xfy);

        internOperator(PrologAtoms.COMMA, 1000, xfy);
        internOperator(PrologAtoms.NOT, 900, fy);

        internOperator(PrologAtoms.UNIFIES, 700, xfx);
        internOperator(PrologAtoms.NON_UNIFIES, 700, xfx);
        internOperator(PrologAtoms.IDENTICAL, 700, xfx);
        internOperator(PrologAtoms.NON_IDENTICAL, 700, xfx);
        internOperator(PrologAtoms.AT_LESS, 700, xfx);
        internOperator(PrologAtoms.AT_LESS_OR_EQUAL, 700, xfx);
        internOperator(PrologAtoms.AT_GREATER, 700, xfx);
        internOperator(PrologAtoms.AT_GREATER_OR_EQUAL, 700, xfx);
        internOperator(PrologAtoms.UNIV, 700, xfx);
        internOperator(PrologAtoms.IS, 700, xfx);
//        internOperator(PrologAtoms.C, 700, XFX);
        internOperator(PrologAtoms.EQ_BSLASH_EQ, 700, xfx);
        internOperator(PrologAtoms.LESS, 700, xfx);
        internOperator(PrologAtoms.LESS_OR_EQUAL, 700, xfx);
        internOperator(PrologAtoms.GREATER, 700, xfx);
        internOperator(PrologAtoms.GREATER_OR_EQUAL, 700, xfx);

//        internOperator(PrologAtoms."+", 500, YFX);
//        internOperator(PrologAtoms."-", 500, YFX);

        internOperator(PrologAtoms.BSLASH_SLASH, 500, yfx);
        internOperator(PrologAtoms.SLASH_BSLASH, 500, yfx);

        internOperator(PrologAtoms.SLASH, 400, yfx);
        internOperator(PrologAtoms.SLASH_SLASH, 400, yfx);
        internOperator(PrologAtoms.STAR, 400, yfx);
        internOperator(PrologAtoms.RSHIFT, 400, yfx);
        internOperator(PrologAtoms.LSHIFT, 400, yfx);
        internOperator(PrologAtoms.REM, 400, yfx);
        internOperator(PrologAtoms.MOD, 400, yfx);

        internOperator(PrologAtoms.MINUS, 200, fy);
        internOperator(PrologAtoms.UP, 200, yfx);
        internOperator(PrologAtoms.STAR_STAR, 200, yfx);
        internOperator(PrologAtoms.AS, 200, fy);
        //FIXME
        internOperator(PrologAtoms.VBAR, 1001, xfy);
        internOperator(PrologAtoms.VBAR, 1001, fy);

        // Intern all built in names.
        interner.internFunctorName(PrologAtoms.ARGLIST_NIL, 0);
        interner.internFunctorName(new HtFunctorName(PrologAtoms.ARGLIST_CONS, 0, 2));

        interner.internFunctorName(PrologAtoms.NIL, 0);
        interner.internFunctorName(new HtFunctorName(PrologAtoms.CONS, 0, 2));

        interner.internFunctorName(PrologAtoms.TRUE, 0);
        interner.internFunctorName(PrologAtoms.FAIL, 0);
        interner.internFunctorName(PrologAtoms.FALSE, 0);
        interner.internFunctorName(PrologAtoms.CUT, 0);

        interner.internFunctorName(PrologAtoms.BYPASS_NIL, 0);
        interner.internFunctorName(new HtFunctorName(PrologAtoms.BYPASS_CONS, 0, 2));
    }
}
