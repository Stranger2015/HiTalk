package org.ltc.hitalk.parser;

import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.PlToken.TokenKind;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.IdentifiedTerm;
import org.ltc.hitalk.term.IdentifiedTerm.Associativity;
import org.ltc.hitalk.term.io.HiTalkInputStream;
import org.ltc.hitalk.wam.compiler.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static java.util.EnumSet.of;
import static java.util.Objects.requireNonNull;
import static org.ltc.hitalk.core.BaseApp.getAppContext;
import static org.ltc.hitalk.core.utils.TermUtilities.convertToClause;
import static org.ltc.hitalk.parser.Directive.DirectiveKind.DK_ENCODING;
import static org.ltc.hitalk.parser.Directive.DirectiveKind.DK_IF;
import static org.ltc.hitalk.parser.HtPrologParser.ParserState.*;
import static org.ltc.hitalk.parser.PlToken.TokenKind.*;
import static org.ltc.hitalk.parser.PlToken.newToken;
import static org.ltc.hitalk.parser.PrologAtoms.*;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity.*;
import static org.ltc.hitalk.wam.compiler.Language.PROLOG;

public class HtPrologParser implements IParser {
    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    final EnumSet<TokenKind> rDelims = of(TK_COMMA, TK_CONS);

    protected Deque<StateRecord> states = new ArrayDeque<>();
    private final int MAX_PRIORITY = 1200;
    private final int MIN_PRIORITY = 0;
    private int currPriority = MAX_PRIORITY;

    /**
     *
     */
    public enum ParserState {
        START,

        EXPR_A,
        EXPR_A_EXIT,

        EXPR_B,
        EXPR_B_EXIT,

        EXPR_C,
        EXPR_C_EXIT,

        EXPR_A0,
        EXPR_A0_EXIT,

        EXPR_A0_PAREN,
        EXPR_A0_PAREN_EXIT,

        EXPR_A0_BRACKET,
        EXPR_A0_BRACKET_EXIT,

        EXPR_A0_BRACE,
        EXPR_A0_BRACE_EXIT,

        FINISH
    }

    HtPrologParser.ParserState state = START;
    final Map<TokenKind, Class<?>> map = new HashMap<>();

    public static final String BEGIN_OF_FILE_STRING = "begin_of_file";
    public static final String END_OF_FILE_STRING = "end_of_file";
    public static final IdentifiedTerm END_OF_FILE = new IdentifiedTerm(END_OF_FILE_STRING);
    public static final IdentifiedTerm BEGIN_OF_FILE = new IdentifiedTerm(BEGIN_OF_FILE_STRING);
    public static final String ANONYMOUS = "_";

    protected final Deque<PlLexer> tokenSourceStack = new ArrayDeque<>();

    protected IOperatorTable operatorTable;
    protected static IVafInterner interner;
    protected ITermFactory termFactory;

    /**
     * Holds the variable scoping context for the current sentence.
     */
    protected Map<Integer, HtVariable> variableContext = new HashMap<>();
    protected IdentifiedTerm operator;

    protected ITerm lastTerm;
    protected ITerm lastSequence;

    private int braces;
    private int parentheses;
    private int brackets;

    private int squotes;
    private int dquotes;
    private int bquotes;

    /**
     * @param inputStream
     * @param factory
     * @param optable
     */
    public HtPrologParser(HiTalkInputStream inputStream,
                          IVafInterner interner,
                          ITermFactory factory,
                          IOperatorTable optable)
            throws Exception {
        setTokenSource(new PlLexer(inputStream, inputStream.getPath()));
        PlPrologParser.interner = interner;
        this.termFactory = factory;
        this.operatorTable = optable;
    }

    /**
     *
     */
    public HtPrologParser() throws Exception {
        this(
                getAppContext().getInputStream(),
                getAppContext().getInterner(PROLOG.getNameSpace("Variables", "Functors")),
                getAppContext().getTermFactory(),
                getAppContext().getOpTable()
        );
    }

    /**
     * @return
     */
    public HtPrologParser getParser() {
        return this;
    }

    @Override
    public IVafInterner getInterner() {
        return interner;
    }

    @Override
    public void setInterner(IVafInterner interner) {
        HtPrologParser.interner = interner;
    }

    @Override
    public ITermFactory getFactory() {
        return termFactory;
    }

    public IOperatorTable getOptable() {
        return operatorTable == null ?
                new PlDynamicOperatorParser() : operatorTable;
    }

    @Override
    public void setOptable(IOperatorTable optable) {
        this.operatorTable = optable;
    }

    /**
     * @return
     */
    public Language language() {
        return PROLOG;
    }

