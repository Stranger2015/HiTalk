package org.ltc.hitalk.parser;

import com.thesett.common.util.Source;
import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

import static java.util.EnumSet.of;
import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.REPRESENTATION_ERROR;
import static org.ltc.hitalk.core.BaseApp.getAppContext;
import static org.ltc.hitalk.core.utils.TermUtilities.convertToClause;
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
    public static final Atom END_OF_FILE_ATOM = getAppContext().getTermFactory().newAtom(END_OF_FILE);
    public static final Atom BEGIN_OF_FILE_ATOM = getAppContext().getTermFactory().newAtom(BEGIN_OF_FILE);
    public static final String ANONYMOUS = "_";

    protected final Deque <PlTokenSource> tokenSourceStack = new ArrayDeque <>();
    //    public static final ListTerm CONJUNCTION = getAppContext().getTermFactory().newListTerm(CLAUSE_BODY);
    protected IOperatorTable operatorTable;
    protected IVafInterner interner;
    protected ITermFactory termFactory;
    /**
     * Holds the variable scoping context for the current sentence.
     */
    protected Map <Integer, HtVariable> variableContext = new HashMap <>();
    protected HlOpSymbol operator;

    /**
     * @param inputStream
     * @param factory
     * @param optable
     */
    public PlPrologParser ( HiTalkInputStream inputStream,
                            IVafInterner interner,
                            ITermFactory factory,
                            IOperatorTable optable ) throws FileNotFoundException {
        setTokenSource(new PlLexer(inputStream));
        this.interner = interner;
        this.termFactory = factory;
        this.operatorTable = optable;

//        this.initializeBuiltIns();
    }

    /**
     *
     */
    public PlPrologParser () throws FileNotFoundException {
        this(getAppContext().getInputStream(),
                getAppContext().getInterner(),
                getAppContext().getTermFactory(),
                getAppContext().getOpTable());
    }

    public void toString0 ( StringBuilder sb ) {
    }

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
        PlToken token = (getLexer()).poll();
        return newTerm(token, false, EnumSet.of(EOF));
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
     * @param source
     */
    public void setTokenSource ( PlTokenSource source ) {
        logger.info("Adding ts " + source.getPath() + source.isOpen());
        if (!tokenSourceStack.contains(source)) {
            tokenSourceStack.push(source);
        }
        logger.info("declined  Adding dup ts " + source.getPath());
    }

    /**
     * @param source
     */
    public void setTokenSource ( Source <PlToken> source ) {
        setTokenSource((PlTokenSource) source);
    }

    /**
     * @return
     */
    @Override
    public PlTokenSource popTokenSource () throws IOException {
        if (!tokenSourceStack.isEmpty()) {
            final PlTokenSource ts = tokenSourceStack.pop();
            ts.close();
            return ts;
        }

        throw new ExecutionError(REPRESENTATION_ERROR, null);
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
        logger.info(token.toString());
        ITerm result = null;
        boolean finished = false;
        if (token.kind == BOF) {
            result = BEGIN_OF_FILE_ATOM;
        } else if (token.kind == EOF) {
            result = END_OF_FILE_ATOM;
        } else {
            HlOperatorJoiner <ITerm> joiner = new HlOperatorJoiner <ITerm>() {
                @Override
                protected ITerm join ( int notation, List <ITerm> args ) {
                    return new HtFunctor(notation, new ListTerm(args.toArray(new ITerm[args.size()])));
                }
            };
            outer:
            for (int i = 0; ; ++i, token = getLexer().next(joiner.accept(x))) {
                if (token.kind == EOF) {
                    throw new ParseException("Premature EOF");
                }
                if (!token.quote) {
                    if (nullable && i == 0 || joiner.accept(xf)) {
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
                    if (token.kind == ATOM) {
                        logger.info(token.image);
                        final EnumMap <Fixity, HlOpSymbol> syms = getOptable().getOperatorsMatchingNameByFixity(token.image);
//                        final Collection <HlOpSymbol> values = getOptable().getOperatorsMatchingNameByFixity(token.image);.values();
                        if (syms == null) {
                            return result;
                        }
                        for (HlOpSymbol right : syms.values()) {
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

        return result;
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
            case D_QUOTE:
                break;
            case S_QUOTE:
                token.quote = true;
                term = termFactory.newAtom(token.image);
                break;
            case B_QUOTE:
                break;
            default:
                throw new ParseException("Unexpected token: " + token);
        }

        return term;
    }

    protected ListTerm readSequence ( TokenKind ldelim, TokenKind rdelim, boolean isBlocked ) throws Exception {
        List <ITerm> elements = new ArrayList <>();
        EnumSet <TokenKind> rdelims = isBlocked ? of(rdelim) : of(/*COMMA, CONS, */rdelim);
        Kind kind;
        switch (ldelim) {
            case LPAREN:
                kind = CLAUSE_BODY;
//                if (isBlocked) {
//                    kind = CLAUSE_BODY;
//                } else {
//                    kind = LIST;//compound args
//                }
                break;
            case LBRACKET:
                kind = LIST;
                break;
            case LBRACE:
                kind = BYPASS;
                break;
            default:
                throw new IllegalArgumentException();
        }
        ITerm term = newTerm(getLexer().getNextToken(), true, rdelims);
        if (term == null) {
            if (rdelim != getLexer().peek().kind) {
                throw new ParseException("No (more) elements");
            }
            elements.add(term);
//            remove this
            while (COMMA == getLexer().peek().kind) {//todo flatten
                elements.add(newTerm(getLexer().getNextToken(), false, rdelims));
            }
            if (CONS == getLexer().peek().kind) {
                elements.add(newTerm(getLexer().getNextToken(), true, rdelims));
            }

            return termFactory.newListTerm(kind, flatten(elements));
        }

        return termFactory.newListTerm(kind, flatten(elements));
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
    public ITerm termSentence () throws Exception {
        // Each new sentence provides a new scope in which to make variables unique.
        variableContext.clear();

        if (!tokenSourceStack.isEmpty() && tokenSourceStack.peek().isOpen()) {
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
//        logger.info(format("Operator \"%s\", %d, %s", operatorName, priority, associativity));

        int arity;
        if ((associativity == xfy) || (associativity == yfx) || (associativity == xfx)) {
            arity = 2;
        } else {
            arity = 1;
        }

        int name = getInterner().internFunctorName(operatorName, arity);
        getOptable().setOperator(name, operatorName, priority, associativity);
    }

    public void op ( int priority, Associativity associativity, String... operatorNames ) {
        IntStream.range(0, operatorNames.length).forEachOrdered(i ->
                internOperator(operatorNames[i], priority, associativity));
    }

    /**
     * Interns and inserts into the operator table all of the built in operators and names in Prolog.
     */
    public void initializeBuiltIns () {
        logger.info("Initializing built-in operators...");

        // Initializes the operator table with the standard ISO prolog built-in operators.
        internOperator(PrologAtoms.IMPLIES, 1200, xfx);
        internOperator(PrologAtoms.IMPLIES, 1200, fx);
        internOperator(PrologAtoms.DCG_IMPLIES, 1200, xfx);
        internOperator(PrologAtoms.QUERY, 1200, fx);
        internOperator(PrologAtoms.QUESTION, 500, fx);

        internOperator(PrologAtoms.SEMICOLON, 1100, xfy);
        internOperator(PrologAtoms.IF, 1050, xfy);
        internOperator(PrologAtoms.IF_STAR, 1050, xfy);

//        internOperator(PrologAtoms.COMMA, 1000, xfy);
        internOperator(PrologAtoms.ASSIGN, 990, xfy);
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
        internOperator(PrologAtoms.BSLASH, 500, fy);
        internOperator(PrologAtoms.BSLASH_SLASH, 500, yfx);
        internOperator(PrologAtoms.SLASH_BSLASH, 500, yfx);

        op(400, yfx,
                PrologAtoms.SLASH,
                PrologAtoms.SLASH_SLASH,
                PrologAtoms.STAR,
                PrologAtoms.RSHIFT,
                PrologAtoms.LSHIFT,
                PrologAtoms.REM,
                PrologAtoms.MOD,
                PrologAtoms.XOR,
                PrologAtoms.REM,
                PrologAtoms.REM,
                PrologAtoms.REM,
                );
        internOperator(PrologAtoms.SLASH, 400, yfx);
        internOperator(PrologAtoms.SLASH_SLASH, 400, yfx);
        internOperator(PrologAtoms.STAR, 400, yfx);
        internOperator(PrologAtoms.RSHIFT, 400, yfx);
        internOperator(PrologAtoms.LSHIFT, 400, yfx);
        internOperator(PrologAtoms.REM, 400, yfx);
        internOperator(PrologAtoms.MOD, 400, yfx);

        internOperator(PrologAtoms.PLUS, 500, yfx);
        internOperator(PrologAtoms.PLUS, 200, fy);
        internOperator(PrologAtoms.MINUS, 500, yfx);
        internOperator(PrologAtoms.MINUS, 200, fy);
        internOperator(PrologAtoms.COLON, 600, xfy);
        internOperator(PrologAtoms.UP, 200, yfx);
        internOperator(PrologAtoms.UP_UP, 200, yfx);
        internOperator(PrologAtoms.STAR_STAR, 200, xfx);
        internOperator(PrologAtoms.AS, 200, fy);
        internOperator(PrologAtoms.VBAR, 1100, xfx);
        internOperator(PrologAtoms.VBAR, 1100, fx);
//        internOperator(PrologAtoms.DOT, 100, yfx);
        internOperator(PrologAtoms.DOLLAR, 1, fx);

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
}
