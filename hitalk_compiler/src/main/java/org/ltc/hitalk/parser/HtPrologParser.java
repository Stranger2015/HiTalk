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
import org.ltc.hitalk.term.IdentifiedTerm.Fixity;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.term.ListTerm.Kind;
import org.ltc.hitalk.term.io.HiTalkInputStream;
import org.ltc.hitalk.wam.compiler.HtFunctor;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.ltc.hitalk.wam.compiler.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

import static java.util.EnumSet.of;
import static org.ltc.hitalk.core.BaseApp.getAppContext;
import static org.ltc.hitalk.core.utils.TermUtilities.convertToClause;
import static org.ltc.hitalk.parser.Directive.DirectiveKind.*;
import static org.ltc.hitalk.parser.ParserState.*;
import static org.ltc.hitalk.parser.PlToken.TokenKind.*;
import static org.ltc.hitalk.parser.PlToken.newToken;
import static org.ltc.hitalk.parser.PrologAtoms.*;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity.*;
import static org.ltc.hitalk.wam.compiler.Language.PROLOG;

/**
 *
 */
public class HtPrologParser implements IParser {
    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    ParserState state = START;
    private EnumSet<Associativity> assocs;
    private EnumSet<DirectiveKind> directiveKinds;
    private EnumSet<TokenKind> rDelims;
    private int maxPriority;
    private PlToken token;

    public Deque<StateRecord> states = new ArrayDeque<>();

    public static final String BEGIN_OF_FILE_STRING = "begin_of_file";
    public static final String END_OF_FILE_STRING = "end_of_file";
    public static final IdentifiedTerm END_OF_FILE = new IdentifiedTerm(END_OF_FILE_STRING);
    public static final IdentifiedTerm BEGIN_OF_FILE = new IdentifiedTerm(BEGIN_OF_FILE_STRING);
    public static final String ANONYMOUS = "_";

    protected static final int MAX_PRIORITY = 1200;
    protected static final int MIN_PRIORITY = 0;

    protected final Deque<PlLexer> tokenSourceStack = new ArrayDeque<>();

    protected IOperatorTable operatorTable;
    protected IVafInterner interner;
    protected ITermFactory termFactory;

    /**
     * Holds the variable scoping context for the current sentence.
     */
    protected Map<Integer, HtVariable> variableContext = new HashMap<>();
    protected IdentifiedTerm operator;

    protected ITerm lastTerm;

    protected int braces;
    protected int parentheses;
    protected int brackets;

    protected int squotes;
    protected int dquotes;
    protected int bquotes;

    protected int currPriority;
    protected Predicate<ITerm> rDelimsCondition;
    protected ParserStateCompleter completer;

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
        this.interner = interner;
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
     * @param operatorName
     * @param priority
     * @param assoc
     */
    public void setOperator(String operatorName, int priority, Associativity assoc) {
        Map<Fixity, IdentifiedTerm> ops = operatorTable.getOperatorsMatchingNameByFixity(operatorName);
        if (ops == null || ops.isEmpty()) {
            int arity = calcArity(assoc);
            int name = interner.internFunctorName(operatorName, arity);
            operatorTable.setOperator(name, operatorName, priority, assoc);
        }
    }

    /**
     * @param assoc
     * @return
     */
    protected int calcArity(Associativity assoc) {
        return assoc.arity;
    }

    /**
     * Interns an operators name as a name of appropriate arity for the operators fixity, and sets the operator in
     * the operator table.
     *
     * @param operatorName The name of the operator to create.
     * @param priority     The priority of the operator, zero unsets it.
     * @param assoc        The operators assoc.
     */
    void op(String operatorName, int priority, Associativity assoc) {
        int arity;
        if ((assoc == xfy) || (assoc == yfx) || (assoc == xfx)) {
            arity = 2;
        } else {
            arity = 1;
        }

        int name = getInterner().internFunctorName(operatorName, arity);
        getOptable().setOperator(name, operatorName, priority, assoc);
    }

    /**
     * @param priority
     * @param assoc
     * @param operatorNames
     */
    public void op(int priority, Associativity assoc, String... operatorNames) {
        for (final String operatorName : operatorNames) {
            op(operatorName, priority, assoc);
        }
    }

    /**
     * @return
     */
    public HtPrologParser getParser() {
        return null;
    }

    /**
     * @return
     */
    public IVafInterner getInterner() {
        return null;
    }

    /**
     * @param interner
     */
    public void setInterner(IVafInterner interner) {

    }

    /**
     * @return
     */
    public ITermFactory getFactory() {
        return null;
    }

    /**
     * @return
     */
    public IOperatorTable getOptable() {
        return null;
    }