    /**
     * @return
     * @throws HtSourceCodeException
     */
    public ITerm parse() throws Exception {
        return termSentence();
    }

    /**
     * @param operatorName
     * @param priority
     * @param associativity
     */
    public void setOperator(String operatorName, int priority, Associativity associativity) {
        Map<IdentifiedTerm.Fixity, IdentifiedTerm> ops = operatorTable.getOperatorsMatchingNameByFixity(operatorName);
        if (ops == null || ops.isEmpty()) {
            int arity = calcArity(associativity);
            int name = interner.internFunctorName(operatorName, arity);
            operatorTable.setOperator(name, operatorName, priority, associativity);
        }
    }

    /**
     * @param associativity
     * @return
     */
    private int calcArity(Associativity associativity) {
        return associativity.arity;
    }

    /**
     * Interns an operators name as a name of appropriate arity for the operators fixity, and sets the operator in
     * the operator table.
     *
     * @param operatorName  The name of the operator to create.
     * @param priority      The priority of the operator, zero unsets it.
     * @param associativity The operators associativity.
     */
    void op(String operatorName, int priority, Associativity associativity) {
        int arity;
        if ((associativity == xfy) || (associativity == yfx) || (associativity == xfx)) {
            arity = 2;
        } else {
            arity = 1;
        }

        int name = getInterner().internFunctorName(operatorName, arity);
        getOptable().setOperator(name, operatorName, priority, associativity);
    }

    public void op(int priority, Associativity associativity, String... operatorNames) {
        for (final String operatorName : operatorNames) {
            op(operatorName, priority, associativity);
        }
    }


    /**
     * Interns and inserts into the operator table all of the built in operators and names in Prolog.
     */
    public void initializeBuiltIns() {
        logger.info("Initializing built-in operators...");

        // Initializes the operator table with the standard ISO prolog built-in operators.
        op(1200, xfx, IMPLIES, DCG_IMPLIES);
        op(1200, fx, IMPLIES, QUERY);

        op(1100, xfy, SEMICOLON);

        op(1050, xfy, IF_THEN, IF_STAR);

        op(1000, xfy, COMMA);

        op(990, xfy, ASSIGN);
        op(900, fy, NAF);

        op(700, xfx,
                UNIFIES,
                GREATER,
                IDENTICAL,
                NON_IDENTICAL,

                EQ_BSLASH_EQ,
                EQ_COLON_EQ,
                LESS,
                LESS_OR_EQUAL,
                AT_GREATER_OR_EQUAL,
                AT_GREATER,
                AT_LESS,
                AT_LESS_OR_EQUAL,
                UNIV,
                IS,
                GREATER_OR_EQUAL,
                GREATER
        );
        op(600, xfy, COLON);

        op(500, yfx,
                PLUS,
                MINUS);

        op(400, yfx,
                SLASH,
                SLASH_SLASH,
                STAR,
                RSHIFT,
                LSHIFT,
                REM,
                MOD,
                XOR,
                DIV,
                RDIV
        );

        op(200, fy,
                PLUS,
                MINUS,
                BSLASH);

        op(200, yfx, UP);
        op(200, yfx, UP_UP);
        op(200, xfx, STAR_STAR);
        op(200, fy, AS);
        op(100, yfx, DOT);
        op(1, fx, DOLLAR);
//Block operators
//        This operator is typically declared as a low-priority yf postfix operator,
//        which allows for array[index] notation. This syntax produces a term []([index],array).
//        { }
//        This operator is typically declared as a low-priority xf postfix operator,
//        which allows for head(arg) { body } notation. This syntax produces a term {}({body},head(arg)).

        op(100, xf, "{}");
        op(100, yf, "[]");
//        op(100, yf, "()");  fixme

//        hilog ops;

        // Intern all built in names.
//        interner.internFunctorName(ARGLIST_NIL, 0);

        interner.internFunctorName(PrologAtoms.NIL, 0);
//        interner.internFunctorName(new HtFunctorName(CONS, 1, 2));

        interner.internFunctorName(TRUE, 0);
        interner.internFunctorName(FAIL, 0);
        interner.internFunctorName(FALSE, 0);
        interner.internFunctorName(CUT, 0);
    }

    // * BNF part 2: Parser
    // * term ::=
    //      exprA(1200)

