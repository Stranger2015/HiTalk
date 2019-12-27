package org.ltc.hitalk.parser;

import com.thesett.common.util.Source;
import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.parser.PlToken.TokenKind;
import org.ltc.hitalk.term.*;
import org.ltc.hitalk.term.HlOpSymbol.Associativity;
import org.ltc.hitalk.term.HlOpSymbol.Fixity;
import org.ltc.hitalk.term.ListTerm.Kind;
import org.ltc.hitalk.term.io.HiTalkInputStream;
import org.ltc.hitalk.wam.compiler.HtFunctor;
import org.ltc.hitalk.wam.compiler.HtFunctorName;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.ltc.hitalk.wam.compiler.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static java.lang.String.format;
import static java.util.EnumSet.of;
import static org.ltc.hitalk.core.BaseApp.getAppContext;
import static org.ltc.hitalk.core.utils.TermUtilities.convertToClause;
import static org.ltc.hitalk.parser.HiLogParser.hilogApply;
import static org.ltc.hitalk.parser.PlToken.TokenKind.*;
import static org.ltc.hitalk.term.HlOpSymbol.Associativity.*;
import static org.ltc.hitalk.term.ListTerm.Kind.*;
import static org.ltc.hitalk.wam.compiler.Language.PROLOG;

/**
 * @author shun
 */
public class PlPrologParser implements IParser {
    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    public static final String BEGIN_OF_FILE = "begin_of_file";
    public static final String END_OF_FILE = "end_of_file";

    public static final Atom END_OF_FILE_ATOM =
            getAppContext().getTermFactory().newAtom(END_OF_FILE);
    public static final Atom BEGIN_OF_FILE_ATOM =
            getAppContext().getTermFactory().newAtom(BEGIN_OF_FILE);
    //    protected final ITermFactory factory;
    protected IOperatorTable operatorTable = new PlDynamicOperatorParser();
    protected final Deque <PlTokenSource> tokenSourceStack = new ArrayDeque <>();
    protected IVafInterner interner;
    protected ITermFactory termFactory;

    /**
     * @param inputStream
     * @param factory
     * @param optable
     */
    public PlPrologParser ( HiTalkInputStream inputStream,
                            IVafInterner interner,
                            ITermFactory factory,
                            IOperatorTable optable ) {
        setTokenSource(new PlLexer(inputStream));
        this.interner = interner;
        this.termFactory = factory;
        this.operatorTable = optable;

        this.initializeBuiltIns();
    }

    /**
     *
     */
    public PlPrologParser () {
        this(getAppContext().getInputStream(),
                getAppContext().getInterner(),
                getAppContext().getTermFactory(),
                getAppContext().getOpTable());
    }

    public void toString0 ( StringBuilder sb ) {
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
        return operatorTable == null ? new PlDynamicOperatorParser() : operatorTable;
    }

