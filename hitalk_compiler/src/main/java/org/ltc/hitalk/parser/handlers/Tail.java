package org.ltc.hitalk.parser.handlers;


import org.ltc.hitalk.parser.Directive;
import org.ltc.hitalk.parser.ParserException;
import org.ltc.hitalk.parser.ParserState;
import org.ltc.hitalk.parser.PlToken;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.IdentifiedTerm;

import java.util.EnumSet;

import static org.ltc.hitalk.core.BaseApp.appContext;
import static org.ltc.hitalk.parser.PlToken.TokenKind.TK_VAR;

/**
 *
 */
public class Tail extends ParserStateHandler {

    /**
     * @param state
     * @param assocs
     * @param dks
     * @param currPriority
     * @param token
     */
    public Tail(
            ParserState state,
            EnumSet<IdentifiedTerm.Associativity> assocs,
            EnumSet<Directive.DirectiveKind> dks,
            int currPriority,
            PlToken token) throws Exception {
        super(state, assocs, dks, currPriority, token);
    }

//    @Override
//    public void doPrepareState(StateRecord sr) throws Exception {
//        PlToken token = parser.getLexer().readToken(true);
//        if (token.kind == TK_CONS) {
//            create(LIST.getRuleClass(),
//                    LIST,
//                    sr.getAssocs(),
//                    sr.getDks(),
//                    sr.getCurrPriority(),
//                    sr.getToken());
//        }
//        parser.setLastTerm(null);
//    }
//
//    @Override
//    public void doCompleteState(StateRecord sr) throws Exception {
//        if (parser.getLastTerm() == null) {
//            parser.setLastTerm(readVariable());
//        }
//    }

    /**
     * @return
     * @throws Exception
     */
    public HtVariable readVariable() throws Exception {
        PlToken token = parser.getLexer().readToken(true);
        if (token.kind == TK_VAR) {
            return appContext.getTermFactory().newVariable(token.image);
        }
        throw new ParserException("Expected variable");
    }
}
