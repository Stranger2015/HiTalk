package org.ltc.hitalk.parser;

import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.Source;
import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.core.BaseApp;
import org.ltc.hitalk.core.utils.TermUtilities;
import org.ltc.hitalk.parser.PlToken.TokenKind;
import org.ltc.hitalk.term.*;
import org.ltc.hitalk.term.HlOpSymbol.Associativity;
import org.ltc.hitalk.term.HlOpSymbol.Fixity;
import org.ltc.hitalk.term.io.HiTalkStream;
import org.ltc.hitalk.wam.compiler.HtFunctor;
import org.ltc.hitalk.wam.compiler.HtFunctorName;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.ltc.hitalk.wam.compiler.Language;

import java.io.IOException;
import java.util.*;

import static java.util.EnumSet.of;
import static org.ltc.hitalk.core.BaseApp.getAppContext;
import static org.ltc.hitalk.parser.HiLogParser.hilogApply;
import static org.ltc.hitalk.parser.PlToken.TokenKind.*;
import static org.ltc.hitalk.term.HlOpSymbol.Associativity.*;
import static org.ltc.hitalk.term.ListTerm.Kind.*;
import static org.ltc.hitalk.wam.compiler.Language.PROLOG;

/**
 * @author shun
 */
public class PlPrologParser implements IParser {

    public static final String BEGIN_OF_FILE = "begin_of_file";
    public static final String END_OF_FILE = "end_of_file";

    public static ITerm BEGIN_OF_FILE_ATOM;
    public static ITerm END_OF_FILE_ATOM;

    protected HiTalkStream stream;
    protected PlLexer lexer;
    protected final ITermFactory factory;
    protected final IOperatorTable operatorTable;
    protected final Deque <PlTokenSource> tokenSourceStack = new ArrayDeque <>();
    protected IVafInterner interner;
    protected ITermFactory termFactory;

    /**
     * @param stream
     * @param factory
     * @param optable
     */
    public PlPrologParser ( HiTalkStream stream,
                            IVafInterner interner,
                            ITermFactory factory,
                            IOperatorTable optable ) {
        this.stream = stream;
        lexer = new PlLexer(stream);
        this.interner = interner;
        this.factory = factory;
        this.operatorTable = optable;
    }

    /**
     *
     */
    public PlPrologParser () {
        this(
                getAppContext().getStream(),
                getAppContext().getInterner(),
                getAppContext().getTermFactory(),
                getAppContext().getOpTable());
    }

    /**
     * Describes the possible system directives in interactive mode.
     */
    public enum Directive {
        Trace, Info, User, File
    }

    /**
     * Holds the variable scoping context for the current sentence.
     */
    protected Map <Integer, HtVariable> variableContext = new HashMap <>();
    protected HlOpSymbol operator;

    @Override
    public PlPrologParser getParser () {
        return this;
    }

    public IVafInterner getInterner () {
        return interner;
    }

    public void setInterner ( IVafInterner interner ) {
        this.interner = interner;
    }

    public ITermFactory getFactory () {
        return termFactory;
    }

    public IOperatorTable getOptable () {
        return null;
    }

    public void setOptable ( IOperatorTable optable ) {

    }

    /**
     * @return
     */
    public Language language () {
        return PROLOG;
    }

    /**
     * @return
     */
    public ISentence <ITerm> parse () throws ParseException, IOException {
        return new PlSentenceImpl(termSentence());
    }

    /**
     * @return
     */
    @Override
    public ITerm next () throws IOException, ParseException {
        PlToken token = lexer.next(true);
        if (token == null) {
            token = PlToken.newToken(EOF);
        }
        return newTerm(token, false, EnumSet.of(DOT));
    }

    /**
     * @return
     */
    public HtClause parseClause () throws ParseException, SourceCodeException, IOException {
        return TermUtilities.convertToClause(termSentence(), BaseApp.getAppContext().getInterner());
    }

    /**
     * @return
     */
    @Override
    public PlTokenSource getTokenSource () {
        return tokenSourceStack.peek();
    }

    /**
     * @return
     */
    public PlTokenSource popTokenSource () {
        return tokenSourceStack.pop();
    }

