package org.ltc.hitalk.compiler.parser;

import com.thesett.aima.logic.fol.Cons;
import com.thesett.aima.logic.fol.Nil;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.isoprologparser.PrologParser;
import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.aima.logic.fol.isoprologparser.TokenSource;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.parsing.SourceCodePosition;
import com.thesett.common.parsing.SourceCodePositionImpl;
import org.ltc.hitalk.term.HiLogCompound;
import org.ltc.hitalk.term.ListTerm;

import java.util.Arrays;

import static com.thesett.aima.logic.fol.OpSymbol.Associativity.FX;

public
class HiLogParser extends PrologParser {

    final protected static int HILOG_COMPOUND = 38;
    private static final String BEGIN_TERM_TOKENS = Arrays.toString(new String[]{
            tokenImage[FUNCTOR], tokenImage[LSQPAREN], tokenImage[VAR], tokenImage[INTEGER_LITERAL], tokenImage[FLOATING_POINT_LITERAL], tokenImage[STRING_LITERAL], tokenImage[ATOM], tokenImage[LPAREN]
    });
    protected static final ListTerm NIL = new ListTerm(new Term[0]);//fixme

    /**
     * Builds a
     * public
     * prolog parser on a token source to be parsed.
     *
     * @param interner The interner for variable and functor names.
     */
    public
    HiLogParser ( TokenSource source, VariableAndFunctorInterner interner ) {
        super(source, interner);
        //

    }

    /**
     * Parses a list expressed as a sequence of functors in first order logic. The empty list consists of the atom 'nil'
     * and a non-empty list consists of the functor 'cons' with arguments the head of the list and the remainder of the
     * list.
     * <p>
     * <p/>A list can be empty '[]', contains a sequence of terms seperated by commas '[a,...]', contain a sequence of
     * terms separated by commas consed onto another term '[a,...|T]'. In the case where a term is consed onto the end,
     * if the term is itself a list the whole will form a list with a cons nil terminator, otherwise the term will be
     * consed onto the end as the list terminal.
     *
     * @return A list expressed as a sequence of functors in first order.
     * @throws SourceCodeException If the token sequence does not parse as a valid list.
     */
    public
    Term listFunctor ( int delim ) throws SourceCodeException { //fixme
        // Get the interned names of the nil and cons functors.
        int nilId = interner.internFunctorName("nil", 0);
        int consId = interner.internFunctorName("cons", 2);

        // A list always starts with a '['.
        Token leftDelim = consumeToken((delim == RSQPAREN) ? LSQPAREN : LPAREN);

        // Check if the list contains any arguments and parse them if so.
        Term[] args = null;

        Token nextToken = tokenSource.peek();

        switch (nextToken.kind) {
            case LPAREN:
            case LSQPAREN:
            case INTEGER_LITERAL:
            case FLOATING_POINT_LITERAL:
            case STRING_LITERAL:
            case VAR:
            case FUNCTOR:
            case ATOM:
                args = arglist();
                break;

            default:
        }

        // Work out what the terminal element in the list is. It will be 'nil' unless an explicit cons '|' has
        // been used to specify a different terminal element. In the case where cons is used explciitly, the
        // list prior to the cons must not be empty.
        Term accumulator;

        if (tokenSource.peek().kind == CONS) {
            if (args == null) {
                throw new SourceCodeException("Was expecting one of " + BEGIN_TERM_TOKENS + " but got " + tokenImage[nextToken.kind] + ".", null, null, null, new SourceCodePositionImpl(nextToken.beginLine, nextToken.beginColumn, nextToken.endLine, nextToken.endColumn));
            }

            consumeToken(CONS);

            accumulator = term();
        }
        else {
            accumulator = new Nil(nilId, null);
        }

        // A list is always terminated with a ']'.
        Token rightDelim = consumeToken(RSQPAREN);

        // Walk down all of the lists arguments joining them together with cons/2 functors.
        if (args != null) // 'args' will contain one or more elements if not null.
        {
            for (int i = args.length - 1; i >= 0; i--) {
                Term previousAccumulator = accumulator;

                //accumulator = new Functor(consId.ordinal(), new Term[] { args[i], previousAccumulator });
                accumulator = new Cons(consId, new Term[]{args[i], previousAccumulator});
            }
        }

        // Set the position that the list was parsed from, as being the region between the '[' and ']' brackets.
        SourceCodePosition position = new SourceCodePositionImpl(leftDelim.beginLine, leftDelim.beginColumn, rightDelim.endLine, rightDelim.endColumn);
        accumulator.setSourceCodePosition(position);

        // The cast must succeed because arglist must return at least one argument, therefore the cons generating
        // loop must have been run at least once. If arglist is not called at all because an empty list was
        // encountered, then the accumulator will contain the 'nil' constant which is a functor of arity zero.
        return accumulator;
    }

