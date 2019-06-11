package org.ltc.hitalk.wam.compiler;


import com.thesett.aima.logic.fol.OpSymbol;
import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.parser.HiLogParser;

/**
 *
 */
public
class HiTalkParserListener extends HiLogParserBaseListener implements Parser <Term, Token> {


    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     *
     * @param ctx
     */
    @Override
    public
    void enterClause ( HiLogParser.ClauseContext ctx ) {

    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     *
     * @param ctx
     */
    @Override
    public
    void exitClause ( HiLogParser.ClauseContext ctx ) {

    }

    /**
     * Establishes the token source to parse from.
     *
     * @param source The token source to parse from.
     */
    @Override
    public
    void setTokenSource ( Source source ) {

    }

    /**
     * Parses the next sentence from the current token source.
     *
     * @return The fully parsed syntax tree for the next sentence.
     * @throws SourceCodeException If the source being parsed does not match the grammar.
     */
    @Override
    public
    Sentence <Term> parse () throws SourceCodeException {
        return null;
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
    void setOperator ( String operatorName, int priority, OpSymbol.Associativity associativity ) {

    }
}
