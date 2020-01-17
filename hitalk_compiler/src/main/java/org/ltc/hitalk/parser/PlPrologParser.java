package org.ltc.hitalk.parser;

import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.parser.PlToken.TokenKind;
import org.ltc.hitalk.term.*;
import org.ltc.hitalk.term.HlOpSymbol.Associativity;
import org.ltc.hitalk.term.HlOpSymbol.Fixity;
import org.ltc.hitalk.term.io.HiTalkInputStream;
import org.ltc.hitalk.wam.compiler.HtFunctor;
import org.ltc.hitalk.wam.compiler.HtFunctorName;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.ltc.hitalk.wam.compiler.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

import static java.lang.Integer.MAX_VALUE;
import static java.util.EnumSet.of;
import static java.util.Objects.requireNonNull;
import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.REPRESENTATION_ERROR;
import static org.ltc.hitalk.core.BaseApp.getAppContext;
import static org.ltc.hitalk.core.utils.TermUtilities.convertToClause;
import static org.ltc.hitalk.parser.PlDynamicOperatorParser.OP_HIGH;
import static org.ltc.hitalk.parser.PlDynamicOperatorParser.OP_LOW;
import static org.ltc.hitalk.parser.PlToken.TokenKind.*;
import static org.ltc.hitalk.parser.PrologAtoms.NIL;
import static org.ltc.hitalk.parser.PrologAtoms.TRUE;
import static org.ltc.hitalk.parser.PrologAtoms.*;
import static org.ltc.hitalk.term.HlOpSymbol.Associativity.*;
import static org.ltc.hitalk.term.ListTerm.Kind.*;
import static org.ltc.hitalk.wam.compiler.Language.PROLOG;

/**
 * @author shun
 */
public class PlPrologParser implements IParser {
    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    private boolean stringQDelim;

    public boolean isStringQDelim() {
        return stringQDelim;
    }

    private static class IdentifiedTerm extends HlOpSymbol implements ITerm {

        private ITerm result;

        /**
         * @param priority
         * @param result
         */
        public IdentifiedTerm(int priority, ITerm result) {
            super(0, "", xfx, priority);

            this.result = result;
        }

        /**
         * @param name
         * @param textName
         * @param associativity
         * @param priority
         */
        public IdentifiedTerm(int name, String textName, Associativity associativity, int priority) {
            super(name, textName, associativity, priority, true, null);
        }

        public ITerm getResult() {
            return result;
        }
    }

    public static final String BEGIN_OF_FILE = "begin_of_file";
    public static final String END_OF_FILE = "end_of_file";
    public static final Atom END_OF_FILE_ATOM = getAppContext().getTermFactory().newAtom(END_OF_FILE);
    public static final Atom BEGIN_OF_FILE_ATOM = getAppContext().getTermFactory().newAtom(BEGIN_OF_FILE);
    public static final String ANONYMOUS = "_";

    protected final Deque<ITokenSource> tokenSourceStack = new ArrayDeque<>();

    protected IOperatorTable operatorTable;
    protected IVafInterner interner;
    protected ITermFactory termFactory;
    /**
     * Holds the variable scoping context for the current sentence.
     */
    protected Map<Integer, HtVariable> variableContext = new HashMap<>();
    protected HlOpSymbol operator;

    /**
     * /**
     *
     * @param inputStream
     * @param factory
     * @param optable
     */
    public PlPrologParser(HiTalkInputStream inputStream,
                          IVafInterner interner,
                          ITermFactory factory,
                          IOperatorTable optable) throws Exception {
        setTokenSource(new PlLexer(inputStream));
        this.interner = interner;
        this.termFactory = factory;
        this.operatorTable = optable;
    }

    /**
     *
     */
    public PlPrologParser() throws Exception {
        this(getAppContext().getInputStream(),
                getAppContext().getInterner(),
                getAppContext().getTermFactory(),
                getAppContext().getOpTable());
    }

    public void toString0(StringBuilder sb) {
    }

    @Override
    public PlPrologParser getParser() {
        return this;
    }

    @Override
    public IVafInterner getInterner() {
        return interner;
    }

    /**
     * @param interner
     */
    @Override
    public void setInterner(IVafInterner interner) {
        this.interner = interner;
    }

    /**
     * @return
     */
    @Override
    public ITermFactory getFactory() {
        return termFactory;
    }

