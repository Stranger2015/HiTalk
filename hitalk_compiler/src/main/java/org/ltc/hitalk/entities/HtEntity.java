package org.ltc.hitalk.entities;

import org.ltc.hitalk.compiler.bktables.BkTable;
import org.ltc.hitalk.compiler.bktables.BkTableKind;
import org.ltc.hitalk.compiler.bktables.BookKeepingTables;
import org.ltc.hitalk.compiler.bktables.db.Record;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.ltc.hitalk.entities.HtRelationKind.LENGTH;

/**
 * object/1-5
 * Description
 * <p>
 * Stand-alone objects (prototypes)
 * <p>
 * object(Object)
 * <p>
 * object(Object,
 * implements(Protocols))
 * <p>
 * object(Object,
 * imports(Categories))
 * <p>
 * object(Object,
 * implements(Protocols),
 * imports(Categories))
 * <p>
 * Prototype extensions
 * <p>
 * object(Object,
 * extends(Objects))
 * <p>
 * object(Object,
 * implements(Protocols),
 * extends(Objects))
 * <p>
 * object(Object,
 * imports(Categories),
 * extends(Objects))
 * <p>
 * object(Object,
 * implements(Protocols),
 * imports(Categories),
 * extends(Objects))
 * <p>
 * Class instances
 * <p>
 * object(Object,
 * instantiates(Classes))
 * <p>
 * object(Object,
 * implements(Protocols),
 * instantiates(Classes))
 * <p>
 * object(Object,
 * imports(Categories),
 * instantiates(Classes))
 * <p>
 * object(Object,
 * implements(Protocols),
 * imports(Categories),
 * instantiates(Classes))
 * <p>
 * Classes
 * <p>
 * object(Object,
 * specializes(Classes))
 * <p>
 * object(Object,
 * implements(Protocols),
 * specializes(Classes))
 * <p>
 * object(Object,
 * imports(Categories),
 * specializes(Classes))
 * <p>
 * object(Object,
 * implements(Protocols),
 * imports(Categories),
 * specializes(Classes))
 * <p>
 * Classes with metaclasses
 * <p>
 * object(Object,
 * instantiates(Classes),
 * specializes(Classes))
 * <p>
 * object(Object,
 * implements(Protocols),
 * instantiates(Classes),
 * specializes(Classes))
 * <p>
 * object(Object,
 * imports(Categories),
 * instantiates(Classes),
 * specializes(Classes))
 * <p>
 * object(Object,
 * implements(Protocols),
 * imports(Categories),
 * instantiates(Classes),
 * specializes(Classes))
 * <p>
 * =============================================================================
 * <p>
 * protocol/1-2
 * Description
 * <p>
 * protocol(Protocol)
 * <p>
 * protocol(Protocol,
 * extends(Protocols))
 * <p>
 * Starting protocol directive.
 * Template and modes
 * <p>
 * protocol(+protocol_identifier)
 * <p>
 * protocol(+protocol_identifier,
 * extends(+extended_protocols))
 * <p>
 * =============================================================================
 * <p>
 * category/1-4
 * Description
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
class HtEntity extends PropertyOwner {
    /**
     *
     */
    private final List <IRelation> relations = new ArrayList <>();
    private int[][] relIndexes = new int[LENGTH][]; //indexes Relationkind.ord--> rel. example number --> relations

    /**
     *
     */
    protected final HtEntityIdentifier identifier;

    /**
     * @param kind
     */
    protected HtEntity ( IFunctor functor, HtEntityKind kind, HtProperty... props ) {
        super(props);

        this.identifier = new HtEntityIdentifier(functor, kind);
        for (int i = 0, relIndexesLength = relIndexes.length; i < relIndexesLength; i++) {
            relIndexes[i] = new int[0];
        }
    }

    /**
     * @return
     */
    public IFunctor getName () {
        return identifier;
    }

    /**
     * @param relation
     */
    public
    void add ( IRelation relation ) {
        if (relations.contains(relation)) {
            int idx = relations.size();
            relations.add(relation);
            HtRelationKind kind = relation.getRelationKind();
            int[] array = relIndexes[kind.ordinal()];
            relIndexes[kind.ordinal()] = Arrays.copyOf(array, array.length + 1);
            relIndexes[kind.ordinal()][array.length] = idx;
            BookKeepingTables <Record, BkTable <Record>> tables = new BookKeepingTables <>();
            BkTable <Record> table = tables.getTable(BkTableKind.ENTITY_RELATIONS);
        }
    }
}