    /**
     * @param token
     * @param nullable
     * @param terminators
     * @return
     * @throws IOException
     * @throws ParseException
     */
    protected ITerm newTerm ( PlToken token, boolean nullable, EnumSet <TokenKind> terminators )
            throws IOException, ParseException {
        HlOperatorJoiner <ITerm> joiner = new HlOperatorJoiner <ITerm>() {
            @Override
            protected ITerm join ( int notation, List <ITerm> args ) {
                return new HtFunctor(notation, args.toArray(new ITerm[args.size()]));
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
                    for (HlOpSymbol right : operatorTable.getOperatorsMatchingNameByFixity(token.image).values()) {
                        if (joiner.accept(right.getAssociativity())) {
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

    /**
     * @param token
     * @return
     * @throws IOException
     * @throws ParseException
     */
    protected ITerm literal ( PlToken token ) throws IOException, ParseException {
        ITerm term = new ErrorTerm();
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
                if (!getTokenSource().isBofGenerated()) {
                    term = factory.newAtom(BEGIN_OF_FILE);
                    BEGIN_OF_FILE_ATOM = term;
                    getTokenSource().setBofGenerated(true);
                    getTokenSource().setEncodingPermitted(true);
                }
                break;
            case EOF:
                if (!getTokenSource().isEofGenerated()) {
                    term = factory.newAtom(END_OF_FILE);
                    term = factory.newAtom(BEGIN_OF_FILE);
                    END_OF_FILE_ATOM = term;
                    getTokenSource().setEofGenerated(true);
                    popTokenSource();
                }
                break;
            case LPAREN:
                ListTerm listTerm = readSequence(token.kind, RPAREN, true);//blocked sequence
                break;
            case LBRACKET:
                listTerm = readSequence(token.kind, RBRACKET, false);
                break;
            case LBRACE:
                listTerm = readSequence(token.kind, RBRACE, false);
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
    protected ListTerm readSequence ( TokenKind ldelim, TokenKind rdelim, boolean isBlocked ) throws IOException, ParseException {
        List <ITerm> elements = new ArrayList <>();
        EnumSet <TokenKind> rdelims = isBlocked ? of(COMMA, rdelim) : of(COMMA, CONS, rdelim);
        ListTerm.Kind kind = LIST;
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
        ITerm term = newTerm(lexer.getNextToken(), true, rdelims);
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

            return factory.newListTerm(kind, flatten(elements));
        }

        return (ListTerm) term;
    }

    private ITerm[] flatten ( List <ITerm> elements ) {
        return elements.toArray(new ITerm[elements.size()]);
    }

    /**
     * @param name
     * @return
     * @throws IOException
     * @throws ParseException
     */
    protected IFunctor compound ( String name ) throws IOException, ParseException {
        ListTerm args = readSequence(LPAREN, RPAREN, false);
        return compound(name, args);
    }

    protected IFunctor compound ( String name, ListTerm args ) throws IOException, ParseException {
        return factory.newFunctor(hilogApply, name, args);
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
//    @Override?
    public void setTokenSource ( Source <PlToken> source ) {
        setTokenSource((PlTokenSource) source);
    }

    /**
     * Parses a single terms, or atom (a name with arity zero), as a sentence in first order logic. The sentence will
     * be parsed in a fresh variable context, to ensure its variables are scoped to within the term only. The sentence
     * does not have to be terminated by a full stop. This method is not generally used by Prolog, but is provided as a
     * convenience to languages over terms, rather than clauses.
     *
     * @return A term parsed in a fresh variable context.
     */
    public ITerm termSentence () throws IOException, ParseException {
        // Each new sentence provides a new scope in which to make variables unique.
        variableContext.clear();

        if (tokenSourceStack.size() > 0) {
            return literal(lexer.getNextToken());
        }
        return null;
    }

    //    @Override
    public void setOperator ( String operatorName, int priority, Associativity associativity ) {
        EnumMap <Fixity, HlOpSymbol> ops = operatorTable.getOperatorsMatchingNameByFixity(operatorName);
        if (ops == null || ops.isEmpty()) {
            int arity = calcArity(associativity);
            int name = interner.internFunctorName(operatorName, arity);
            operatorTable.setOperator(name, operatorName, priority, associativity);
        }
    }

    private int calcArity ( Associativity associativity ) {
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
    public void internOperator ( String operatorName, int priority, Associativity associativity ) {
        int arity;

        if ((associativity == xfy) | (associativity == yfx) | (associativity == xfx)) {
            arity = 2;
        } else {
            arity = 1;
        }

        int name;
        name = interner.internFunctorName(operatorName, arity);
        operatorTable.setOperator(name, operatorName, priority, associativity);
    }

    /**
     * Interns and inserts into the operator table all of the built in operators and names in Prolog.
     */
    public void initializeBuiltIns () {
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
//        internOperator(PrologAtoms.VBAR, 1001, xfy);
//        internOperator(PrologAtoms.VBAR, 1001, fy);

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

    public Directive peekAndConsumeDirective () {

        return null;//fixme
    }
}