    // * exprA(n) ::=
    //      exprB(n) { op(yfx,n) exprA(n-1) |
    // *                         op(yf,n) }*
    // * exprB(n) ::=
    //      exprC(n-1)
    //      {
    //          op(xfx,n) exprA(n-1) |
    // *        op(xfy,n) exprA(n) |
    // *        op(xf,n)
    //      }*
    // * // exprC is caled parseLeftSide in the code
    // * exprC(n) ::=
    //      '-' integer | '-' float |
    //      op( fx,n ) exprA(n-1) |
    // *    op( fy,n ) exprA(n) |
    // *               exprA(n)
    // * exprA(0) ::=
    //      integer |
    // *    float |
    // *    atom |
    // *    variable |
    //      exprA(1200)'('  [ [ exprA(1200) { ',' exprA(1200) }* ][ '|' exprA(1200) ] ] ')' |
    // *               '['  [ [ exprA(1200) { ',' exprA(1200) }* ][ '|' exprA(1200) ] ] ']' |
    // *   '('  { exprA(1200) }* ')'  //block
    // *   '{'  { exprA(1200) }* '}'

    // * op(type,n) ::=
    //      atom | { symbol }+

    /**
     * @return
     * @throws Exception
     */
    @Override
    public ITerm expr() throws Exception {
        PlToken token = getLexer().getToken(true);
        StateRecord sr = createState(START, of(x), of(DK_IF), of(TK_DOT), MAX_PRIORITY, token);
        states.push(sr);
        ITerm term;
        for (; ; ) {
            switch (states.peek().getParserState()) {
                case START:
                    sr = createState(EXPR_A, of(yfx, yf), of(DK_IF, DK_ENCODING), of(TK_DOT), currPriority, token);
                    break;
                case EXPR_A:
                    sr = createState(EXPR_B, of(xfx, xfy, xf), of(DK_IF), of(TK_DOT), currPriority, token);
                    break;
                case EXPR_A_EXIT://========================
                    break;
                case EXPR_B:
                    sr = createState(EXPR_C, of(xfx, xfy, xf), of(DK_IF), of(TK_DOT), --currPriority, token);//fixme
                    break;
                case EXPR_B_EXIT://========================
                    break;
                case EXPR_C:
                    if (token.image.equals("-") || token.image.equals("+")) {
                        lastTerm = readNumber(token.image);
                        if (lastTerm == null) {
                            lastTerm = tryOperators(token.image, EnumSet.of(fx), currPriority - 1);
                        }
                    }
                    sr = createState(EXPR_A, of(fx, fy), of(DK_IF), of(TK_DOT), MAX_PRIORITY, token);
                    break;
                case EXPR_C_EXIT://=======================
                    break;
                case EXPR_A0:
                    getLexer().getToken(true);
                    break;
                case EXPR_A0_EXIT://=======================
                    break;
                case EXPR_A0_PAREN:
                    break;
                case EXPR_A0_PAREN_EXIT://=============================
                    token = getLexer().getToken(true);
                    switch (token.kind) {
                        case TK_BOF:
                            term = BEGIN_OF_FILE;
                            break;
                        case TK_EOF:
                            term = END_OF_FILE;
                            break;
                        case TK_DOT:
                            createState(FINISH);
                            break;
                        case TK_LPAREN:

                            break;
                        case TK_RPAREN:
                            break;
                        case TK_LBRACKET:
                            EXPR_A0_BRACKET:
                            break;
                        case TK_RBRACKET:
                            break;
                        case TK_LBRACE:
                            EXPR_A0_BRACE:
                            break;
                        case TK_RBRACE:
                            break;
                        case TK_D_QUOTE:
                            break;
                        case TK_S_QUOTE:
                            break;
                        case TK_B_QUOTE:
                            break;
                        case TK_INTEGER_LITERAL:
                            break;
                        case TK_DECIMAL_LITERAL:
                            break;
                        case TK_HEX_LITERAL:
                            break;
                        case TK_FLOATING_POINT_LITERAL:
                            break;
                        case TK_DECIMAL_EXPONENT:
                            break;
                        case TK_CHARACTER_LITERAL:
                            break;
                        case TK_STRING_LITERAL:
                            break;
                        case TK_VAR:
                            break;
                        case TK_FUNCTOR_BEGIN:
                            break;
                        case TK_ATOM:
                            break;
                        case TK_QUOTED_NAME:
                            break;
                        case TK_SYMBOLIC_NAME:
                            break;
                        case TK_DIGIT:
                            break;
                        case TK_ANY_CHAR:
                            break;
                        case TK_LOWERCASE:
                            break;
                        case TK_UPPERCASE:
                            break;
                        case TK_SYMBOL:
                            break;
                        case TK_COMMA:
                            break;
                        case TK_SEMICOLON:
                            break;
                        case TK_COLON:
                            break;
                        case TK_CONS:
                            break;
                    }
                    break;
                case EXPR_A0_BRACKET:
                    break;
                case EXPR_A0_BRACKET_EXIT://===========================
                    break;
                case EXPR_A0_BRACE:
                    break;
                case EXPR_A0_BRACE_EXIT://=============================
                    break;
                case FINISH:
//                    createState(PlToken.newToken(TK_EOF);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + states.peek().getParserState());
            }
        }
//       return sr;
    }

