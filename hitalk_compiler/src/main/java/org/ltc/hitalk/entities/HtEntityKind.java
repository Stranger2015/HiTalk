package org.ltc.hitalk.entities;

/**
 * category/1-4
 * <p>
 * category(Category)
 * <p>
 * category(Category,
 * implements(Protocols))
 * <p>
 * category(Category,
 * extends(Categories))
 * <p>
 * category(Category,
 * complements(Objects))
 * <p>
 * category(Category,
 * implements(Protocols),
 * extends(Categories))
 * <p>
 * category(Category,
 * implements(Protocols),
 * complements(Objects))
 * <p>
 * category(Category,
 * extends(Categories),
 * complements(Objects))
 * <p>
 * category(Category,
 * implements(Protocols),
 * extends(Categories),
 * complements(Objects))
 * <p>
 * Starting category directive.
 * Template and modes
 * <p>
 * category(+category_identifier)
 * <p>
 * category(+category_identifier,
 * implements(+implemented_protocols))
 * <p>
 * category(+category_identifier,
 * extends(+extended_categories))
 * <p>
 * category(+category_identifier,
 * complements(+complemented_objects))
 * <p>
 * category(+category_identifier,
 * implements(+implemented_protocols),
 * extends(+extended_categories))
 * <p>
 * category(+category_identifier,
 * implements(+implemented_protocols),
 * complements(+complemented_objects))
 * <p>
 * category(+category_identifier,
 * extends(+extended_categories),
 * complements(+complemented_objects))
 * <p>
 * category(+category_identifier,
 * implements(+implemented_protocols),
 * extends(+extended_categories),
 * complements(+complemented_objects))
 * <p>
 * Examples
 * <p>
 * :- category(monitoring).
 * <p>
 * :- category(monitoring,
 * implements(monitoringp)).
 * <p>
 * :- category(attributes,
 * implements(protected::variables)).
 * <p>
 * :- category(extended,
 * extends(minimal)).
 * <p>
 * :- category(logging,
 * implements(monitoring),
 * complements(employee)).
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

    private final HtEntityKind parent;
    private final boolean isAbstract;

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