    /**
     * @param optable
     */
    public void setOptable(IOperatorTable optable) {

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
        return null;
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

        interner.internFunctorName(PrologAtoms.NIL, 0);
        interner.internFunctorName(TRUE, 0);
        interner.internFunctorName(FAIL, 0);
        interner.internFunctorName(FALSE, 0);
        interner.internFunctorName(CUT, 0);
    }

    // * BNF part 2: Parser
    //================================================================================================================
    //   term ::=
    //      exprA(1200)
    //============================
    //   exprA(n) ::=
    //      exprB(n) { op(yfx,n) exprA(n-1) | op(yf,n) }*
//============================
    //  exprB(n) ::=
    //      exprC(n-1) { op(xfx,n) exprA(n-1) | op(xfy,n) exprA(n) | op(xf,n) }*
    //   // exprC is called parseLeftSide in the code
    //============================
    //  exprC(n) ::=
    //      '-' integer | '-' float |
    //      op( fx,n ) exprA(n-1) |
    //      op( hx,n ) exprA(n-1) |
    //      op( fy,n ) exprA(n) |
    //      op( hy,n ) exprA(n) |
    //                 exprA(n)
    //=================================================================================================================
    //  expr_A0 ::=
    //      integer |
    //      float |
    //      atom |
    //      variable |
    //      list     |
    //      functor
    //============================
    // functor ::= functorName args
    //============================
    // functorName ::= expr_A0
    // ============================
    // args ;;= '(' sequence ')'
    //----------------------------
    // list ::= '[' sequence ']'
//===========================
//     block ::= '('  { exprA(1200) }* ')'  //block
//============================
//     bypass_blk ::= '{' { exprA(1200) }* '}'
//============================
//     op(type,n) ::= atom | { symbol }+
//============================
//     sequence ::= [ heads tail ]
//============================
//     heads ::= [ exprA(1200) { ',' exprA(1200) }* ]
//============================
//     tail ::=  [ '|' (variable | list) ]

    /**
     * //  expr_A0 ::=
     * //      integer |
     * //      float |
     * //      atom |
     * //      variable |
     * //      list     |
     * //      functor
     * //============================
     * // functor ::= functorName args
     * //============================
     * // functorName ::= expr_A0
     *
     * @param token
     * @return
     * @throws Exception
     */
    public ITerm
    completeExprA0(PlToken token) throws Exception {
        EnumSet<Associativity> assocs = of(x);
        EnumSet<DirectiveKind> directiveKinds = of(DK_IF, DK_ENCODING, DK_HILOG);
        EnumSet<TokenKind> rDelims = of(TK_DOT);
        switch (token.kind) {
            case TK_BOF:
                lastTerm = BEGIN_OF_FILE;
                break;
            case TK_EOF:
                lastTerm = END_OF_FILE;
                popTokenSource();
                break;
            case TK_DOT:
                break;
            case TK_LPAREN:
                if (isFunctorBegin()) {
                    directiveKinds = of(DK_IF);
                    rDelims.add(TK_RPAREN);
                    newState(EXPR_A0_ARGS,
                            assocs,
                            directiveKinds,
                            rDelims,
                            MAX_PRIORITY,
                            token);
                    if (lastTerm.isHiLog()) {
                        termFactory.newHiLogFunctor(lastTerm, new ListTerm(0));//fixme
                    } else if (lastTerm.isAtom()) {
                        int name = ((IFunctor) lastTerm).getName();
                        int arity = ((IFunctor) lastTerm).getArity();
                        lastTerm = new HtFunctor(name, new ListTerm(arity));
                    } else {
                        throw new ParserException("Possibly undeclared HILOG term ->%s<- ", token);
                    }
                } else {
                    newState(EXPR_A,
                            assocs,
                            directiveKinds,
                            rDelims,
                            MAX_PRIORITY,
                            token);
                    if (rDelims.contains(token.kind)) {
                        break;
                    }
                }
                break;
            case TK_LBRACKET:
                rDelims.add(TK_RBRACKET);
                newState(EXPR_A0_BRACKET,
                        of(x),
                        of(DK_IF),
                        of(TK_DOT),
                        MAX_PRIORITY,
                        token);
                break;
            case TK_LBRACE:
                rDelims.add(TK_RBRACE);
                newState(EXPR_A0_BRACE,
                        assocs,
                        directiveKinds,
                        rDelims,
                        MAX_PRIORITY,
                        token);
                break;
            case TK_RBRACE:
                if (--braces < 0) {
                    throw new ParserException("Extra right brace.");
                }
                states.pop();
                break;
            case TK_RBRACKET:
                if (--brackets < 0) {
                    throw new ParserException("Extra right bracket.");
                }
                states.pop();
                break;
            case TK_RPAREN:
                if (--parentheses < 0) {
                    throw new ParserException("Extra right parenthesis.");
                }
                states.pop();
                break;
            case TK_D_QUOTE:
                if (++dquotes % 2 != 0) {
                    if (options(TK_D_QUOTE)) {
                        lastTerm = readString(TK_D_QUOTE);
                    }
                } else {
                    return lastTerm;
                }
                rDelims.clear();
                rDelims.add(token.kind);
                newState(SEQUENCE,
                        of(x),
                        of(DK_IF),
                        rDelims,
                        currPriority,
                        token);
            case TK_S_QUOTE:
                if (++squotes % 2 != 0) {
                    if (options(TK_S_QUOTE)) {
                        lastTerm = readString(TK_S_QUOTE);
                    }
                } else {
                    return lastTerm;
                }
                rDelims.clear();
                rDelims.add(token.kind);
                newState(SEQUENCE,
                        of(x),
                        of(DK_IF),
                        rDelims,
                        currPriority,
                        token);
            case TK_B_QUOTE:
                if (++bquotes % 2 != 0) {
                    if (options(TK_B_QUOTE)) {
                        lastTerm = readString(TK_B_QUOTE);
                    }
                } else {
                    return lastTerm;
                }
                rDelims.clear();
                rDelims.add(token.kind);
//                return readSequence(LIST, rDelims);//char codelist
                newState(SEQUENCE,
                        of(x),
                        of(DK_IF),
                        rDelims,
                        currPriority,
                        token);
//            case TK_INTEGER_LITERAL:
//                break;
//            case TK_DECIMAL_LITERAL:
//                break;
//            case TK_HEX_LITERAL:
//                break;
//            case TK_FLOATING_POINT_LITERAL:
//                break;
//            case TK_DECIMAL_EXPONENT:
//                break;
            case TK_CHARACTER_LITERAL:
                break;
            case TK_STRING_LITERAL:
                break;
            case TK_VAR:
                lastTerm = termFactory.newVariable(token.image);
                break;
            case TK_ATOM:
            case TK_QUOTED_NAME:
            case TK_SYMBOLIC_NAME:
                lastTerm = termFactory.newFunctor(token.image, ListTerm.NIL);
                break;
//            case TK_DIGIT:
//                break;
//            case TK_ANY_CHAR:
//                break;
//            case TK_LOWERCASE:
//                break;
//            case TK_UPPERCASE:
//                break;
//            case TK_SYMBOL:
//                break;
//            case TK_COMMA:
//                break;
//            case TK_SEMICOLON:
//                break;
//            case TK_COLON:
//                break;
//            case TK_CONS:
//                break;
            default:
                throw new IllegalStateException("Unused or unknown value: " + token.kind);
        }

        return lastTerm;
    }