    private void createState(ParserState finish) {
        finish = FINISH;
    }

    private StateRecord finish() {
        return new StateRecord(FINISH,
                of(x),
                of(IF),
                of(TK_DOT),
                -1,
                newToken(TK_EOF)
        );
    }

    public StateRecord createState(
            ParserState state,
            EnumSet<Associativity> associativities,
            EnumSet<DirectiveKind> kinds,
            EnumSet<TokenKind> rDelims,
            int maxPriority,
            PlToken token) {
        final StateRecord stateRec = new StateRecord(
                state,
                associativities,
                kinds,
                rDelims,
                maxPriority,
                token);

        states.push(stateRec);
        return stateRec;
    }

    private ITerm tryOperators(String name, EnumSet<Associativity> associativities, int maxPriority) {
        for (Associativity associativity : associativities) {
            final Map<IdentifiedTerm.Fixity, IdentifiedTerm> ops = getOptable().getOperatorsMatchingNameByFixity(name);
        }
        return null;
    }

    private ITerm readNumber(String image) {
        return null;
    }

    /**
     * @return
     */
    public HtClause parseClause() throws Exception {
        return convertToClause(termSentence(), getAppContext().getInterner());
    }

    /**
     * Parses a single terms, or atom (a name with arity zero), as a sentence in first order logic. The sentence will
     * be parsed in a fresh variable context, to ensure its variables are scoped to within the term only. The sentence
     * does not have to be terminated by a full stop. This method is not generally used by Prolog, but is provided as a
     * convenience to languages over terms, rather than clauses.
     *
     * @return A term parsed in a fresh variable context.
     */
    public ITerm termSentence() throws Exception {
        // Each new sentence provides a new scope in which to make variables unique.
        variableContext.clear();

        if (!tokenSourceStack.isEmpty() && tokenSourceStack.peek().isOpen()) {
            return expr();
        }

        popTokenSource();
        return END_OF_FILE;
    }

