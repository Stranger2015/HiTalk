package org.ltc.hitalk.parser;

import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.PlToken.TokenKind;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.term.OpSymbolFunctor;
import org.ltc.hitalk.term.OpSymbolFunctor.Associativity;
import org.ltc.hitalk.term.OpSymbolFunctor.Fixity;
import org.ltc.hitalk.term.io.HiTalkInputStream;
import org.ltc.hitalk.wam.compiler.HtFunctor;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.ltc.hitalk.wam.compiler.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.util.*;

import static java.util.EnumSet.of;
import static org.ltc.hitalk.core.BaseApp.appContext;
import static org.ltc.hitalk.parser.Directive.DirectiveKind.*;
import static org.ltc.hitalk.parser.PlLexer.getPlLexerForString;
import static org.ltc.hitalk.parser.PlToken.TokenKind.*;
import static org.ltc.hitalk.parser.PrologAtoms.*;
import static org.ltc.hitalk.term.OpSymbolFunctor.Associativity.*;
import static org.ltc.hitalk.wam.compiler.Language.PROLOG;

/**
 *
 */
public class HtPrologParser implements IParser<HtClause> {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    public static final String BEGIN_OF_FILE_STRING = "begin_of_file";
    public static final String END_OF_FILE_STRING = "end_of_file";
    public static final IFunctor BEGIN_OF_FILE = new HtFunctor(appContext.getInterner().internFunctorName(BEGIN_OF_FILE_STRING, 0));
    public static final IFunctor END_OF_FILE = new HtFunctor(appContext.getInterner().internFunctorName(END_OF_FILE_STRING, 0));
    public static final String ANONYMOUS = "_";

    public static final int MAX_PRIORITY = 1200;
    public static final int MIN_PRIORITY = 0;


    protected final Deque<PlLexer> tokenSourceStack = new ArrayDeque<>();
    protected ListTerm listTerm = new ListTerm(0);
    protected EnumSet<Associativity> assocs = of(x);
    protected EnumSet<DirectiveKind> dks = of(DK_IF, DK_ENCODING, DK_HILOG);
    protected PlToken token;
    protected IOperatorTable operatorTable;
    protected IVafInterner interner;
    protected ITermFactory termFactory;
    protected Map<ITerm, Integer> offsetsMap;
    protected int tokenStart;
    /**
     * Holds the variable scoping context for the current sentence.
     */
    protected Map<Integer, HtVariable> variableContext = new HashMap<>();
    protected OpSymbolFunctor operator;
    protected ITerm lastTerm;
    protected int braces;
    protected int parentheses;
    protected int brackets;
    protected int squotes;
    protected int dquotes;
    protected int bquotes;
    protected int currPriority;

