package org.ltc.hitalk.parser.handlers;

import org.ltc.hitalk.parser.ParserState;
import org.ltc.hitalk.parser.PlToken;
import org.ltc.hitalk.parser.StateRecord;
import org.ltc.hitalk.term.ListTerm;

import java.util.EnumSet;

import static org.ltc.hitalk.parser.Directive.DirectiveKind;
import static org.ltc.hitalk.parser.ParserState.HEADS;
import static org.ltc.hitalk.parser.ParserState.TAIL;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity;

/**
 *
 */
@Deprecated
public class SequenceHandler extends ParserStateHandler {
    protected ListTerm listTerm = new ListTerm(0);
    /**
     * @param state
     * @param assocs
     * @param dks
     * @param currPriority
     * @param token
     */
    public SequenceHandler(
            ParserState state,
            EnumSet<Associativity> assocs,
            EnumSet<DirectiveKind> dks,
            int currPriority,
            PlToken token) throws Exception {
        super(state, assocs, dks, currPriority, token);
    }

    public void doPrepareState(StateRecord sr) throws Exception {
        parser.setLastTerm(null);
        create(HEADS.getHandlerClass(),
                HEADS,
                sr.getAssocs(),
                sr.getDks(),
                sr.getCurrPriority(),
                sr.getToken()
        );

        create(TAIL.getRuleClass(),
                TAIL,
                sr.getAssocs(),
                sr.getDks(),
                sr.getCurrPriority(),
                sr.getToken()
        );
    }

    public void doCompleteState(StateRecord sr) throws Exception {
        if (parser.getLastTerm() != null) {
            listTerm.addHead(parser.getLastTerm());
        }
//        create(TAIL.getHandlerClass(),
//                    TAIL,
//                    sr.getAssocs(),
//                    sr.getDks(),
//                    sr.getCurrPriority(),
//                    sr.getToken()
//            );
    }
}