    // * This class defines a parser of prolog terms and sentences.

//    /**
//     * Parses next term from the stream built on string.
//     *
//     * @param endNeeded <tt>true</tt> if it is required to parse the end token
//     *                  (a period), <tt>false</tt> otherwise.
//     */
//    public ITerm nextTerm(boolean valued, boolean endNeeded) throws Exception {
//        PlToken t = getLexer().readToken(valued);
//        if (t.kind == TK_BOF) {
//            return BEGIN_OF_FILE;
//        }
//        if (t.kind == TK_EOF) {//error!!!!!!!!!!
//            popTokenSource();
//            return END_OF_FILE;
//        }
//        //    getLexer().unreadToken(t);
//        ITerm term = expr(EnumSet.of(TK_DOT));
//        if (term == END_OF_FILE) {
//            return term;
//        }
//        if (endNeeded && getLexer().readToken(valued).kind != TK_DOT) {
//            throw new ParserException("The term " + term + " is not ended with a period.");
//        }
//
//        return term;
//    }
//
//    /**
//     * Static service to get a term from its string representation
//     */
//    public ITerm parseSingleTerm(String st) throws Exception {
//        PlLexer ts = PlLexer.getPlLexerForString(st);
//        return parseSingleTerm(ts);
//    }
//
//    /**
//     * Static service to get a term from its string representation,
//     * providing a specific operator manager
//     */
//    public ITerm parseSingleTerm(PlLexer ts) throws Exception {
//        try {
//            setTokenSource(ts);
//            PlToken t = getLexer().readToken(true);
//            if (t.kind == TK_BOF) {
//                return BEGIN_OF_FILE;
//            }
//            if (t.kind == TK_EOF) {
//                popTokenSource();
//                return END_OF_FILE;
//            }
//
////            getLexer().unreadToken(t);
//            ITerm term = expr(EnumSet.of(TK_DOT));
//            if (term == null) {
//                throw new InvalidTermException("Term is null");
//            }
//            if (!(getLexer().readToken(true).kind == TK_EOF)) {
//                throw new InvalidTermException("The entire string could not be read as one term");
//            }
//            return term;
//        } catch (IOException | InvalidTermException ex) {
//            throw new IOException("An I/O error occurred");
//        }
//    }
//
//    private IdentifiedTerm exprA(int maxPriority, EnumSet<TokenKind> delims) throws Exception {
//        IdentifiedTerm leftSide = exprB(maxPriority, delims);
//        if (leftSide == END_OF_FILE) {
//            return leftSide;
//        }
//        //{op(yfx,n) exprA(n-1) | op(yf,n)}*
//        PlToken t = getLexer().readToken(true);
//        for (; isOperator(t, delims); t = getLexer().readToken(true)) {
//            int YFX = this.getOptable().getPriority(t.image, yfx);
//            int YF = this.getOptable().getPriority(t.image, yf);
//            //YF and YFX has a higher priority than the left side expr and less then top limit
//            // if (YF < leftSide.getPriority() && YF > PlDynamicOperatorParser.OP_HIGH) YF = -1;
//            if (YF < leftSide.getPriority() || YF > maxPriority) {
//                YF = -1;
//            }
//            // if (YFX < leftSide.getPriority() && YFX > PlDynamicOperatorParser.OP_HIGH) YFX = -1;
//            if (YFX < leftSide.getPriority() || YFX > maxPriority) {
//                YFX = -1;
//            }
//            //YFX has getPriority() over YF
//            if (YFX >= YF && YFX >= OP_LOW) {
//                IdentifiedTerm ta = exprA(YFX - 1, delims);
//                if (ta != null) {
//                    leftSide = new IdentifiedTerm(
//                            t.image,
//                            yfx,
//                            YFX,
//                            leftSide.getResult(),
//                            ta.getResult());
//                    continue;
//                }
//            }
//            //either YF has priority over YFX or YFX failed
//            if (YF >= OP_LOW) {
//                leftSide = new IdentifiedTerm(
//                        t.image,
//                        yf,
//                        YF,
//                        leftSide.getResult());
//                continue;
//            }
//            break;
//        }
////        getLexer().unreadToken(t);//todo ?????????
//
//        return leftSide;
//    }
//
//    /**
//     * @param maxPriority
//     * @param delims
//     * @return
//     * @throws Exception
//     */
//    private IdentifiedTerm exprB(int maxPriority, EnumSet<TokenKind> delims) throws Exception {
//        //1. op(fx,n) exprA(n-1) | op(fy,n) exprA(n) | expr0
//        IdentifiedTerm left = (IdentifiedTerm) parseLeftSide(delims, maxPriority);
//        if (left == END_OF_FILE) {
//            return left;
//        }
//        //2.left is followed by either xfx, xfy or xf operators, parse these
//        PlToken token = getLexer().readToken(true);
//        for (; isOperator(token, delims); token = getLexer().readToken(true)) {
//            int XFX = getOptable().getPriority(token.image, xfx);
//            int XFY = getOptable().getPriority(token.image, xfy);
//            int XF = getOptable().getPriority(token.image, xf);
//            //check that no operator has a priority higher than permitted
//            //or a lower priority than the left side expression
//            if (XFX > maxPriority || XFX < OP_LOW) {
//                XFX = -1;
//            }
//            if (XFY > maxPriority || XFY < OP_LOW) {
//                XFY = -1;
//            }
//            if (XF > maxPriority || XF < OP_LOW) {
//                XF = -1;
//            }
//
//            //XFX
//            boolean haveAttemptedXFX = false;
//            if (XFX >= XFY && XFX >= XF && XFX >= left.getPriority()) {     //XFX has priority
//                IdentifiedTerm found = exprA(XFX - 1, delims);
//                if (found != null) {
//                    left = new IdentifiedTerm(
//                            token.image,
//                            xfx,
//                            XFX,
//                            left.getResult(),
//                            found.getResult());
////                    continue;
//                } else {
//                    haveAttemptedXFX = true;
//                }
//            }
//            //XFY //XFY has priority, or XFX has failed
//            if ((XFY >= XF) && (XFY >= left.getPriority())) {
//                IdentifiedTerm found = exprA(XFY, delims);
//                if (found != null) {
//                    left = new IdentifiedTerm(
//                            token.image,
//                            xfy,
//                            XFY,
//                            left.getResult(),
//                            found.getResult());
//                    continue;
//                }
//            } else //!!!!!!!!!!!
//                //XF      //XF has priority, or XFX and/or XFY has failed
//                if (XF >= left.getPriority()) {
//                    return new IdentifiedTerm(
//                            token.image,
//                            xf,
//                            XF,
//                            left.getResult());
//                }// else //!!!!!!!!!!!!!!!!!!
////            continue;
//            //2XFX did not have top priority, but XFY failed
//            if (!haveAttemptedXFX && XFX >= left.getPriority()) {
//                IdentifiedTerm found = exprA(XFX - 1, delims);
//                if (found != null) {
//                    left = new IdentifiedTerm(
//                            token.image,
//                            xfx,
//                            XFX,
//                            left.getResult(),
//                            found.getResult());
//                    continue;
//                }
//            }
//            break;
//        }
//        // getLexer().unreadToken(token);
//
//        return left;
//    }
//
//    /**
//     * @param token
//     * @param delims
//     * @return
//     */
//    private boolean isOperator(PlToken token, EnumSet<TokenKind> delims) {
//        final String name = token.getImage();
//        final Set<IdentifiedTerm> ops = this.getOptable().getOperators(name);
//
//        return ops.stream().anyMatch(op -> op.getTextName().equals(name));
//    }
//
//    /**
//     * Parses and returns a valid 'leftside' of an expression.
//     * If the left side starts with a prefix, it consumes other expressions with a lower priority than itself.
//     * If the left side does not have a prefix it must be an expr0.
//     *
//     * @param maxPriority operators with a higher priority than this will effectively end the expression
//     * @return a wrapper of: 1. term correctly structured and 2. the priority of its root operator
//     * @throws InvalidTermException
//     */
//    private ITerm parseLeftSide(EnumSet<TokenKind> delims, int maxPriority) throws Exception {
////        1. prefix expression
//        PlToken token = getLexer().readToken(true);
//        if (isOperator(token, delims)) {
//            int FX = getOptable().getPriority(token.image, fx);
//            int FY = getOptable().getPriority(token.image, fy);
//            if (FY == 0) {
//                FY = -1;
//            }
//
//            if (token.image.equals("-")) {
//                PlToken t = getLexer().readToken(true);
//                if (t.isNumber()) {
//                    return termFactory.createNumber(token.image);
//                } else {
//                    getLexer().unreadToken(t);
//                }
//                //check that no operator has a priority higher than permitted
//                if (FY > maxPriority) {
//                    FY = -1;
//                }
//                if (FX > maxPriority) {
//                    FX = -1;
//                }
//                //FX has priority over FY
//                boolean haveAttemptedFX = false;
//                if (FX >= FY && FX >= OP_LOW) {
//                    IdentifiedTerm found = exprA(FX - 1, delims);    //op(fx, n) exprA(n - 1)
//                    if (found != null) {
//                        return new IdentifiedTerm(
//                                token.image,
//                                fx,
//                                FX,
//                                found.getResult());
//                    } else {
//                        haveAttemptedFX = true;
//                    }
//                }
//                //FY has priority over FX, or FX has failed
//                if (FY >= OP_LOW) {
//                    IdentifiedTerm found = exprA(FY, delims); //op(fy,n) exprA(1200)  or   op(fy,n) exprA(n)
//                    if (found != null) {
//                        return new IdentifiedTerm(
//                                token.image,
//                                fy,
//                                FY,
//                                found.getResult());
//                    }
//                }
//                //FY has priority over FX, but FY failed
//                if (!haveAttemptedFX && FX >= OP_LOW) {
//                    IdentifiedTerm found = exprA(FX - 1, delims);    //op(fx, n) exprA(n - 1)
//                    if (found != null) {
//                        return new IdentifiedTerm(
//                                token.image,
//                                fx,
//                                FX,
//                                found.getResult());
//                    }
//                }
//            }
//            //2. expr0
//            return lastTerm; //nextTerm(true, false);
//        }
//
//        popTokenSource();
//        return END_OF_FILE;
//    }
////        throw new ParserException("The following token could not be identified: "+t1.image);
//
//    /**
//     * @return
//     * @throws Exception
//     */
//    private ITerm expr0_list() throws Exception {
//        return readSequence(LIST, rDelims);// ',' , '|' , ']'
//    }
//
//    /**
//     * @return
//     * @throws Exception
//     */
//    private ITerm expr0_arglist() throws Exception {
//        return readSequence(ARGS, rDelims);//  ',' , '|' , ')'
//    }
//
//    /**
//     * @return
//     * @throws Exception
//     */
//    private ITerm expr0_block() throws Exception {
//        return readSequence(BLOCK, EnumSet.of(TK_COMMA, TK_RPAREN));
//    }
//
//    /**
//     * @return
//     * @throws Exception
//     */
//    private ITerm expr0_bypass() throws Exception {
//        return readSequence(BYPASS, rDelims);////  ',' , '|' , '}'
//    }
//
//    /**
//     * @param kind
//     * @param rDelims
//     * @return
//     * @throws Exception
//     */
//    protected ITerm readSequence(ListTerm.Kind kind, EnumSet<TokenKind> rDelims) throws Exception {
//        ITerm tail = NIL;
//        List<ITerm> heads = new ArrayList<>();
//        for (; ; ) {
//            PlToken t = getLexer().readToken(true);
//            if (rDelims.contains(t.kind)) {
//                return new ListTerm(kind, heads, tail);
//            } else {
//                switch (t.kind) {
//                    case TK_EOF:
//                        throw new ExecutionError(SYNTAX_ERROR, new HtFunctorName("Premature EOF", -1));
//                    case TK_LPAREN:
//                        parentheses++;
//                        lastTerm = (getLexer().getLastToken().kind == TK_FUNCTOR_BEGIN) ?
//                                expr0_arglist() : expr0_block();
////                        getLexer().unreadToken(t);
//                        break;
//                    case TK_RPAREN:
//                        if (--parentheses < 0) {
//                            throw new ParserException("Extra rparen");
//                        }
//                        return lastSequence;
//                    case TK_LBRACKET:
//                        brackets++;
//                        lastTerm = lastSequence = expr0_list();
////                        getLexer().unreadToken(t);
//                        break;
//                    case TK_RBRACKET:
//                        if (--brackets < 0) {
//                            throw new ParserException("Extra rbracket");
//                        }
//                        return lastSequence;
//                    case TK_LBRACE:
//                        braces++;
//                        lastTerm = lastSequence = expr0_bypass();
////                        getLexer().unreadToken(t);
//                        break;
//                    case TK_RBRACE:
//                        if (--braces < 0) {
//                            throw new ParserException("Extra rbrace");
//                        }
//                        return lastSequence;
//                    case TK_D_QUOTE:
//                        if (++dquotes % 2 != 0) {
//                            if (options(TK_D_QUOTE)) {
//                                lastTerm = readString(TK_D_QUOTE);
//                            }
//                        } else {
//                            return lastTerm;
//                        }
//                        rDelims.clear();
//                        rDelims.add(t.kind);
//                        return readSequence(kind, rDelims);//char codelist
//                    case TK_S_QUOTE:
//                        if (++squotes % 2 != 0) {
//                            if (options(TK_S_QUOTE)) {
//                                lastTerm = readString(TK_S_QUOTE);
//                            }
//                        } else {
//                            return lastTerm;
//                        }
//                        rDelims.clear();
//                        rDelims.add(t.kind);
//                        return lastTerm;
//                    case TK_B_QUOTE:
//                        if (++bquotes % 2 != 0) {
//                            if (options(TK_S_QUOTE)) {
//                                lastTerm = readString(TK_S_QUOTE);
//                            }
//                        } else {
//                            return lastTerm;
//                        }
//                        rDelims.clear();
//                        rDelims.add(t.kind);
//                        return lastTerm;
//                    case TK_VAR:
//                        lastTerm = termFactory.newVariable(t.image);
//                        return handleFunctor(lastTerm);
//                    case TK_FUNCTOR_BEGIN:
//                        lastTerm = compound(t.image, (ListTerm) lastSequence);
//                        return handleFunctor(lastTerm);
//                    case TK_INTEGER_LITERAL:
//                        lastTerm = termFactory.newAtomic(Integer.parseInt(t.image));
//                        return handleFunctor(lastTerm);
//                    case TK_FLOATING_POINT_LITERAL:
//                        lastTerm = termFactory.newAtomic(Double.parseDouble(t.image));
//                        return handleFunctor(lastTerm);
////                  case TK_CHARACTER_LITERAL:
////                        break;
////                    case TK_STRING_LITERAL:
////
////                    case TK_LOWERCASE:
////                        nextTerm(true);
////                        break;
////                    case TK_UPPERCASE:
////                        break;
////                    case TK_SYMBOL:
////                        break;
//                    case TK_COMMA:
//                        lastTerm = expr(rDelims);
//                        heads.add(lastTerm);
//                        break;
//                    case TK_SEMICOLON:
//                        break;
//                    case TK_COLON:
//                        break;
//                    case TK_CONS://  ',' , '|' , ')]}' =>
//                        final EnumSet<TokenKind> rd = EnumSet.of(t.kind);
//                        lastTerm = tail = expr(rd);//VAR OR LIST
//                        break;
//                    default:
//                        if (!heads.isEmpty()) {
//                            throw new ParserException("The expression: %s\nis not followed by either a ',' or '|' or ']'.");
//                        } else {
//
//                        }
//                }
//            }
//        }
//    }
//
//    /**
//     * @param term
//     * @return
//     * @throws Exception
//     */
//    protected ITerm handleFunctor(ITerm term) throws Exception {
//        PlToken t = getLexer().readToken(true);
//        if (t.kind == TK_LPAREN) {
//            if (term.isAtom()) {
//                lastSequence = readSequence(ARGS, rDelims);//',' , '|' , ')'
//                getLexer().unreadToken(t);//pushBack )
//                t = getLexer().readToken(true);
//                if (t.kind == TK_RPAREN) {
//                    if (term.isAtom()) {
//                        return lastTerm = termFactory.newFunctor((IFunctor) term, (ListTerm) lastSequence);
//                    }
//                }
//            } else {
//                lastSequence = expr0_block();
//                // lastSequence=readSequence(BLOCK, expr0_block())
//            }
//        }
//
//        return lastTerm;
//    }
//
//    /**
//     * @param tkDQuote
//     * @return
//     */
//    public StringTerm readString(TokenKind tkDQuote) {
//        return new StringTerm(0);
//    }
//
//    /**
//     * @param tkDQuote
//     * @return
//     */
//    private boolean options(TokenKind tkDQuote) {
//        return map.get(tkDQuote) != null;
//    }
//
//    /**
//     * @param delims
//     * @return
//     * @throws Exception
//     */
//    private ITerm expr(EnumSet<TokenKind> delims) throws Exception {
//        IdentifiedTerm term = exprA(OP_HIGH, delims);
//        if (term == END_OF_FILE) {
//            return term;
//        }
//        return term.getResult();
//    }