    /**
     * @return
     */
    @Override
    public IOperatorTable getOptable() {
        return operatorTable == null ? new PlDynamicOperatorParser() : operatorTable;
    }

    /**
     * @param optable
     */
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
     */
    public ITerm parse() throws Exception {
        return termSentence();
    }

    /**
     * @return
     */
    @Override
    public ITerm next() throws Exception {
        PlToken token = (getLexer()).next(true);
        return newTerm(token, of(TK_DOT));
    }

    /**
     * @return
     */
    public HtClause parseClause() throws Exception {
        return convertToClause(termSentence(), getAppContext().getInterner());
    }

    /**
     * @return
     */
    @Override
    public ITokenSource getTokenSource() {
        return tokenSourceStack.peek();
    }

    /**
     * @param source
     */
    public void setTokenSource(ITokenSource source) {
        logger.info("Adding ts " + source.getPath() + source.isOpen());
        if (!tokenSourceStack.contains(source) && source.isOpen()) {
            tokenSourceStack.push(source);
        }
        logger.info("declined  Adding dup ts " + source.getPath());
    }

    /**
     * @return
     */
    @Override
    public ITokenSource popTokenSource() throws IOException {
        if (!tokenSourceStack.isEmpty()) {
            final ITokenSource ts = tokenSourceStack.pop();
            ts.close();
            return ts;
        }

        throw new ExecutionError(REPRESENTATION_ERROR, null);
    }

    /**
     * @param token
     * @param terminators
     * @return
     * @throws IOException
     * @throws ParseException
     */
    protected ITerm newTerm(PlToken token, EnumSet<TokenKind> terminators)
            throws Exception {
        logger.info(token.toString());
        ITerm result = null;
        boolean finished = false;
        if (token.kind == TK_BOF) {
            result = BEGIN_OF_FILE_ATOM;
        } else if (token.kind == TK_EOF) {
            result = END_OF_FILE_ATOM;
        } else {
            HlOperatorJoiner<ITerm> joiner = new HlOperatorJoiner<ITerm>() {
                @Override
                protected ITerm join(int notation, List<ITerm> args) {
                    return new HtFunctor(notation, new ListTerm(args.size()));
                }
            };
            outer:
            for (int i = 0; ; ++i, token = getLexer().next(joiner.accept(x))) {
                if (token.kind == TK_EOF) {
                    throw new ParseException("Premature EOF");
                }
                if (token.kind.isAtom()) {
                    if (false && i == 0 || joiner.accept(xf)) {
                        for (TokenKind terminator : terminators) {
                            if (token.kind == terminator) {
                                result = i > 0 ? joiner.complete() : null;
                                finished = true;
                                break;
                            }
                        }
                        if (finished) {
                            break;
                        }
                    }
                    logger.info(token.image);
                    final Map<Fixity, HlOpSymbol> syms = getOptable().getOperatorsMatchingNameByFixity(token.image);
                    for (HlOpSymbol right : syms.values()) {
                        if (joiner.accept(right.getAssociativity())) {
                            joiner.push(right);
                            continue outer;
                        }
                    }
                }
                if (!joiner.accept(x)) {
                    throw new ParseException("Impossible to resolve operator! token = " + token);
                }
                joiner.push(literal(token));
            }
        }

        return result;
    }

    public PlLexer getLexer() {
        return (PlLexer) getTokenSource();
    }

    /**
     * @param token
     * @return
     * @throws IOException
     * @throws ParseException
     */
    protected ITerm literal(PlToken token) throws Exception {
        ITerm term = null;
        switch (token.kind) {
            case TK_VAR:
                term = termFactory.newVariable(token.image);
                break;
            case TK_FUNCTOR_BEGIN:
                term = compound(token.image);
                break;
            case TK_INTEGER_LITERAL:
                term = termFactory.newAtomic(Integer.parseInt(token.image));
                break;
            case TK_FLOATING_POINT_LITERAL:
                term = termFactory.newAtomic(Double.parseDouble(token.image));
                break;
            case TK_BOF:
                term = BEGIN_OF_FILE_ATOM;
                getTokenSource().setEncodingPermitted(true);
                break;
            case TK_EOF:
                term = END_OF_FILE_ATOM;
                popTokenSource();
                break;
            case TK_LPAREN:
            case TK_LBRACKET:
            case TK_LBRACE:
//                term = readSequence(token.kind, token.image);
                break;
            case TK_QUOTED_NAME:
            case TK_SYMBOLIC_NAME:
            case TK_ATOM:
                term = termFactory.newAtom(token.image);
                break;
            default:
                throw new ParserException("Unexpected token: " + token);
        }

        return term;
    }