    private ITerm readSequence(Kind kind, EnumSet<TokenKind> rDelims) {
        return null;//fixme
    }

    private ITerm readString(TokenKind quote) {
        return null;//todo
    }

    /**
     * @param quote
     * @return
     */
    private boolean options(TokenKind quote) {
        return false;
    }

    /**
     * @return
     * @throws Exception
     */
    @Override
    public ITerm expr() throws Exception {
        PlToken token;
        EnumSet<Associativity> assocs = of(x);
        EnumSet<DirectiveKind> directiveKinds = of(DK_IF, DK_ENCODING, DK_HILOG);
        EnumSet<TokenKind> rDelims = of(TK_DOT);
        newState(START, assocs, directiveKinds, rDelims, MAX_PRIORITY, newToken(TK_BOF));
        lastTerm = BEGIN_OF_FILE;
        currPriority = MAX_PRIORITY;
        for (; ; ) {
            token = getLexer().getToken(true);
            switch (states.peek().getParserState()) {
                case START:
                    newState(EXPR_A,
                            of(yfx, yf),
                            of(DK_IF, DK_ENCODING, DK_HILOG),
                            of(TK_DOT),
                            currPriority,
                            token);
                    break;
//==========================================================================================================
                case EXPR_A:
                    //   exprA(n) ::=
                    //      exprB(n) { op(yfx,n) exprA(n-1) | op(yf,n) }*
                    newState(EXPR_B,
                            of(xfx, xfy, xf),
                            of(DK_IF),
                            of(TK_DOT),
                            currPriority,
                            token);
                    break;
                case EXPR_A_EXIT:
                    token = getLexer().readToken(true);
                    completeExprA(currPriority, token, (IdentifiedTerm) lastTerm);
                    if (lastTerm == END_OF_FILE) {
                        return lastTerm;
                    }

                    break;
                case EXPR_B:
//                    exprB(n) ::=
//                        exprC(n-1) { op(xfx,n) exprA(n-1) | op(xfy,n) exprA(n) | op(xf,n) }*
// exprC is called parseLeftSide in the code
                    newState(EXPR_C,
                            of(fx, fy, hx, hy),
                            of(DK_IF, DK_HILOG),
                            of(TK_DOT),
                            currPriority - 1,
                            token);
                    break;
                case EXPR_B_EXIT:

                    break;
                case EXPR_C:
                    //  exprC(n) ::=
                    //      '-' integer | '-' float |
                    //      op( fx,n ) exprA(n-1) |
                    //      op( hx,n ) exprA(n-1) |
                    //      op( fy,n ) exprA(n) |
                    //      op( hy,n ) exprA(n) |
                    //                 exprA(n)
                    if (token.image.equals("-") || token.image.equals("+")) {
                        PlToken token1 = getLexer().readToken(true);
                        lastTerm = readNumber(token.image, token1);
                        if (lastTerm == null) {
                            getLexer().unreadToken(token1);
                        }
                    }
                case EXPR_C_EXIT:
                    completeExprC(token);
                    break;
                case EXPR_A0:
                    //  expr_A0 ::=
                    //      integer |
                    //      float |
                    //      atom |
                    //      variable |
                    //      list     |
                    //      functor
                    // ============================
                    // functor ::= functorName args
                    //============================
                    // functorName ::= expr_A0
                    newState(EXPR_A0,
                            of(x),
                            of(DK_IF),
                            rDelims,
                            currPriority,
                            token);
                    break;
                case EXPR_A0_EXIT:
                    completeExprAn(currPriority, token, (IdentifiedTerm) lastTerm);
                    break;
                case FINISH:
//                    newState(FINISH,
//                            of(x),
//                            of(DK_IF),
//                            of(TK_DOT),
//                            currPriority,
//                            newToken(TK_EOF));
//                    break;
                case EXPR_A0_BRACE:
                    break;
                case EXPR_A0_BRACE_EXIT:
                    break;
                case EXPR_A0_BRACKET:
                    break;
                case EXPR_A0_BRACKET_EXIT:
                    break;
                case EXPR_A0_ARGS:

                    break;
                case EXPR_A0_ARGS_EXIT:
                    rDelims = of(TK_COMMA, TK_CONS, TK_RPAREN);
//                    token = getLexer().getToken(true);
//                    if (rDelims.contains(token.kind)) {
//                        break;
//                    }
                    newState(EXPR_A0_HEADS,
                            of(x),
                            directiveKinds,
                            rDelims,
                            MAX_PRIORITY,
                            token);
//                    continue;
                    newState(EXPR_A,
                            assocs,
                            directiveKinds,
                            rDelims,
                            MAX_PRIORITY,
                            token);
                    break;
                case EXPR_A0_HEADS:
                    newState(EXPR_A0_TAIL,
                            of(yfx, yf),
                            of(DK_IF),
                            of(TK_COMMA, TK_CONS, TK_RPAREN),
                            MAX_PRIORITY,
                            token);
                    break;
                case EXPR_A0_HEADS_EXIT:

                    break;
                case EXPR_A0_TAIL:

                    break;
                case EXPR_A0_TAIL_EXIT:
                    token = getLexer().getToken(true);
                    if (EnumSet.of(TK_VAR, TK_LBRACKET).contains(token.kind)) {
                        if (token.kind == TK_LBRACKET) {
//                            final StateRecord listState;
                        }
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + states.peek().getParserState());
            }

            return lastTerm;
        }
    }