    public HtPrologParser() {
    }

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
//        setTokenSource(new PlLexer(inputStream, inputStream.getPath()));
        this.interner = interner;
        this.termFactory = factory;
        this.operatorTable = optable;
    }

    public boolean isEndOfTerm(TokenKind tokenKind) {
        return tokenKind == TK_DOT && isEndOfTerm();
    }

    protected boolean isEndOfTerm() {
        return (parentheses == 0) && (brackets == 0) && (braces == 0) &&
                ((squotes % 2) == 0) && ((dquotes % 2) == 0) && ((bquotes % 2) == 0);
    }

    @Override
    public Deque<PlLexer> getTokenSourceStack() {
        return tokenSourceStack;
    }

    /**
     * int arity = calcArity(assoc);
     *
     * @param operatorName
     * @param priority
     * @param assoc
     */
    public void setOperator(String operatorName, int priority, Associativity assoc) {
        Map<Fixity, OpSymbolFunctor> ops = operatorTable.getOperatorsMatchingNameByFixity(operatorName);
        if (ops == null || ops.isEmpty()) {
            int name = interner.internFunctorName(operatorName, assoc.arity);
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
    protected void op(String operatorName, int priority, Associativity assoc) {
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
        return this;
    }

    /**
     * @return
     */
    public EnumSet<Associativity> getAssocs() {
        return assocs;
    }

    /**
     * @return
     */
    public int getCurrPriority() {
        return currPriority;
    }

    /**
     * @return
     */
    public EnumSet<DirectiveKind> getDks() {
        return dks;
    }

    /**
     * @return
     */
    public PlToken getToken() {
        return token;
    }

    /**
     * @return
     */
    public IVafInterner getInterner() {
        return interner;
    }

    /**
     * @param interner
     */
    public void setInterner(IVafInterner interner) {
        this.interner = interner;
    }

    /**
     * @return
     */
    public ITermFactory getFactory() {
        return termFactory;
    }

    /**
     * @return
     */
    public IOperatorTable getOptable() {
        return operatorTable;
    }

    /**
     * @param optable
     */
    public void setOptable(IOperatorTable optable) {
        operatorTable = optable;
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
     * @return
     */
    public PlLexer getTokenSource() {
        if (!tokenSourceStack.isEmpty()) {
            return tokenSourceStack.peek();
        }
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
    //      exprC(n-1) { op(priorityXFX,n) exprA(n-1) | op(priorityXFY,n) exprA(n) | op(xf,n) }*
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
    // args ::= '(' sequence ')'
    //----------------------------
    // list ::= '[' sequence ']'
//===========================
//     block ::= '('  { exprA(1200) }* ')'  //block
//============================
//     bypass_blk ::= '{' { exprA(1200) }* '}'
//============================
//     op(type,n) ::= atom | { symbol }+
//============================
//     sequence ::= [ heads "|" tail ]
//============================
//     heads ::= [ exprA(1200) { ',' exprA(1200) }* ]
//============================
//     tail ::=  variable | list

    /**
     * //  expr_A0 ::=
     * //      integer |
     * //      float |
     * //      atom |
     * //      variable |
     * //      list     |
     * //      functor
     * //============================
     * // functor ::=
     * functorName args
     * //============================
     * // functorName ::=
     * expr_A0
     *
     * @param quote
     * @return
     */
    protected boolean options(TokenKind quote) {
        return false;
    }

    /**
     * @param rdelim
     * @return
     * @throws EOFException
     */
    @Override
    public ITerm expr(TokenKind rdelim) throws Exception {
        return exprA(MAX_PRIORITY, rdelim);
    }

//    /**
//     * @return
//     */
//    public HtClause parseClause() throws Exception {
//        return convert(termSentence());
//    }

    /**
     * @return
     */
    public boolean isFunctorBegin() throws IOException {
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
            return expr(TK_DOT);
        }

//        popTokenSource();
        return END_OF_FILE;
    }

    public ITerm getTerm() throws Exception {
        EnumSet<Associativity> assocs = of(x);
        EnumSet<DirectiveKind> directiveKinds = of(DK_IF, DK_ENCODING, DK_HILOG);
        EnumSet<TokenKind> rDelims = of(TK_DOT);
        PlToken token = readToken();
        switch (token.kind) {
            case TK_BOF:
                lastTerm = BEGIN_OF_FILE;
                break;
            case TK_EOF:
                lastTerm = END_OF_FILE;
//                popTokenSource();
                getLexer().atEOF = true;
                break;
            case TK_DOT:
                if (!isEndOfTerm()) {
                    throw new IllegalStateException("Brackets or/and quotes are unbalanced ...");
                }
                break;
            case TK_LPAREN:
                if (isFunctorBegin()) {
                    directiveKinds = of(DK_IF);
                    rDelims.add(TK_RPAREN);
                    final ListTerm l = (ListTerm) expr0Args();
                    if (lastTerm.isHiLog()) {
                        termFactory.newHiLogFunctor(lastTerm, l);//fixme
                    } else if (lastTerm.isAtom()) {
                        int name = ((IFunctor) lastTerm).getName();
                        int arity = ((IFunctor) lastTerm).getArity();
                        lastTerm = new HtFunctor(name, l);
                    } else {
                        throw new ParserException("Possibly undeclared HiLog term ->%s<- ", token);
                    }
                } else {
                    if (rDelims.contains(token.kind)) {
                        break;
                    }
                    lastTerm = expr0Block();
                    lastTerm.setBracketed(true);
                }
                break;
            case TK_LBRACKET:
                rDelims.add(TK_RBRACKET);
                lastTerm = expr0List();
                break;
            case TK_LBRACE:
                rDelims.add(TK_RBRACE);
                lastTerm = expr0BraceBlock();
                break;
            case TK_RBRACE:
                if (--braces < 0) {
                    throw new ParserException("Extra right brace.");
                }
                break;
            case TK_RBRACKET:
                if (--brackets < 0) {
                    throw new ParserException("Extra right bracket.");
                }
                break;
            case TK_RPAREN:
                if (--parentheses < 0) {
                    throw new ParserException("Extra right parenthesis.");
                }
                break;
            case TK_D_QUOTE:
                if (++dquotes % 2 != 0) {
                    if (options(TK_D_QUOTE)) {
                        lastTerm = null;//readString(TK_D_QUOTE);
                    }
                } else {
                    return lastTerm;
                }
                rDelims.clear();
                rDelims.add(token.kind);
                break;
            case TK_S_QUOTE:
                if (++squotes % 2 != 0) {
                    if (options(TK_S_QUOTE)) {
                        lastTerm = null;//readString(TK_S_QUOTE);
                    }
                } else {
                    return lastTerm;
                }
                rDelims.clear();
                rDelims.add(token.kind);
                break;
            case TK_B_QUOTE:
                if (++bquotes % 2 != 0) {
                    if (options(TK_B_QUOTE)) {
                        lastTerm = null;//readString(TK_B_QUOTE);
                    }
                } else {
                    return lastTerm;
                }
                rDelims.clear();
                rDelims.add(token.kind);
                break;
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
            default:
                throw new IllegalStateException("Unused or unknown value: " + token.kind);
        }

        return lastTerm;
    }

    /**
     * @param token
     * @return
     */
    public boolean isOperator(PlToken token) {
        final String name = token.getImage();
        final Set<OpSymbolFunctor> ops = appContext.getOpTable().getOperators(name);

        return ops.stream().anyMatch(op -> op.getTextName().equals(name));
    }

//    protected Set<OpSymbolFunctor> tryOperators(String image, TokenKind rDelim) throws Exception {
//        Set<OpSymbolFunctor> ops = (token.kind == TK_ATOM) ?
//                tryOperators(token.image) :
//                Collections.emptySet();
//        for (OpSymbolFunctor op : ops) {
//            if (op.getPriority() == currPriority) {
//                switch (op.getAssociativity()) {
//                    case fx:
//                        lastTerm = exprA(currPriority - 1);
//                        break;
//                    case fy:
//                        lastTerm = exprA(currPriority);
//                        break;
//                    case hx:
//                        lastTerm = exprA(currPriority - 1);
//                        break;
//                    case hy:
//                        lastTerm = exprA(currPriority);
//                        break;
//                    default:
//                }
//            }
//            lastTerm = op;
//        }
//
//        return ops;
//    }

    /**
     * Parses and returns a valid 'leftside' of an expression.
     * If the left side starts with a prefix, it consumes other expressions with a lower priority than itself.
     * If the left side does not have a prefix it must be an expr0.
     *
     * @param currPriority operators with a higher priority than this will effectively end the expression
     * @param rDelim
     * @return a wrapper of: 1. term correctly structured and 2. the priority of its root operator
     */
    public ITerm parseLeftSide(int currPriority, TokenKind rDelim) throws Exception {
//        1. prefix expression
//        lastTerm = null;
        PlToken token = readToken();
//        token.kind = TK_ATOM;
//        token.image = END_OF_FILE_STRING;//(getTokenSource().isEOFGenerated = true))) {
//        lastTerm = END_OF_FILE;
//        return lastTerm;
//    }
//        if (token.kind == TK_BOF) {
//            token.kind = TK_ATOM;
        if (token == BOF) {
            getTokenSource().isBOFGenerated = true;
            lastTerm = BEGIN_OF_FILE;
            return lastTerm;
        }
        if (token == EOF) {
            getTokenSource().isEOFGenerated = true;
            lastTerm = END_OF_FILE;
            return lastTerm;
        }
        if (currPriority == 0) {
            return exprA0(rDelim);
        }
        if (isOperator(token)) {
            int priorityFX = getOptable().getPriority(token.image, fx);
            int priorityFY = getOptable().getPriority(token.image, fy);
            int priorityHX = getOptable().getPriority(token.image, hx);
            int priorityHY = getOptable().getPriority(token.image, hy);
            if (priorityFY == 0) {
                priorityFY = -1;
            }
            if (priorityHY == 0) {
                priorityHY = -1;
            }
            token = readToken();
            String prefix = token.image;
            if ("+".equals(prefix) || "-".equals(prefix)) {
                token = readToken();
                switch (token.kind) {
                    case TK_INTEGER_LITERAL:
                        lastTerm = termFactory.newIntTerm(prefix, token.image);
                        return lastTerm;
                    case TK_FLOATING_POINT_LITERAL:
                        lastTerm = termFactory.newFloatTerm(prefix, token.image);
                        return lastTerm;
                    default:
                        getLexer().unreadToken(token);
                }
                //check that no operator has a priority higher than permitted
                if (priorityFY > currPriority) {
                    priorityFY = -1;
                }
                if (priorityFX > currPriority) {
                    priorityFX = -1;
                }
                //priorityFX has priority over priorityFY
                boolean haveAttemptedFX = false;
                if (priorityFX >= priorityFY && priorityFX >= MIN_PRIORITY) {
                    if (lastTerm != null) {
                        return new OpSymbolFunctor(
                                token.image,
                                fx,
                                priorityFX - 1,
                                ((OpSymbolFunctor) lastTerm).getResult());
                    } else {
                        haveAttemptedFX = true;
                    }
                }
                boolean haveAttemptedHX = false;
                if (priorityHX >= priorityHY && priorityHX >= MIN_PRIORITY) {
                    if (lastTerm != null) {
                        return new OpSymbolFunctor(
                                token.image,
                                hx,
                                priorityHX - 1,
                                ((OpSymbolFunctor) lastTerm).getResult());
                    } else {
                        haveAttemptedHX = true;
                    }
                }
                //priorityFY has priority over priorityFX, or priorityFX has failed
                if (priorityFY >= MIN_PRIORITY) {
                    if (lastTerm != null) {
                        return new OpSymbolFunctor(
                                token.image,
                                fy,
                                priorityFY,
                                ((OpSymbolFunctor) lastTerm).getResult());
                    }
                }
                if (priorityHY >= MIN_PRIORITY) {//todo hilog
                    if (lastTerm != null) {
                        return new OpSymbolFunctor(
                                token.image,
                                hy,
                                priorityHY,
                                ((OpSymbolFunctor) lastTerm).getResult());
                    }
                }
                //priorityFY has priority over priorityFX, but priorityFY failed
                if (!haveAttemptedFX && priorityFX >= MIN_PRIORITY) {
                    haveAttemptedFX = true;
                }
                //priorityFY has priority over priorityFX, but priorityFY failed
                if (!haveAttemptedHX && priorityHX >= MIN_PRIORITY) {
                    if (lastTerm != null) {
                        return new OpSymbolFunctor(
                                token.image,
                                hx,
                                priorityHX - 1,
                                ((OpSymbolFunctor) lastTerm).getResult());
                    } else {
                        haveAttemptedHX = true;
                    }
                }
            }
        }

        return lastTerm;
    }

    /**
     * @param currPriority
     * @return
     * @throws Exception
     */
    protected OpSymbolFunctor exprA(int currPriority, TokenKind rDelim) throws Exception {
        OpSymbolFunctor leftSide = exprB(currPriority, rDelim);
        //{op(yfx,n) exprA(n-1) | op(yf,n)}*
        PlToken t = readToken();
        for (; isOperator(t); t = readToken()) {
            int priorityYFX = getOptable().getPriority(t.image, yfx);
            int priorityYF = getOptable().getPriority(t.image, yf);

            //YF and YFX has a higher priority than the left side expr and less then top limit
            // if (YF < leftSide.priority && YF > OperatorManager.OP_HIGH) YF = -1;
            if (priorityYF < leftSide.getPriority() || priorityYF > currPriority) {
                priorityYF = -1;
            }
            // if (YFX < leftSide.priority && YFX > OperatorManager.OP_HIGH) YFX = -1;
            if (priorityYFX < leftSide.getPriority() || priorityYFX > currPriority) {
                priorityYFX = -1;
            }
            //YFX has priority over YF
            if (priorityYFX >= priorityYF && priorityYFX >= MIN_PRIORITY) {
                OpSymbolFunctor ta = exprA(priorityYFX - 1, rDelim);
                if (ta != null) {
                    leftSide = identifyTerm(priorityYFX,
                            new OpSymbolFunctor(
                                    t.image,
                                    leftSide.getResult(),
                                    ta.getResult()),
                            tokenStart);
                    continue;
                }
            }
            //either YF has priority over YFX or YFX failed
            if (priorityYF >= MIN_PRIORITY) {
                leftSide = identifyTerm(
                        priorityYF,
                        new OpSymbolFunctor(
                                t.image,
                                leftSide.getResult()),
                        tokenStart);
                continue;
            }
            break;
        }
        getLexer().unreadToken(t);

        return leftSide;
    }

    /**
     * @param currPriority
     * @param rDelim
     * @return
     * @throws Exception
     */
    protected OpSymbolFunctor exprB(int currPriority, TokenKind rDelim) throws Exception {
        //1. op(fx,n) exprA(n-1) | op(fy,n) exprA(n) | expr0
        OpSymbolFunctor left = (OpSymbolFunctor) parseLeftSide(currPriority, rDelim);
        //2.left is followed by either priorityXFX, priorityXFY or xf operators, parse these
        PlToken operator = readToken();
        for (; isOperator(operator); operator = readToken()) {
            int priorityXFX = getOptable().getPriority(operator.image, xfx);
            int priorityXFY = getOptable().getPriority(operator.image, fx);
            int priorityXF = getOptable().getPriority(operator.image, xf);
            //check that no operator has a priority higher than permitted
            //or a lower priority than the left side expression
            if (priorityXFX > currPriority || priorityXFX < MIN_PRIORITY) {
                priorityXFX = -1;
            }
            if (priorityXFY > currPriority || priorityXFY < MIN_PRIORITY) {
                priorityXFY = -1;
            }
            if (priorityXF > currPriority || priorityXF < MIN_PRIORITY) {
                priorityXF = -1;
            }
            //priorityXFX
            boolean haveAttemptedXFX = false;
            if (priorityXFX >= priorityXFY && priorityXFX >= priorityXF && priorityXFX >= left.getPriority()) {
                //priorityXFX has priority
                OpSymbolFunctor found = exprA(priorityXFX - 1, rDelim);
                if (found != null) {
                    //OpSymbolFunctor priorityXFX = new OpSymbolFunctor(operator.seq, left.getResult(), found.getResult());
                    //left = new OpSymbolFunctor(priorityXFX, priorityXFX);
                    left = identifyTerm(priorityXFX,
                            new OpSymbolFunctor(
                                    operator.image,
                                    left.getResult(),
                                    found.getResult()),
                            tokenStart);
                    continue;
                } else {
                    haveAttemptedXFX = true;
                }
            }
            //priorityXFY has priority, or priorityXFX has failed
            if (priorityXFY >= priorityXF && priorityXFY >= left.getPriority()) {
                OpSymbolFunctor found = exprA(priorityXFY, rDelim);
                if (found != null) {
                    //OpSymbolFunctor priorityXFY = new OpSymbolFunctor(token.image, left.getResult(), found.getResult());
                    //left = new OpSymbolFunctor(priorityXFY, priorityXFY);
                    left = identifyTerm(
                            priorityXFY,
                            new OpSymbolFunctor(
                                    operator.image,
                                    left.getResult(),
                                    found.getResult()),
                            tokenStart);
                    continue;
                }
            }
            //XF has priority, or priorityXFX and/or priorityXFY has failed
            if (priorityXF >= left.getPriority())
                //return new OpSymbolFunctor(XF, new OpSymbolFunctor(token.image, left.getResult()));
                return identifyTerm(priorityXF,
                        new OpSymbolFunctor(
                                operator.image,
                                left.getResult()),
                        tokenStart);

            //priorityXFX did not have top priority, but priorityXFY failed
            if (!haveAttemptedXFX && priorityXFX >= left.getPriority()) {
                OpSymbolFunctor found = exprA(priorityXFX - 1, rDelim);
                if (found != null) {
                    left = identifyTerm(priorityXFX,
                            new OpSymbolFunctor(
                                    operator.image,
                                    left.getResult(),
                                    found.getResult()),
                            tokenStart);
                    continue;
                }
            }
            break;
        }
        getLexer().unreadToken(operator);

        return left;
    }

    /**
     * @param priority
     * @param term
     * @param offset
     * @return
     */
    protected OpSymbolFunctor identifyTerm(int priority, ITerm term, int offset) {
        map(term, offset);
        return new OpSymbolFunctor("", priority, term, null);
    }

    protected void map(ITerm term, int offset) {
        if (offsetsMap != null)
            offsetsMap.put(term, offset);
    }

    public Map<ITerm, Integer> getTextMapping() {
        return offsetsMap;
    }

    /**
     * @return
     * @throws Exception
     */
    protected PlToken readToken() throws Exception {
        final PlToken tok = getLexer().readToken(true);
        logger.info("token = " + tok);

        return tok;
    }

    /**
     * @param sb
     */
    public void toString0(StringBuilder sb) {

    }

    /**
     * @return
     */
    public PlLexer getLexer() throws IOException {
        return getTokenSourceStack().peek();
    }

    /**
     * Static service to get a term from its string representation,
     * providing a specific operator manager
     */
    public ITerm parseSingleTerm(String st) throws Exception {
        setTokenSource(getPlLexerForString(st));
//        PlToken t = readToken();
//        if (t.kind == TK_BOF) {
//            return BEGIN_OF_FILE;
//        }
//        if (t.kind == TK_EOF) {
//            popTokenSource();
//            return END_OF_FILE;
//        }

//        getLexer().unreadToken(t);
        return termSentence();
    }

    /**
     * exprA(0) ::= integer |
     * float |
     * variable |
     * atom |
     * atom( exprA(1200) { , exprA(1200) }* ) |
     * '[' exprA(1200) { , exprA(1200) }* [ | exprA(1200) ] ']' |
     * '{' [ exprA(1200) ] '}' |
     * '(' exprA(1200) ')'
     *
     * @param rDelim
     */
    public ITerm exprA0(TokenKind rDelim) throws Exception {
        lastTerm = getTerm();
        return lastTerm;
    }

    /**
     * @return
     * @throws Exception
     */
    public ITerm expr0List() throws Exception {
        return listSequence(TK_LBRACKET);
    }

    private ITerm listSequence(TokenKind lDelim) throws Exception {
        ListTerm l = sequence(lDelim);
        PlToken token = readToken();
        final TokenKind rDelim = calcRDelim(lDelim);
        if (token.kind == TK_CONS) {
            l.addTail(tail(lDelim));
        } else if (token.kind == rDelim) {
            l.newTail(false);//NIL
        }
        return lastTerm = l;
    }

    /**
     * @return
     * @throws Exception
     */
    public ITerm expr0Args() throws Exception {
        return listSequence(TK_LPAREN);
    }

    /**
     * @return
     * @throws Exception
     */
    public ITerm expr0Block() throws Exception {
        final ListTerm t = sequence(TK_LPAREN);
        t.setBracketed(true);
        return t;
    }

    /**
     * @return
     * @throws Exception
     */
    public ITerm expr0BraceBlock() throws Exception {
        return sequence(TK_LBRACE);
    }

    protected ListTerm sequence(TokenKind kind) throws Exception {
        ListTerm l = new ListTerm(0);
        TokenKind rdelim = calcRDelim(kind);
        ITerm head = null;
        for (; ; ) {
            PlToken t = readToken();
            switch (t.kind) {
                case TK_LPAREN:
                case TK_LBRACKET:
                case TK_LBRACE:
                    if (t.kind == kind) {
                        head = expr(rdelim);
                    }
                    break;
                case TK_RPAREN:
                case TK_RBRACKET:
                case TK_RBRACE:
                    l.addHead(head);
                    getLexer().unreadToken(t);
                    break;
                case TK_COMMA:
                    l.addHead(head);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + t.kind);
            }
        }
    }

    private TokenKind calcRDelim(TokenKind kind) {
        switch (kind) {
            case TK_LPAREN:
                return TK_RPAREN;
            case TK_LBRACKET:
                return TK_RBRACKET;
            case TK_LBRACE:
                return TK_RBRACE;
        }

        return kind;
    }

    private ITerm tail(TokenKind rDelim) throws Exception {
        final ITerm t = expr(rDelim);
        if (t.isList() || t.isVar()) {
            return t;
        }
        throw new ParserException("list or var are expected here.");
    }
}