    /**
     * @param sb
     */
    public void toString0(StringBuilder sb) {

    }

    /**
     * @return
     */
    @Override
    public PlLexer getTokenSource() {
        return tokenSourceStack.peek();
    }

    /**
     * @param source
     */
    public void setTokenSource(PlLexer source) {
        if (source.isOpen()) {
            logger.info("Adding ts " + requireNonNull(source.getPath(), "" + source.isOpen()));
            if (!tokenSourceStack.contains(source) && source.isOpen()) {
                tokenSourceStack.push(source);
                state = START;
            } else {
                logger.info("Declined adding dup ts " + source.getPath());
            }
        }
    }

    /**
     * @return
     */
    @Override
    public PlLexer popTokenSource() throws IOException {
        if (!tokenSourceStack.isEmpty()) {
            PlLexer ts = tokenSourceStack.peek();
            ts.getInputStream().removeListener(ts);
            ts = tokenSourceStack.pop();
            ts.getInputStream().addListener(ts);

            logger.info("Popping TS " + ts + "... ");
            ts.close();
            if (tokenSourceStack.isEmpty()) {
                state = PlPrologParser.ParserState.FINISH;
            }
            return ts;
        }
        return null;
    }

    /**
     * @return
     */
    public PlLexer getLexer() {
        return getTokenSource();
    }

