package org.ltc.hitalk.parser.handlers;

import org.ltc.hitalk.parser.IStateHandler;
import org.ltc.hitalk.parser.ParserState;
import org.ltc.hitalk.parser.PlToken;
import org.ltc.hitalk.term.IdentifiedTerm;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

import static org.ltc.hitalk.parser.Directive.DirectiveKind;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity;

@Deprecated
public class ExprAHandler extends ParserStateHandler {

    /**
     *
     * @param state
     * @param assocs
     * @param dks
     * @param currPriority
     * @param token
     * @throws Exception
     */
    public ExprAHandler(ParserState state,
                        EnumSet<Associativity> assocs,
                        EnumSet<DirectiveKind> dks,
                        int currPriority,
                        PlToken token) throws Exception {
        super(state, assocs, dks, currPriority, token);
    }

    /**
     * @param handler
     */
    protected static void accept(IStateHandler handler) {

    }

    /**
     * @param state
     * @throws Exception
     */
    @Override
    public void doPrepareState(ParserState state) throws Exception {

    }

    /**
     * @param token
     * @throws Exception
     */
    @Override
    public void doCompleteState(PlToken token) throws Exception {
        repeat(ExprAHandler::accept);

    }

    public Set<IdentifiedTerm> tryOperators(String name) {
        return Collections.emptySet();
    }

    public final void repeat(Consumer<IStateHandler> action) {
        action.accept(this);
    }
}