    /**
     * @param rDelims
     * @param currPriority
     * @param token
     * @throws Exception
     */
    protected void completeExprC(EnumSet<TokenKind> rDelims, int currPriority, PlToken token) throws Exception {
        parseLeftSide(of(TK_DOT), currPriority, token);
    }

    /**
     * @param token
     * @throws Exception
     */
    protected void completeExprB(PlToken token) throws Exception {//todo fixme
        //1. op(fx,n) exprA(n-1) | op(fy,n) exprA(n) | expr0
        IdentifiedTerm left = (IdentifiedTerm) parseLeftSide(rDelims, maxPriority, token);
        if (left == END_OF_FILE) {
            lastTerm = left;
        }
        //2.left is followed by either xfx, xfy or xf operators, parse these
        token = getLexer().readToken(true);
        for (; isOperator(token, rDelims); token = getLexer().readToken(true)) {
            int priorityXFX = getOptable().getPriority(token.image, xfx);
            int priorityXFY = getOptable().getPriority(token.image, xfy);
            int priorityXF = getOptable().getPriority(token.image, xf);
            //check that no operator has a priority higher than permitted
            //or a lower priority than the left side expression
            if (priorityXFX > maxPriority || priorityXFX < MIN_PRIORITY) {
                priorityXFX = -1;
            }
            if (priorityXFY > maxPriority || priorityXFY < MIN_PRIORITY) {
                priorityXFY = -1;
            }
            if (priorityXF > maxPriority || priorityXF < MIN_PRIORITY) {
                priorityXF = -1;
            }
            //priorityXFX
            boolean haveAttemptedXFX = false;
            //priorityXFX has priority
            if (priorityXFX >= priorityXFY && priorityXFX >= priorityXF && priorityXFX >= left.getPriority()) {
//                            IdentifiedTerm found = exprA(priorityXFX - 1, delims);
                if (lastTerm != null) {
                    left = new IdentifiedTerm(
                            token.image,
                            xfx,
                            priorityXFX,
                            left.getResult(),
                            ((IdentifiedTerm) lastTerm).getResult());
//                    continue;
                } else {
                    haveAttemptedXFX = true;
                }
            }
            //priorityXFY //priorityXFY has priority, or priorityXFX has failed
            if ((priorityXFY >= priorityXF) && (priorityXFY >= left.getPriority())) {
//                            IdentifiedTerm found = exprA(priorityXFY, delims);
                newState(EXPR_A,
                        assocs,
                        directiveKinds,
                        rDelims,
                        priorityXFX,
                        token);

                if (lastTerm != null) {
                    left = new IdentifiedTerm(
                            token.image,
                            xfy,
                            priorityXFY,
                            left.getResult(),
                            ((IdentifiedTerm) lastTerm).getResult());
                    continue;
                }
            } else //!!!!!!!!!!!
                //priorityXF      //priorityXF has priority, or priorityXFX and/or priorityXFY has failed
                if (priorityXF >= left.getPriority()) {
                    lastTerm = new IdentifiedTerm(
                            token.image,
                            xf,
                            priorityXF,
                            left.getResult());
                }// else //!!!!!!!!!!!!!!!!!!
//            continue;
            //2XFX did not have top priority, but priorityXFY failed
            if (!haveAttemptedXFX && priorityXFX >= left.getPriority()) {
//                            IdentifiedTerm found = exprA(priorityXFX - 1, delims);
                newState(EXPR_A,
                        assocs,
                        directiveKinds,
                        rDelims,
                        priorityXFX,
                        token);
                //todo
                if (lastTerm != null) {
                    left = new IdentifiedTerm(
                            token.image,
                            xfx,
                            priorityXFX,
                            left.getResult(),
                            ((IdentifiedTerm) lastTerm).getResult());
                    continue;
                }
            }
            break;
        }
    }