    /**
     * @param kind
     * @return
     */
    private TokenKind calcRDelim(TokenKind kind) {
        switch (kind) {
            case TK_LPAREN:
                return TK_RPAREN;
            case TK_LBRACKET:
                return TK_RBRACKET;
            case TK_LBRACE:
                return TK_RBRACE;

            default:
                throw new IllegalStateException("Unexpected value: " + kind);
        }
    }

//    /**
//     * @param ldelim
//     * @param image
//     * @return
//     * @throws Exception
//     */
//    protected ListTerm readSequence(TokenKind ldelim, ListTerm.Kind kind) throws Exception {
//        List<ITerm> elements = new ArrayList<>();
//        TokenKind rdelim = calcRDelim(ldelim);
//
//        switch (kind) {
//            case NIL:
//                break;
//            case LIST:
//                break;
//            case BYPASS:
//                break;
////            case AND:
////                break;
////            case OR:
////                break;
////            case NOT:
////                break;
////            case IF_THEN:
////                break;
////            case GOAL:
////                break;
////            case HILOG_APPLY:
////                break;
////            case INLINE_GOAL:
////                break;
////            case OTHER:
////                break;
////            case CLAUSE_BODY:
////                break;
//        }
//        EnumSet<TokenKind> rdelims = ldelim == TK_CONS ? of(TK_COMMA, rdelim) : of(TK_COMMA, TK_CONS, rdelim);
//        ListTerm.Kind kind1;
//        switch (ldelim) {
//            case TK_LPAREN:
//                kind = CLAUSE_BODY;
////                if (isBlocked) {
////                    kind = CLAUSE_BODY;
////                } else {
////                    kind = LIST;//compound args
////                }
//                break;
//            case TK_LBRACKET:
//                kind = LIST;
//                break;
//            case TK_LBRACE:
//                kind = BYPASS;
//                break;
//            default:
//                throw new IllegalArgumentException();
//        }
//        ITerm term = newTerm(getLexer().getNextToken(), true, rdelims);
//        if (term == null) {
//            if (rdelim != getLexer().peek().kind) {
//                throw new ParseException("No (more) elements");
//            }
//            elements.add(term);
////            remove this
//            while (TK_COMMA == getLexer().peek().kind) {//todo flatten
//                elements.add(newTerm(getLexer().getNextToken(), false, rdelims));
//            }
//            if (TK_CONS == getLexer().peek().kind) {
//                elements.add(newTerm(getLexer().getNextToken(), true, rdelims));
//            }
//
//            return termFactory.newListTerm(kind, flatten(elements));
//        }
//
//        return termFactory.newListTerm(kind, flatten(elements));
//    }

    private ITerm[] flatten(List<ITerm> elements) {
        return elements.toArray(new ITerm[elements.size()]);
    }

    /**
     * @param name
     * @return
     * @throws IOException
     * @throws ParseException
     */
    protected IFunctor compound(String name) throws Exception {
        ListTerm args = null;//readSequence(/*name.equals(IMPLIES),*/ TK_RPAREN, false);
        return compound(name, args);
    }

    protected IFunctor compound(String name, ListTerm args) throws Exception {
        final int iname = interner.internFunctorName(name, 0);
        return termFactory.newFunctor(iname, args);
//        return termFactory.newFunctor(hilogApply, name, args);
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
            return literal((getLexer()).getNextToken());
        }

