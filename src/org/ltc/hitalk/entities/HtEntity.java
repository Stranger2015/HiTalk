package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.Functor;
import org.ltc.hitalk.compiler.bktables.HiTalkFlag;
import org.ltc.hitalk.compiler.bktables.INameable;
import org.ltc.hitalk.entities.context.Context;

/**
 * object/1-5
 * Description
 *
 * Stand-alone objects (prototypes)
 *
 * object(Object)
 *
 * object(Object,
 *     implements(Protocols))
 *
 * object(Object,
 *     imports(Categories))
 *
 * object(Object,
 *     implements(Protocols),
 *     imports(Categories))
 *
 * Prototype extensions
 *
 * object(Object,
 *     extends(Objects))
 *
 * object(Object,
 *     implements(Protocols),
 *     extends(Objects))
 *
 * object(Object,
 *     imports(Categories),
 *     extends(Objects))
 *
 * object(Object,
 *     implements(Protocols),
 *     imports(Categories),
 *     extends(Objects))
 *
 * Class instances
 *
 * object(Object,
 *     instantiates(Classes))
 *
 * object(Object,
 *     implements(Protocols),
 *     instantiates(Classes))
 *
 * object(Object,
 *     imports(Categories),
 *     instantiates(Classes))
 *
 * object(Object,
 *     implements(Protocols),
 *     imports(Categories),
 *     instantiates(Classes))
 *
 * Classes
 *
 * object(Object,
 *     specializes(Classes))
 *
 * object(Object,
 *     implements(Protocols),
 *     specializes(Classes))
 *
 * object(Object,
 *     imports(Categories),
 *     specializes(Classes))
 *
 * object(Object,
 *     implements(Protocols),
 *     imports(Categories),
 *     specializes(Classes))
 *
 * Classes with metaclasses
 *
 * object(Object,
 *     instantiates(Classes),
 *     specializes(Classes))
 *
 * object(Object,
 *     implements(Protocols),
 *     instantiates(Classes),
 *     specializes(Classes))
 *
 * object(Object,
 *     imports(Categories),
 *     instantiates(Classes),
 *     specializes(Classes))
 *
 * object(Object,
 *     implements(Protocols),
 *     imports(Categories),
 *     instantiates(Classes),
 *     specializes(Classes))
 *
 *     =============================================================================
 *
 *     protocol/1-2
 *       Description
 *
 *     protocol(Protocol)
 *
 *     protocol(Protocol,
 *     extends(Protocols))
 *
 *      Starting protocol directive.
 *      Template and modes
 *
 * protocol(+protocol_identifier)
 *
 * protocol(+protocol_identifier,
 *     extends(+extended_protocols))
 *
 *     =============================================================================
 *
 *     category/1-4
 * Description
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
 *
 */
public
class HtEntity extends PropertyOwner implements INameable <Functor> {

    /**
     *
     */
    protected final HtEntityIdentifier identifier;

    /**
     *
     * @param kind
     */
    protected
    HtEntity ( Functor functor, HtEntityKind kind, HiTalkFlag... props ) {
        super(props);
        this.identifier = new HtEntityIdentifier(functor, kind);
    }

    /**
     * @return
     */
    @Override
    public
    Functor getName () {
        return identifier;
    }

    @Override
    public
    String get ( Context.Kind.Loading basename ) {
        return null;
    }

}