    protected void completeExprAn(int currPriority, PlToken token, IdentifiedTerm leftSide) throws Exception {
        for (; isOperator(token, rDelims); token = getLexer().readToken(true)) {
            int priorityYFX = this.getOptable().getPriority(token.image, yfx);
            int priorityYF = this.getOptable().getPriority(token.image, yf);
            //YF and priorityYFX has a higher priority than the left side expr and less then top limit
            // if (YF < leftSide.getPriority() && YF > PlDynamicOperatorParser.OP_HIGH) YF = -1;

            if (priorityYF < leftSide.getPriority() || priorityYF > this.currPriority) {
                priorityYF = -1;
            }
            // if (priorityYFX < leftSide.getPriority() && priorityYFX > MAX_PRIORITY) priorityYFX = -1;
            if (priorityYFX < leftSide.getPriority() || priorityYFX > this.currPriority) {
                priorityYFX = -1;
            }
            //priorityYFX has getPriority() over YF
            if (priorityYFX >= priorityYF && priorityYFX >= MIN_PRIORITY) {
                newState(EXPR_A,
                        assocs,
                        directiveKinds,
                        rDelims,
                        priorityYFX - 1,
                        token);
                if (lastTerm != null) {
                    leftSide = new IdentifiedTerm(
                            token.image,
                            yfx,
                            priorityYFX,
                            leftSide.getResult(),
                            ((IdentifiedTerm) lastTerm).getResult());
                }
            } else {
                //either YF has priority over priorityYFX or priorityYFX failed
                if (priorityYF >= MIN_PRIORITY) {
                    leftSide = new IdentifiedTerm(
                            token.image,
                            yf,
                            priorityYF,
                            leftSide.getResult());
                }
            }
        }
    }

    /**
     * exprA(n) ::=
     * exprB(n) { op(yfx,n) exprA(n-1) | op(yf,n) }*
     * //============================
     * //  exprB(n) ::=
     * //      exprC(n-1) { op(xfx,n) exprA(n-1) | op(xfy,n) exprA(n) | op(xf,n) }*
     * //   // exprC is called parseLeftSide in the code
     * //============================
     * //  exprC(n) ::=
     * //      '-' integer | '-' float |
     * //      op( fx,n ) exprA(n-1) |
     * //      op( hx,n ) exprA(n-1) |
     * //      op( fy,n ) exprA(n) |
     * //      op( hy,n ) exprA(n) |
     * //                 exprA(n)
     * //=================================================================================================================
     * //  expr_A0 ::=
     * //      integer |
     * //      float |
     * //      atom |
     * //      variable |
     * //      list     |
     * //      functor
     * //============================
     * // functor ::= functorName args
     * //============================
     * // functorName ::= expr_A0
     * // ============================
     * // args ;;= '(' sequence ')'
     * //----------------------------
     * // list ::= '[' sequence ']'
     * //===========================
     * //     block ::= '('  { exprA(1200) }* ')'  //block
     * //============================
     * //     bypass_blk ::= '{' { exprA(1200) }* '}'
     * //============================
     * //     op(type,n) ::= atom | { symbol }+
     * //============================
     * //     sequence ::= [ heads tail ]
     * //============================
     *
     * @param currPriority
     * @param token
     * @param leftSide
     * @throws Exception
     */
    protected void completeExprA(int currPriority, PlToken token, IdentifiedTerm leftSide) throws Exception {
        if (currPriority == 0) {
            completeExprA0(token);
        } else {
            completeExprAn(currPriority, token, leftSide);
        }
    }