        return literal(PlToken.newToken(TK_EOF));
    }

    //    @Override
    public void setOperator(String operatorName, int priority, Associativity associativity) {
        Map<Fixity, HlOpSymbol> ops = operatorTable.getOperatorsMatchingNameByFixity(operatorName);
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
        IntStream.range(0, operatorNames.length).forEachOrdered(i ->
                op(operatorNames[i], priority, associativity));
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
        interner.internFunctorName(ARGLIST_NIL, 0);

        interner.internFunctorName(NIL, 0);
        interner.internFunctorName(new HtFunctorName(CONS, 1, 2));

        interner.internFunctorName(TRUE, 0);
        interner.internFunctorName(FAIL, 0);
        interner.internFunctorName(FALSE, 0);
        interner.internFunctorName(CUT, 0);

        interner.internFunctorName(BYPASS_NIL, 0);
    }

    public Directive peekAndConsumeDirective() {

        return null;//fixme
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder("PlPrologParser{");
//        sb.append("tokenSourceStack=").append(tokenSourceStack);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Describes the possible system directives in interactive mode.
     */
    public enum Directive {
        Trace, Info, User, File
    }

// * This class defines a parser of prolog terms and sentences.
// * <p/>
// * BNF part 2: Parser
// * term ::= exprA(1200)
// * exprA(n) ::= exprB(n) { op(yfx,n) exprA(n-1) |
// *                         op(yf,n) }*
// * exprB(n) ::= exprC(n-1) { op(xfx,n) exprA(n-1) |
// *                           op(xfy,n) exprA(n) |
// *                           op(xf,n) }*
// * // exprC is called parseLeftSide in the code
// * exprC(n) ::= '-' integer | '-' float |
// *              op( fx,n ) exprA(n-1) |
// *              op( fy,n ) exprA(n) |
// *              exprA(n)
// * exprA(0) ::= integer |
// *              float |
// *              atom |
// *              variable |
// *              atom'(' exprA(1200) { ',' exprA(1200) }* ')' |
// *              '[' [ exprA(1200) { ',' exprA(1200) }* [ '|' exprA(1200) ] ] ']' |
// *              '(' { exprA(1200) }* ')'
// *              '{' { exprA(1200) }* '}'
// * op(type,n) ::= atom | { symbol }+
// */
//    public Parser(OperatorManager op, String theoryText) {

    /**
     * Parses next term from the stream built on string.
     *
     * @param endNeeded <tt>true</tt> if it is required to parse the end token
     *                  (a period), <tt>false</tt> otherwise.
     */
    public ITerm nextTerm(boolean endNeeded) throws Exception {
        try {
            PlToken t = getLexer().readToken();
            if (t.isBOF()) {
                return BEGIN_OF_FILE_ATOM;
            }
            if (t.isEOF()) {
                return END_OF_FILE_ATOM;
            }
            getLexer().unreadToken(t);
            ITerm term = expr(false);
            if (term == null) {
                throw new IllegalStateException();
            }
            if (endNeeded && getLexer().readToken().kind == TK_DOT)
//                throw new InvalidTermException("The term " + term + " is not ended with a period.");
//
//                term.resolveTerm();
                return term;
//        } catch (IOException ex) {
//            throw new InvalidTermException("An I/O error occurred.");
        } finally {

        }
        return null;
    }
//

    /**
     * Static service to get a term from its string representation
     */
    public ITerm parseSingleTerm(String st) throws Exception, InvalidTermException {
        ITokenSource ts = ITokenSource.getITokenSourceForString(st);
        return parseSingleTerm(ts);
    }

    /**
     * Static service to get a term from its string representation,
     * providing a specific operator manager
     */
    public ITerm parseSingleTerm(ITokenSource ts) throws Exception {
        try {
            setTokenSource(ts);
            PlToken t = getLexer().readToken();
            if (t.isBOF()) {
                return PlPrologParser.BEGIN_OF_FILE_ATOM;
            }
            if (t.isEOF()) {
                return PlPrologParser.END_OF_FILE_ATOM;
            }

            getLexer().unreadToken(t);
            ITerm term = expr(false);
            if (term == null) {
                throw new InvalidTermException("Term is null");
            }
            if (!getLexer().readToken().isEOF()) {
                throw new InvalidTermException("The entire string could not be read as one term");
            }
//            term.resolveTerm();
            return term;
        } catch (IOException | InvalidTermException ex) {
            throw new IOException("An I/O error occurred");
        }
    }

    private IdentifiedTerm exprA(int maxPriority, boolean commaIsEndMarker) throws Exception {
        IdentifiedTerm leftSide = exprB(maxPriority, commaIsEndMarker);
        if (leftSide == null) {
            return null;
        }

        //{op(yfx,n) exprA(n-1) | op(yf,n)}*
        PlToken t = getLexer().readToken();
        for (; t.isOperator(commaIsEndMarker); t = getLexer().readToken()) {
            int YFX = this.getOptable().getPriority(t.image, yfx);
            int YF = this.getOptable().getPriority(t.image, yf);

            //YF and YFX has a higher priority than the left side expr and less then top limit
            // if (YF < leftSide.getPriority() && YF > PlDynamicOperatorParser.OP_HIGH) YF = -1;
            if (YF < leftSide.getPriority() || YF > maxPriority) YF = -1;
            // if (YFX < leftSide.getPriority() && YFX > PlDynamicOperatorParser.OP_HIGH) YFX = -1;
            if (YFX < leftSide.getPriority() || YFX > maxPriority) YFX = -1;

            //YFX has getPriority() over YF
            if (YFX >= YF && YFX >= OP_LOW) {
                IdentifiedTerm ta = exprA(YFX - 1, commaIsEndMarker);
                if (ta != null) {
                    leftSide = (IdentifiedTerm) new HlOpSymbol(YFX, new HlOpSymbol(t.image, leftSide.getResult(), ta.getResult()));
                    continue;
                }
            }
            //either YF has priority over YFX or YFX failed
            if (YF >= OP_LOW) {
                leftSide = (IdentifiedTerm) new HlOpSymbol(YF, new HlOpSymbol(t.image, leftSide.getResult()));
                continue;
            }
            break;
        }
        getLexer().unreadToken(t);

        return leftSide;
    }

    private IdentifiedTerm exprB(int maxPriority, boolean commaIsEndMarker) throws Exception {
        //1. op(fx,n) exprA(n-1) | op(fy,n) exprA(n) | expr0
        IdentifiedTerm left = (IdentifiedTerm) parseLeftSide(commaIsEndMarker, maxPriority);
        //2.left is followed by either xfx, xfy or xf operators, parse these
        PlToken token = getLexer().readToken();
        for (; token.isOperator(commaIsEndMarker); token = getLexer().readToken()) {
            int XFX = getOptable().getPriority(token.image, xfx);
            int XFY = getOptable().getPriority(token.image, xfy);
            int XF = getOptable().getPriority(token.image, xf);

            //check that no operator has a priority higher than permitted
            //or a lower priority than the left side expression
            if (XFX > maxPriority || XFX < OP_LOW) {
                XFX = -1;
            }
            if (XFY > maxPriority || XFY < OP_LOW) {
                XFY = -1;
            }
            if (XF > maxPriority || XF < OP_LOW) {
                XF = -1;
            }

            //XFX
            boolean haveAttemptedXFX = false;
            if (XFX >= XFY && XFX >= XF && XFX >= requireNonNull(left).getPriority()) {     //XFX has priority
                IdentifiedTerm found = exprA(XFX - 1, commaIsEndMarker);
                if (found != null) {
                    ITerm xfx = new HlOpSymbol(token.image, left.getResult(), found.getResult());
                    left = new IdentifiedTerm(XFX, xfx);
                    continue;
                } else {
                    haveAttemptedXFX = true;
                }
            }
            //XFY
            if ((XFY >= XF) && (XFY >= requireNonNull(left).getPriority())) {           //XFY has priority, or XFX has failed
                IdentifiedTerm found = exprA(XFY, commaIsEndMarker);
                if (found != null) {
                    ITerm xfy = new HlOpSymbol(token.image, left.getResult(), found.getResult());
                    left = new IdentifiedTerm(XFY, xfy);
                    continue;
                }
            }
            //XF
            if (XF >= requireNonNull(left).getPriority())                   //XF has priority, or XFX and/or XFY has failed
            {
                return new IdentifiedTerm(XF, new HlOpSymbol(token.image, left.getResult()));
            }

            //XFX did not have top priority, but XFY failed
            if (!haveAttemptedXFX && XFX >= left.getPriority()) {
                IdentifiedTerm found = exprA(XFX - 1, commaIsEndMarker);
                if (found != null) {
                    HlOpSymbol xfx = new HlOpSymbol(token.image, left.getResult(), found.getResult());
                    left = new IdentifiedTerm(XFX, xfx);
                    continue;
                }
            }
            break;
        }
        getLexer().unreadToken(token);
        return left;
    }

    //    /**
//     * Parses and returns a valid 'leftside' of an expression.
//     * If the left side starts with a prefix, it consumes other expressions with a lower priority than itself.
//     * If the left side does not have a prefix it must be an expr0.
//     *
//     * @param commaIsEndMarker used when the leftside is part of and argument list of expressions
//     * @param maxPriority operators with a higher priority than this will effectivly end the expression
//     * @return a wrapper of: 1. term correctly structured and 2. the priority of its root operator
//     * @throws InvalidTermException
//     */
    private ITerm parseLeftSide(boolean commaIsEndMarker, int maxPriority) throws Exception {
//        1. prefix expression
        PlToken token = getLexer().readToken();
        if (token.isOperator(commaIsEndMarker)) {
            int FX = getOptable().getPriority(token.image, fx);
            int FY = getOptable().getPriority(token.image, fy);

            if (token.image.equals("-")) {
                PlToken t = getLexer().readToken();
                if (t.isNumber()) {
//                    return termFactory.createNumber(token.image);
                } else {
                    getLexer().unreadToken(t);
                }
                //check that no operator has a priority higher than permitted
                if (FY > maxPriority) {
                    FY = -1;
                }
                if (FX > maxPriority) {
                    FX = -1;
                }
                //FX has priority over FY
                boolean haveAttemptedFX = false;
                if (FX >= FY && FX >= OP_LOW) {
                    IdentifiedTerm found = exprA(FX - 1, commaIsEndMarker);    //op(fx, n) exprA(n - 1)
                    if (found != null) {
                        return new HlOpSymbol(FX, new HlOpSymbol(token.image, found.getResult()));
                    } else {
                        haveAttemptedFX = true;
                    }
                }
                //FY has priority over FX, or FX has failed
                if (FY >= OP_LOW) {
                    IdentifiedTerm found = exprA(FY, commaIsEndMarker); //op(fy,n) exprA(1200)  or   op(fy,n) exprA(n)
                    if (found != null)
                        return new HlOpSymbol(FY, new HlOpSymbol(token.image, found.getResult()));
                }
                //FY has priority over FX, but FY failed
                if (!haveAttemptedFX && FX >= OP_LOW) {
                    IdentifiedTerm found = exprA(FX - 1, commaIsEndMarker);    //op(fx, n) exprA(n - 1)
                    if (found != null) {
                        return new HlOpSymbol(FX, new HlOpSymbol(token.image, found.getResult()));
                    }
                }
            }
            getLexer().unreadToken(token);
            //2. expr0
            return new HlOpSymbol(0, expr0());
        }
        return null;
    }

    /**
     * exprA(0) ::= integer |
     * //     *              float |
     * //     *              variable |
     * //     *              atom |
     * //     *              atom( exprA(1200) { , exprA(1200) }* ) |
     * //     *              '[' exprA(1200) { , exprA(1200) }* [ | exprA(1200) ] ']' |
     * //     *              '{' [ exprA(1200) ] '}' |
     * //     *              '(' exprA(1200) ')'
     * //
     */
    private ITerm expr0() throws Exception {
        PlToken t1 = getLexer().readToken();
        switch (t1.kind) {
            case TK_BOF:
                return BEGIN_OF_FILE_ATOM;
//            break;
            case TK_EOF:
                return END_OF_FILE_ATOM;
//                popTokenSource();
            case TK_DOT:
                break;
            case TK_LPAREN:
                break;
            case TK_RPAREN:
                break;
            case TK_LBRACKET:
                break;
            case TK_RBRACKET:
                break;
            case TK_LBRACE:
                break;
            case TK_RBRACE:
                break;
            case TK_D_QUOTE:
                if (isStringQDelim())
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
                termFactory.createNumber(t1.image);
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
            default:
                throw new IllegalStateException("Unexpected value: " + t1.kind);
        }
//        if (t1.isType(PlLexer.INTEGER))
//            return PlProlog.parseInteger(t1.image); //todo moved method to Number
//
//        if (t1.isType(PlLexer.FLOAT))
//            return PlProlog.parseFloat(t1.image);   //todo moved method to Number
//
//        if (t1.isType(PlLexer.VARIABLE))
//            return new Var(t1.image);             //todo switched to use the internal check for "_" in Var(String)
//
//        if (t1.isType(PlLexer.ATOM) || t1.isType(PlLexer.SQ_SEQUENCE) || t1.isType(PlLexer.DQ_SEQUENCE)) {
//            if (!t1.isFunctor())
//                return new HtFunctor(t1.image);
//
//            String functor = t1.image;
//            PlToken t2 = getLexer().readToken();   //reading left par
//            if (!t2.isType(PlLexer.LPAR))
//                throw new InvalidTermException("bug in parsing process. Something identified as functor misses its first left parenthesis");//todo check can be skipped
//            LinkedList a = expr0_arglist();     //reading arguments
//            PlToken t3 = getLexer().readToken();
//            if (t3.isType(PlLexer.RPAR))      //reading right par
//                return new HtFunctor(functor, a);
//            throw new InvalidTermException("Missing right parenthesis: (" + a + " -> here <-");
//        }
//
//        if (t1.isType(PlLexer.LPAR)) {
//            ITerm term = expr(false);
//            if (getLexer().readToken().isType(PlLexer.RPAR))
//                return term;
//            throw new InvalidTermException("Missing right parenthesis: (" + term + " -> here <-");
//        }
//
//        if (t1.isType(PlLexer.LBRA)) {
        PlToken t2 = getLexer().readToken();
        switch (t2.kind) {
            case TK_BOF:
                break;
            case TK_EOF:
                break;
            case TK_DOT:
                break;
            case TK_LPAREN:
                break;
            case TK_RPAREN:
                break;
            case TK_LBRACKET:
                break;
            case TK_RBRACKET:
                break;
            case TK_LBRACE:
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
            default:
                throw new IllegalStateException("Unexpected value: " + t2.kind);
        }
//        if (t2.isType(PlLexer.RBRA)) {
//            return new HtFunctor();
//        }

        getLexer().unreadToken(t2);
        ITerm term = expr0_list();
//        if (getLexer().readToken().isType(PlLexer.RBRA)) {
//            return term;
//        }
        throw new ParserException("Missing right bracket: [" + term + " -> here <-");
    }
//
//    {
//        PlToken t2 = getLexer().readToken();
//        if (t2.isType(PlLexer.RBRA2))
//            return new HtFunctor("{}");
//
//        getLexer().unreadToken(t2);
//        ITerm arg = null;
//        try {
//            arg = expr(false);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        try {
//            t2 = getLexer().readToken();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        if (t2.isType(PlLexer.RBRA2))
//            return termFactory.newFunctor("{}", new ListTerm(arg));
//        throw new ParserException("Missing right braces: {" + arg + " -> here <-");
//    }

//        throw new ParserException("The following token could not be identified: "+t1.image);


    //    //todo make non-recursive?
    private ITerm expr0_list() throws Exception {
        return readSequence(LIST, TK_RBRACKET);
    }

    private ListTerm expr0_arglist() throws Exception {
        return readSequence(ARGS, TK_RPAREN);
    }

    private ListTerm expr0_bypass() throws Exception {
        return readSequence(BYPASS, TK_RBRACE);
    }

    private ListTerm readSequence(ListTerm.Kind kind, final TokenKind rDelim) throws Exception {
        ITerm tail = ListTerm.NIL;//  = expr(true);
        List<ITerm> heads = new ArrayList<>();
        for (; ; ) {
            PlToken t = getLexer().readToken();
            if (t.kind == rDelim) {
                return new ListTerm(kind, tail, heads.toArray(new ITerm[heads.size()]));
            }
            switch (t.kind) {
                case TK_COMMA:
                    heads.add(expr(true));
                    break;
                case TK_CONS:
                    tail = expr(true);//VAR OR LIST
                    break;
                default:
                    throw new ParserException(String.format("The expression: %s\n" +
                            "is not followed by either a ',' or '|' or ']'.", heads.get(heads.size() - 1)));
            }
        }
    }
//    /**
//     * @return true if the String could be a prolog atom
//     */
//    public static boolean isAtom(String s) {
//        return atom.matcher(s).matches();
//    }
//
//    static private Pattern atom = Pattern.compile("(!|[a-z][a-zA-Z_0-9]*)");
//
    // internal parsing procedures

    private ITerm expr(boolean commaIsEndMarker) throws Exception {
        return requireNonNull(exprA(OP_HIGH, commaIsEndMarker)).getResult();
    }
    // commodity methods to parse numbers

    static Number parseInteger(String s) {
        long num = Long.parseLong(s);
        if (num > Integer.MIN_VALUE && num < MAX_VALUE) {
            return (int) num;
        } else {
            return num;
        }
    }

    static Double parseFloat(String s) {
        return Double.parseDouble(s);
    }

    static Number createNumber(String s) {
        try {
            return parseInteger(s);
        } catch (Exception e) {
            return parseFloat(s);
        }
    }
}