    /**
     * * Parses a sequence of terms as a comma seperated argument list. The ',' operator in prolog can be used as an
     * * operator, when it behaves as a functor of arity 2, or it can be used to separate a sequence of terms that are
     * * arguments to a functor or list. The sequence of functors must first be parsed as a term, using the operator
     * * precedence of "," to form the term. This method takes such a term and flattens it back into a list of terms,
     * * breaking it only on a sequence of commas. Terms that have been parsed as a bracketed expression will not be
     * * broken up.
     * *
     * * <p/>For example, 'a, b, c' is broken into the list { a, b, c}. The example, 'a, (b, c), d' is broken into the
     * * list { a, (b, c), d} and so on.
     * *
     * ===============================================================================================================
     * Parses a sequence of terms as a comma seperated argument list. The ',' operator in prolog can be used as an
     * operator, when it behaves as a functor of arity 2, or it can be used to separate a sequence of terms that are
     * arguments to a functor or list. The sequence of functors must first be parsed as a term, using the operator
     * precedence of "," to form the term. This method takes such a term and flattens it back into a list of terms,
     * breaking it only on a sequence of commas. Terms that have been parsed as a bracketed expression will not be
     * broken up.
     * <p>
     * <p/>For example, 'a, b, c' is broken into the list { a, b, c}. The example, 'a, (b, c), d' is broken into the
     * list { a, (b, c), d} and so on.
     *
     * @return A sequence of terms parsed as a term, then flattened back into a list seperated on commas.
     * @throws SourceCodeException If the token sequence is not a valid term.
     */
    @Override
    public
    Term[] arglist () throws SourceCodeException {

        return super.arglist();
    }


    /**
     * Parses a list expressed as a sequence of functors in first order logic. The empty list consists of the atom 'nil'
     * and a non-empty list consists of the functor 'cons' with arguments the head of the list and the remainder of the
     * list.
     * <p>
     * <p/>A list can be empty '[]', contains a sequence of terms seperated by commas '[a,...]', contain a sequence of
     * terms seperated by commas consed onto another term '[a,...|T]'. In the case where a term is consed onto the end,
     * if the term is itself a list the whole will form a list with a cons nil terminator, otherwise the term will be
     * consed onto the end as the list terminal.
     *
     * @return A list expressed as a sequence of functors in first order.
     * @throws SourceCodeException If the token sequence does not parse as a valid list.
     */
    @Override
    public
    Term listFunctor () throws SourceCodeException {
        return listFunctor(RSQPAREN);
    }

    public
    Term getHilogCompound () {
        Term c = null;
        try {
            Term args = null;
            Token nextToken = tokenSource.peek();
            if (nextToken.kind == FUNCTOR) {
                return functor();
            }
            else {
                Term t = term();
                nextToken = tokenSource.peek();
                if (nextToken.kind == LPAREN) {
                    args = this.listFunctor(RPAREN);
                }

                c = new HiLogCompound(t, args);
            }

        } catch (SourceCodeException e) {
            e.printStackTrace();
        }

        return c;
    }

    /**
     * Parses multiple sequential terms, and if more than one is encountered then the flat list of terms encountered
     * must contain operators in order to be valid Prolog syntax. In that case the flat list of terms is passed to the
     * {@link (Term[])} method for 'deferred decision parsing' of dynamic operators.
     *
     * @return A single first order logic term.
     * @throws SourceCodeException If the sequence of tokens does not form a valid syntactical construction as a first
     *                             order logic term.
     */
    @Override
    public
    Term term () throws SourceCodeException {

        return super.term();
    }

    /**
     * Parses a single functor in first order logic with its arguments.
     *
     * @return A single functor in first order logic with its arguments.
     * @throws SourceCodeException If the token sequence does not parse as a valid functor.
     */
    @Override
    public
    Term functor () throws SourceCodeException {

        return super.functor();
    }


    /**
     * Interns and inserts into the operator table all of the built in operators and functors in Prolog.
     */
    protected
    void initializeBuiltIns () {
        super.initializeBuiltIns();

        internOperator("hilog", 1150, FX);
    }
}