    /**
     * @return
     */
    protected StateRecord getState(ParserState state) {
        this.state = state;
        return new StateRecord(state, of(x), of(DK_IF), rDelims, currPriority, getLexer().getLastToken());
    }

    protected Set<IdentifiedTerm> tryOperators(String name, EnumSet<Associativity> assocs, int maxPriority) {
        final Set<IdentifiedTerm> result = new HashSet<>();
        for (Associativity assoc : assocs) {
            result.addAll(getOptable().getOperators(name, assoc, maxPriority));
        }
        return result;
    }


    protected ITerm readNumber(String prefix, PlToken token) {
        final ITerm result;
        if (token.kind == TK_INTEGER_LITERAL || token.kind == TK_FLOATING_POINT_LITERAL) {
            result = termFactory.createNumber(prefix + token.image);
        } else {
            throw new ParserException("Bad number " + token);
        }
        return result;
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
//    protected IdentifiedTerm exprA(int maxPriority, EnumSet<TokenKind> delims) throws Exception {
//        IdentifiedTerm leftSide = exprB(maxPriority, delims);
//        if (leftSide == END_OF_FILE) {
//            return leftSide;
//        }
//        //{op(yfx,n) exprA(n-1) | op(yf,n)}*
//        PlToken t = getLexer().readToken(true);
//        for (; isOperator(t, delims); t = getLexer().readToken(true)) {
//            int priorityYFX = this.getOptable().getPriority(t.image, yfx);
//            int YF = this.getOptable().getPriority(t.image, yf);
//            //YF and priorityYFX has a higher priority than the left side expr and less then top limit
//            // if (YF < leftSide.getPriority() && YF > PlDynamicOperatorParser.OP_HIGH) YF = -1;
//            if (YF < leftSide.getPriority() || YF > maxPriority) {
//                YF = -1;
//            }
//            // if (priorityYFX < leftSide.getPriority() && priorityYFX > PlDynamicOperatorParser.OP_HIGH) priorityYFX = -1;
//            if (priorityYFX < leftSide.getPriority() || priorityYFX > maxPriority) {
//                priorityYFX = -1;
//            }
//            //priorityYFX has getPriority() over YF
//            if (priorityYFX >= YF && priorityYFX >= MIN_PRIORITY) {
//                IdentifiedTerm ta = exprA(priorityYFX - 1, delims);
//                if (ta != null) {
//                    leftSide = new IdentifiedTerm(
//                            t.image,
//                            yfx,
//                            priorityYFX,
//                            leftSide.getResult(),
//                            ta.getResult());
//                    continue;
//                }
//            }
//            //either YF has priority over priorityYFX or priorityYFX failed
//            if (YF >= MIN_PRIORITY) {
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
//    protected IdentifiedTerm exprB(int maxPriority, EnumSet<TokenKind> delims) throws Exception {
//        //1. op(fx,n) exprA(n-1) | op(fy,n) exprA(n) | expr0
//        IdentifiedTerm left = (IdentifiedTerm) parseLeftSide(delims, maxPriority);
//        if (left == END_OF_FILE) {
//            return left;
//        }
//        //2.left is followed by either xfx, xfy or xf operators, parse these
//        PlToken token = getLexer().readToken(true);
//        for (; isOperator(token, delims); token = getLexer().readToken(true)) {
//            int priorityXFX = getOptable().getPriority(token.image, xfx);
//            int priorityXFY = getOptable().getPriority(token.image, xfy);
//            int priorityXF = getOptable().getPriority(token.image, xf);
//            //check that no operator has a priority higher than permitted
//            //or a lower priority than the left side expression
//            if (priorityXFX > maxPriority || priorityXFX < MIN_PRIORITY) {
//                priorityXFX = -1;
//            }
//            if (priorityXFY > maxPriority || priorityXFY < MIN_PRIORITY) {
//                priorityXFY = -1;
//            }
//            if (priorityXF > maxPriority || priorityXF < MIN_PRIORITY) {
//                priorityXF = -1;
//            }
//
//            //priorityXFX
//            boolean haveAttemptedXFX = false;
//            if (priorityXFX >= priorityXFY && priorityXFX >= priorityXF && priorityXFX >= left.getPriority()) {     //priorityXFX has priority
//                IdentifiedTerm found = exprA(priorityXFX - 1, delims);
//                if (found != null) {
//                    left = new IdentifiedTerm(
//                            token.image,
//                            xfx,
//                            priorityXFX,
//                            left.getResult(),
//                            found.getResult());
////                    continue;
//                } else {
//                    haveAttemptedXFX = true;
//                }
//            }
//            //priorityXFY //priorityXFY has priority, or priorityXFX has failed
//            if ((priorityXFY >= priorityXF) && (priorityXFY >= left.getPriority())) {
//                IdentifiedTerm found = exprA(priorityXFY, delims);
//                if (found != null) {
//                    left = new IdentifiedTerm(
//                            token.image,
//                            xfy,
//                            priorityXFY,
//                            left.getResult(),
//                            found.getResult());
//                    continue;
//                }
//            } else //!!!!!!!!!!!
//                //priorityXF      //priorityXF has priority, or priorityXFX and/or priorityXFY has failed
//                if (priorityXF >= left.getPriority()) {
//                    return new IdentifiedTerm(
//                            token.image,
//                            xf,
//                            priorityXF,
//                            left.getResult());
//                }// else //!!!!!!!!!!!!!!!!!!
////            continue;
//            //2XFX did not have top priority, but priorityXFY failed
//            if (!haveAttemptedXFX && priorityXFX >= left.getPriority()) {
//                IdentifiedTerm found = exprA(priorityXFX - 1, delims);
//                if (found != null) {
//                    left = new IdentifiedTerm(
//                            token.image,
//                            xfx,
//                            priorityXFX,
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
//   //
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
//    protected ITerm expr0_list() throws Exception {
//        return readSequence(LIST, rDelims);// ',' , '|' , ']'
//    }
//
//    /**
//     * @return
//     * @throws Exception
//     */
//    protected ITerm expr0_arglist() throws Exception {
//        return readSequence(ARGS, rDelims);//  ',' , '|' , ')'
//    }
//
//    /**
//     * @return
//     * @throws Exception
//     */
//    protected ITerm expr0_block() throws Exception {
//        return readSequence(BLOCK, EnumSet.of(TK_COMMA, TK_RPAREN));
//    }
//
//    /**
//     * @return
//     * @throws Exception
//     */
//    protected ITerm expr0_bypass() throws Exception {
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
//    protected boolean options(TokenKind tkDQuote) {
//        return map.get(tkDQuote) != null;
//    }
//
//    /**
//     * @param delims
//     * @return
//     * @throws Exception
//     */
//    protected ITerm expr(EnumSet<TokenKind> delims) throws Exception {
//        IdentifiedTerm term = exprA(OP_HIGH, delims);
//        if (term == END_OF_FILE) {
//            return term;
//        }
//        return term.getResult();
//    }

    /**
     * Static service to get a term from its string representation
     */
    public ITerm parseSingleTerm(String st) throws Exception {
        PlLexer ts = PlLexer.getPlLexerForString(st);
        return parseSingleTerm(ts);
    }

    /**
     * Static service to get a term from its string representation,
     * providing a specific operator manager
     */
    public ITerm parseSingleTerm(PlLexer ts) throws Exception {
        try {
            setTokenSource(ts);
            PlToken t = getLexer().readToken(true);
            if (t.kind == TK_BOF) {
                return BEGIN_OF_FILE;
            }
            if (t.kind == TK_EOF) {
                popTokenSource();
                return END_OF_FILE;
            }
//            getLexer().unreadToken(t);
            ITerm term = expr();
            if (term == null) {
                throw new InvalidTermException("Term is null");
            }
            if (!(getLexer().readToken(true).kind == TK_EOF)) {
                throw new InvalidTermException("The entire string could not be read as one term");
            }
            return term;
        } catch (IOException | InvalidTermException ex) {
            throw new IOException("An I/O error occurred");
        }
    }

    private PlLexer getLexer() {
        return tokenSourceStack.peek();
    }

    /**
     * @return
     */
    public boolean isFunctorBegin() {
        final PlToken token = getLexer().getLastToken();
        return token.kind == TK_LPAREN && token.isSpacesOccurred();
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

    /**
     * @return
     */
    public HtClause parseClause() throws Exception {
        return convertToClause(termSentence(), getAppContext().getInterner());
    }

    /**
     * @param state
     * @param assocs
     * @param dks
     * @param rDelims
     * @param currPriority
     * @param token
     * @return
     */
    protected StateRecord newState(ParserState state,
                                   EnumSet<Associativity> assocs,
                                   EnumSet<DirectiveKind> dks,
                                   EnumSet<TokenKind> rDelims,
//                                   Predicate<ITerm> rDelimsCondition,
                                   int currPriority,
                                   PlToken token
//                                   ParserStateCompleter completer
    ) {
        this.completer = completer;
//        this.rDelimsCondition = rDelimsCondition;
        final StateRecord sr = createFinishState(state, assocs, dks, rDelims, currPriority, token);
        states.push(sr);
        return sr;
    }

    public StateRecord createFinishState(
            ParserState state,
            EnumSet<Associativity> assocs,
            EnumSet<DirectiveKind> dks,
            EnumSet<TokenKind> rDelims,
            int maxPriority,
            PlToken token) {
        final StateRecord stateRec = new StateRecord(
                state,
                assocs,
                dks,
                rDelims,
                maxPriority,
                token
        );

        states.push(stateRec);
        return stateRec;
    }

    /**
     * @return
     */
    protected StateRecord finish() {
        return new StateRecord(
                FINISH,
                of(x),
                of(DK_IF),
                of(TK_DOT),
                -1,
                newToken(TK_EOF)
        );
    }

    /**
     * @param token
     * @param delims
     * @return
     */
    private boolean isOperator(PlToken token, EnumSet<TokenKind> delims) {
        final String name = token.getImage();
        final Set<IdentifiedTerm> ops = this.getOptable().getOperators(name);

        return ops.stream().anyMatch(op -> op.getTextName().equals(name));
    }

    /**
     * Parses and returns a valid 'leftside' of an expression.
     * If the left side starts with a prefix, it consumes other expressions with a lower priority than itself.
     * If the left side does not have a prefix it must be an expr0.
     *
     * @param maxPriority operators with a higher priority than this will effectively end the expression
     * @param token
     * @return a wrapper of: 1. term correctly structured and 2. the priority of its root operator
     * @throws InvalidTermException
     */
    protected ITerm parseLeftSide(EnumSet<TokenKind> rDelims, int maxPriority, PlToken token) throws Exception {
//        1. prefix expression
//        token = getLexer().readToken(true);
        if (isOperator(token, rDelims)) {
            int priorityFX = getOptable().getPriority(token.image, fx);
            int priorityFY = getOptable().getPriority(token.image, fy);
            if (priorityFY == 0) {
                priorityFY = -1;
            }

            if (token.image.equals("-") || token.image.equals("+")) {
                PlToken t = getLexer().readToken(true);
                if (t.isNumber()) {
                    return termFactory.createNumber(token.image);
                } else {
                    getLexer().unreadToken(t);
                }
                //check that no operator has a priority higher than permitted
                if (priorityFY > maxPriority) {
                    priorityFY = -1;
                }
                if (priorityFX > maxPriority) {
                    priorityFX = -1;
                }
                //priorityFX has priority over priorityFY
                boolean haveAttemptedFX = false;
                if (priorityFX >= priorityFY && priorityFX >= MIN_PRIORITY) {
//                    IdentifiedTerm found = exprA(priorityFX - 1, rDelims);    //op(fx, n) exprA(n - 1)
                    newState(EXPR_A,
                            assocs,
                            directiveKinds,
                            rDelims,
                            priorityFX - 1,
                            token);

                    if (lastTerm != null) {
                        return new IdentifiedTerm(
                                token.image,
                                fx,
                                priorityFX - 1,
                                ((IdentifiedTerm) lastTerm).getResult());
                    } else {
                        haveAttemptedFX = true;
                    }
                }
                //priorityFY has priority over priorityFX, or priorityFX has failed
                if (priorityFY >= MIN_PRIORITY) {
//                    IdentifiedTerm found = exprA(priorityFY, rDelims); //op(fy,n) exprA(1200)  or   op(fy,n) exprA(n)
                    newState(EXPR_A,
                            assocs,
                            directiveKinds,
                            rDelims,
                            priorityFY,
                            token);

                    if (lastTerm != null) {
                        return new IdentifiedTerm(
                                token.image,
                                fy,
                                priorityFY,
                                ((IdentifiedTerm) lastTerm).getResult());
                    }
                }
                //priorityFY has priority over priorityFX, but priorityFY failed
                if (!haveAttemptedFX && priorityFX >= MIN_PRIORITY) {
//                    IdentifiedTerm found = exprA(priorityFX - 1, rDelims);    //op(fx, n) exprA(n - 1)
                    newState(EXPR_A,
                            assocs,
                            directiveKinds,
                            rDelims,
                            priorityFX - 1,
                            token);

                    if (lastTerm != null) {
                        return new IdentifiedTerm(
                                token.image,
                                fx,
                                priorityFX - 1,
                                ((IdentifiedTerm) lastTerm).getResult());
                    }
                }
            }
            //2. expr0
            return lastTerm;
        }

        return lastTerm;
    }

    /**
     * @param sb
     */
    public void toString0(StringBuilder sb) {

    }
}
