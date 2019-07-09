package org.ltc.hitalk.entities;

import org.ltc.hitalk.compiler.bktables.HiTalkFlag;
import org.ltc.hitalk.entities.context.Context;

/**
 * inline,
 * auxiliary,
 * non_terminal_non_terminal_indicator,
 * include_atom,
 * line_count_integer,
 * //
 * number_of_clauses_integer,  !!!!!!!!!!!!!!
 * number_of_rules_integer,
 */
public
class HtPredicateAlias extends PropertyOwner {
    //  private final static int PROPS_LENGTH = 5;

    /**
     * @param props
     */
    public
    HtPredicateAlias ( HiTalkFlag... props ) {
        super(props);
    }

    @Override
    public
    String get ( Context.Kind.Loading basename ) {
        return null;
    }
}
