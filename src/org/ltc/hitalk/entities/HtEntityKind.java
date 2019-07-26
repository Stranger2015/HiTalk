package org.ltc.hitalk.entities;

/**
 *  category/1-4
 *
 * category(Category)
 *
 * category(Category,
 *     implements(Protocols))
 *
 * category(Category,
 *     extends(Categories))
 *
 * category(Category,
 *     complements(Objects))
 *
 * category(Category,
 *     implements(Protocols),
 *     extends(Categories))
 *
 * category(Category,
 *     implements(Protocols),
 *     complements(Objects))
 *
 * category(Category,
 *     extends(Categories),
 *     complements(Objects))
 *
 * category(Category,
 *     implements(Protocols),
 *     extends(Categories),
 *     complements(Objects))
 *
 * Starting category directive.
 * Template and modes
 *
 * category(+category_identifier)
 *
 * category(+category_identifier,
 *     implements(+implemented_protocols))
 *
 * category(+category_identifier,
 *     extends(+extended_categories))
 *
 * category(+category_identifier,
 *     complements(+complemented_objects))
 *
 * category(+category_identifier,
 *     implements(+implemented_protocols),
 *     extends(+extended_categories))
 *
 * category(+category_identifier,
 *     implements(+implemented_protocols),
 *     complements(+complemented_objects))
 *
 * category(+category_identifier,
 *     extends(+extended_categories),
 *     complements(+complemented_objects))
 *
 * category(+category_identifier,
 *     implements(+implemented_protocols),
 *     extends(+extended_categories),
 *     complements(+complemented_objects))
 *
 * Examples
 *
 * :- category(monitoring).
 *
 * :- category(monitoring,
 *     implements(monitoringp)).
 *
 * :- category(attributes,
 *     implements(protected::variables)).
 *
 * :- category(extended,
 *     extends(minimal)).
 *
 * :- category(logging,
 *     implements(monitoring),
 *     complements(employee)).
 */
public
enum HtEntityKind {
    ENTITY(true),//
    OBJECT_OR_CATEGORY(true),
    OBJECT(OBJECT_OR_CATEGORY, false),
    CATEGORY(OBJECT_OR_CATEGORY, false),
    PROTOCOL(ENTITY, false),
    MODULE(ENTITY, false),
    ;

    //    private final Object[] names;
//
    private final HtEntityKind parent;

    private final boolean isAbstract;

//    private final int propsLength;

//    Hierarchy hierarchy = new Hierarchy( Class<HtEntityProperty> ENTITYROPS

    /**
     * @param isAbstract
     */
    private
    HtEntityKind ( boolean isAbstract ) {
        this(null, isAbstract);
    }

    /**
     * @param parent
     * @param isAbstract
     */
    private
    HtEntityKind ( HtEntityKind parent, boolean isAbstract ) {
        this.parent = parent;
        this.isAbstract = isAbstract;
    }

    /**
     * @return
     */
    public
    HtEntityKind getParent () {
        return parent;
    }

    /**
     * @return
     */
    public
    boolean isAbstract () {
        return isAbstract;
    }
}
