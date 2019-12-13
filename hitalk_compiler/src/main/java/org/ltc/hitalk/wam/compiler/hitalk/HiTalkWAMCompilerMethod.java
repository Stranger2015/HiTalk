package org.ltc.hitalk.wam.compiler.hitalk;

import org.ltc.hitalk.entities.HtEntityIdentifier;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.compiler.IFunctor;

/**
 *
 */
public class HiTalkWAMCompilerMethod extends HiTalkWAMCompiledClause {
    private final HtEntityIdentifier identifier;

    /**
     * add dotted pair
     *
     * @param head
     * @param body
     * @param parent
     */
    public HiTalkWAMCompilerMethod ( HtEntityIdentifier identifier,
                                     IFunctor head,
                                     ListTerm body,
                                     HiTalkWAMCompiledPredicate parent ) {
        super(head, body, parent);
        this.identifier = identifier;
    }
}