    /**
     * @param optable
     */
    @Override
    public void setOptable ( IOperatorTable optable ) {
        this.operatorTable = optable;
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
    public ITerm parse () throws Exception {
        return termSentence();
    }

    /**
     * @return
     */
    @Override
    public ITerm next () throws Exception {
        PlToken token = (getLexer()).next(true);
        return newTerm(token, false, EnumSet.of(DOT, EOF));
    }

    /**
     * @return
     */
    public HtClause parseClause () throws Exception {
        return convertToClause(termSentence(), getAppContext().getInterner());
    }

    /**
     * @return
     */
    @Override
    public PlTokenSource getTokenSource () {
        return tokenSourceStack.peek();
    }

    /**
     *
     */
    public void popTokenSource () {
        if (!tokenSourceStack.isEmpty()) {
            tokenSourceStack.pop();
        }
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
            throws Exception {
        if (token.kind == EOF/* && getLexer() == null*/) {
            return END_OF_FILE_ATOM;
        }
        HlOperatorJoiner <ITerm> joiner = new HlOperatorJoiner <ITerm>() {
            @Override
            protected ITerm join ( int notation, List <ITerm> args ) {
                return new HtFunctor(notation, args.toArray(new ITerm[args.size()]));
            }
        };
        outer:
        for (int i = 0; ; ++i, token = getLexer().next(joiner.accept(x))) {
            if (token.kind == EOF) {
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
                    for (HlOpSymbol right : getOptable().getOperatorsMatchingNameByFixity(token.image).values()) {
                        if (joiner.accept(right.getAssociativity())) {
                            joiner.push(right);
                            continue outer;
                        }
                    }
                }
            }
            if (!joiner.accept(x)) {
                throw new ParseException("Impossible to resolve operator! token = " + token);
            }
            joiner.push(literal(token));
        }
    }

    public PlLexer getLexer () {
        return (PlLexer) getTokenSource();
    }

    /**
     * @param token
     * @return
     * @throws IOException
     * @throws ParseException
     */
    protected ITerm literal ( PlToken token ) throws Exception {
        ITerm term = null;
        switch (token.kind) {
            case VAR:
                term = termFactory.newVariable(token.image);
                break;
            case FUNCTOR_BEGIN:
                term = compound(token.image);
                break;
            case ATOM:
                term = termFactory.newAtom(token.image);
                break;
            case INTEGER_LITERAL:
                term = termFactory.newAtomic(Integer.parseInt(token.image));
                break;
            case FLOATING_POINT_LITERAL:
                term = termFactory.newAtomic(Double.parseDouble(token.image));
                break;
            case BOF:
                term = BEGIN_OF_FILE_ATOM;
//                getTokenSource().setBofGenerated(false);
                getTokenSource().setEncodingPermitted(true);
                break;
            case EOF:
                term = END_OF_FILE_ATOM;
                popTokenSource();
                break;
            case LPAREN:
                term = readSequence(token.kind, RPAREN, true);//blocked sequence
                break;
            case LBRACKET:
                term = readSequence(token.kind, RBRACKET, false);
                break;
            case LBRACE:
                term = readSequence(token.kind, RBRACE, false);
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
    protected ListTerm readSequence ( TokenKind ldelim, TokenKind rdelim, boolean isBlocked ) throws Exception {
        List <ITerm> elements = new ArrayList <>();
        EnumSet <TokenKind> rdelims = isBlocked ? of(COMMA, rdelim) : of(COMMA, CONS, rdelim);
        Kind kind = LIST;
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
        ITerm term = newTerm(getLexer().getNextToken(), true, rdelims);
        if (term == null) {
            if (rdelim != getLexer().peek().kind) {
                throw new ParseException("No (more) elements");//要素がありません・・・。
            }
            elements.add(term);
            while (COMMA == getLexer().peek().kind) {//todo flatten
                elements.add(newTerm(getLexer().getNextToken(), false, rdelims));
            }
            if (CONS == getLexer().peek().kind) {
                newTerm(getLexer().getNextToken(), true, rdelims);
            }

            return termFactory.newListTerm(flatten(elements));
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
    protected IFunctor compound ( String name ) throws Exception {
        ListTerm args = readSequence(LPAREN, RPAREN, false);
        return compound(name, args);
    }

    protected IFunctor compound ( String name, ListTerm args ) throws Exception {
        return termFactory.newFunctor(hilogApply, name, args);
    }

    /**
     * @param source
     */
    public void setTokenSource ( PlTokenSource source ) {
//        logger.info(source.toString());
        tokenSourceStack.push(source);
    }

    /**
     * @param source
     */
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
    public ITerm termSentence () throws Exception {
        // Each new sentence provides a new scope in which to make variables unique.
        variableContext.clear();

        if (!tokenSourceStack.isEmpty()) {
            return literal((getLexer()).getNextToken());
        }

        return literal(PlToken.newToken(EOF));
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
        logger.info(format("Operator \"%s\", %d, %s", operatorName, priority, associativity));

        int arity;
        if ((associativity == xfy) || (associativity == yfx) || (associativity == xfx)) {
            arity = 2;
        } else {
            arity = 1;
        }

        int name = getInterner().internFunctorName(operatorName, arity);
        getOptable().setOperator(name, operatorName, priority, associativity);
    }

    /**
     * Interns and inserts into the operator table all of the built in operators and names in Prolog.
     */
    public void initializeBuiltIns () {
        logger.info("Initializing built-in's...");
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

    public String toString () {
        final StringBuilder sb = new StringBuilder("PlPrologParser{");
        sb.append("tokenSourceStack=").append(tokenSourceStack);
        sb.append('}');
        return sb.toString();
    }
}