    /**
     * @param kind
     * @return
     */
    private TokenKind calcPairDelim(TokenKind kind) {
        switch (kind) {
            case TK_LPAREN:
                return TK_RPAREN;
            case TK_LBRACKET:
                return TK_RBRACKET;
            case TK_LBRACE:
                return TK_RBRACE;
            //
            case TK_RPAREN:
                return TK_LPAREN;
            case TK_RBRACKET:
                return TK_LBRACKET;
            case TK_RBRACE:
                return TK_LBRACE;
            //
            case TK_S_QUOTE:
                return TK_S_QUOTE;
            case TK_D_QUOTE:
                return TK_D_QUOTE;
            case TK_B_QUOTE:
                return TK_B_QUOTE;
            default:
                throw new IllegalStateException("Unexpected value: " + kind);
        }
    }

    /**
     * @param elements
     * @return
     */
    private ITerm[] flatten(List<ITerm> elements) {
        return elements.toArray(new ITerm[elements.size()]);
    }

    /**
     *
     */
    private static class StateRecord {
        private final ParserState parserState;
        private final EnumSet<Associativity> associativities;
        private final EnumSet<DirectiveKind> directiveKinds;
        private final EnumSet<TokenKind> rdelims;
        private final int priority;
        private final PlToken token;

        /**
         * @param parserState
         * @param associativities
         * @param directiveKinds
         * @param rdelims
         * @param priority
         * @param token
         */
        public StateRecord(ParserState parserState,
                           EnumSet<Associativity> associativities,
                           EnumSet<DirectiveKind> directiveKinds,
                           EnumSet<TokenKind> rdelims,
                           int priority,
                           PlToken token) {

            this.parserState = parserState;
            this.associativities = associativities;
            this.directiveKinds = directiveKinds;
            this.rdelims = rdelims;
            this.priority = priority;
            this.token = token;
        }

        public ParserState getParserState() {
            return parserState;
        }

        public EnumSet<Associativity> getAssociativities() {
            return associativities;
        }

        public int getPriority() {
            return priority;
        }


        public EnumSet<DirectiveKind> getDirectiveKinds() {
            return directiveKinds;
        }

        public EnumSet<TokenKind> getRdelims() {
            return rdelims;
        }

        public PlToken getToken() {
            return token;
        }

    }
}
