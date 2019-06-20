package org.ltc.hitalk.entities;

/**
 * inline,
 * auxiliary,
 * non_terminal_non_terminal_indicator,
 * include_atom,
 * line_count_integer,
 * // ----------------------------------------------------
 * number_of_clauses_integer,  !!!!!!!!!!!!!!
 * number_of_rules_integer,
 */
public
class HtPredicateAlias implements IPropertyOwner <HtPredicateIndicator> {
    private final static int PROPS_LENGTH = 5;
    private final HtProperty[] props;

    @Override
    public
    int getPropLength () {
        return PROPS_LENGTH;
    }

    /**
     * @return
     */
    @Override
    public
    HtPredicateIndicator[] getNames () {
        return new HtPredicateIndicator[0];
    }

    public
    HtPredicateAlias ( HtProperty[] props ) {

        this.props = props;
    }
}
