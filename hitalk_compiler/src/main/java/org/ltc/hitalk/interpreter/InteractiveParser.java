package org.ltc.hitalk.interpreter;

import com.thesett.aima.logic.fol.OpSymbol.Associativity;
import com.thesett.aima.logic.fol.Parser;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.Source;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.wam.compiler.HtTokenSource;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.RESOURCE_ERROR;

public
class InteractiveParser implements Parser <HtClause, Token> {
    protected HtPrologParser parser;
    protected VariableAndFunctorInterner interner;

    public
    InteractiveParser ( HtPrologParser parser, VariableAndFunctorInterner interner ) {
        this.parser = parser;
        this.interner = interner;
    }

    public
    InteractiveParser () {
        super();
    }

    /**
     * Establishes the token source to parse from.
     *
     * @param source The token source to parse from.
     */
    @Override
    public
    void setTokenSource ( Source <Token> source ) {
        parser.setTokenSource((HtTokenSource) source);
    }

    /**
     * Parses the next sentence from the current token source.
     *
     * @return The fully parsed syntax tree for the next sentence.
     */
    @Override
    public
    Sentence <HtClause> parse () throws SourceCodeException {
        try {
            return parser.parse();
        } catch (SourceCodeException e) {
            e.printStackTrace();
            throw new ExecutionError(RESOURCE_ERROR, null);
        }
    }

    /**
     * Sets up a custom operator symbol on the parser.
     *
     * @param operatorName  The name of the operator to create.
     * @param priority      The priority of the operator, zero unsets it.
     * @param associativity The operators associativity.
     */
    @Override
    public
    void setOperator ( String operatorName, int priority, Associativity associativity ) {

    }

    public
    void setParser ( HtPrologParser parser ) {
        this.parser = parser;
    }

    public
    HtPrologParser getParser () {
        return parser;
    }
}
