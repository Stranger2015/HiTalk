package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.FunctorName;
import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.IPropertyOwner;
import org.ltc.hitalk.wam.compiler.HtProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * ENTITY PROPERTIES
 * ==================
 *
 * entity
 * common subset of properties
 *
 * static
 * dynamic
 * built_in
 * “file(” atom “)”
 * “file(” atom “,” atom “)”
 *  lines( integer, integer)
 *  events
 * source_data
 * “declares(” predicate_indicator “,” predicate_declaration_property_list “)”
 * “public(” predicate_indicator_list)
 * “protected(” predicate_indicator_list “)”
 * “private(” predicate_indicator_list “)”
 *  debugging
 *
 *
 *===================================================
 * ctgObjBase
 *
 * “calls(” predicate “,” predicate_call_update_property_list “)”
 * “updates(” predicate “,” predicate_call_update_property_list “)”
 *
 * “number_of_clauses(” integer “)” |
 * "number_of_rules(” integer “)” |
 * “number_of_user_clauses(” integer “)”
 * “number_of_user_rules(” integer “)” |
 *
 *defines(” predicate_indicator “,” predicate_definition_property_list “)” |
 *includes(” predicate_indicator “,” object_identifier | category_identifier “,” predicate_definition_property_list “)” |
 *provides(” predicate_indicator “,” object_identifier | category_identifier “,” predicate_definition_property_list “)” |
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *===================================================
 * category_property
 * ::=
 *
 *
 *
 *
 *
 *
 * =================================================
 * object_property
 * ::=
 *
 * “threaded”
 * “context_switching_calls” |
 * “dynamic_declarations” |
 * “complements(” “allow” | “restrict” “)” |
 * “complements” |
 * “defines(” predicate_indicator “,” predicate_definition_property_list “)” |
 * “includes(” predicate_indicator “,” object_identifier | category_identifier “,” predicate_definition_property_list “)” |
 * “provides(” predicate_indicator “,” object_identifier | category_identifier “,” predicate_definition_property_list “)”
 * “calls(” predicate “,” predicate_call_update_property_list “)” |
 * “updates(” predicate “,” predicate_call_update_property_list “)” |
 *
 * “module”
 *
 * =========================================================
 * protocol_property
 * ::=
 *
 * “alias(” predicate_indicator “,” predicate_alias_property_list “)” |
 *
 *
 * static
 *     The protocol is static
 * dynamic
 *     The protocol is dynamic (and thus can be abolished in runtime by calling the abolish_category/1 built-in predicate)
 * built_in
 *     The protocol is a built-in protocol (and thus always available)
 * source_data
 *     Source data available for the protocol
 * file(Path)
 *     Absolute path of the source file defining the protocol (if applicable)
 * file(Basename, Directory)
 *     Basename and directory of the source file defining the protocol (if applicable)
 * lines(BeginLine, EndLine)
 *     Source file begin and end lines of the protocol definition (if applicable)
 * public(Predicates)
 *     List of public predicates declared by the protocol
 * protected(Predicates)
 *     List of protected predicates declared by the protocol
 * private(Predicates)
 *     List of private predicates declared by the protocol
 * declares(Predicate, Properties)
 *     List of properties for a predicate declared by the protocol
 * alias(Predicate, Properties)
 *
 *################################################################
 * Predicate DECL props
 *
 *
 “static” | “dynamic” |
 “scope(” scope “)” |
 “private” | “protected” | “public” |
 “coinductive” |
 “multifile” |
 “synchronized” |
 “meta_predicate(” meta_predicate_template “)” |
 “coinductive(” coinductive_predicate_template “)” |
 “non_terminal(” non_terminal_indicator “)” |
 “include(” atom “)” |
 “line_count(” integer “)” |
 “mode(” predicate_mode_term | non_terminal_mode_term “,” number_of_proofs “)” |
 “info(” list “)”
 *
 *
 * ======================================================================
 *
 *
 predicate_definition_property

 ::=

 “inline” |
 “auxiliary” |
 “non_terminal(” non_terminal_indicator “)” |
 “include(” atom “)” |
 “line_count(” integer “)” |
 “number_of_clauses(” integer “)” |
 “number_of_rules(” integer “)”

 //=========================================================

 predicate_call_update_property ::=


 “caller(” predicate_indicator “)” |
 “include(” atom “)” |
 “line_count(” integer “)” |
 “as(” predicate_indicator “)”
 *
 *
 */
public
class HtEntity implements IPropertyOwner {

    EnumSet <EntityType> entityTypes = EnumSet.noneOf(EntityType.class);

    /**
     *
     */
    enum EntityType {
        OBJECT,
        PROTOCOL,
        CATEGORY,
        MODULE
    }

    /**
     *
     */
    protected final List <HtProperty> properties = new ArrayList <>();

    /**
     *
     */
    protected final FunctorName name;
    /**
     *
     */
    protected final EntityType type;

    /**
     * @param name
     * @param type
     */
    protected
    HtEntity ( FunctorName name, EntityType type ) {
        this.name = name;
        this.type = type;
        initProperties();
    }

    /**
     *
     */
    private
    void initProperties () {

    }

    /**
     * @return
     */
    public final
    FunctorName getName () {
        return name;
    }


    @Override
    public final
    String toString () {
        return getClass().getSimpleName() + "{" + "name=" + name + '}';
    }

    @Override
    public
    List <HtProperty> getProperties () {
        return properties;
    }

    @Override
    public
    List getNames () {
        return null;
    }

    @Override
    public
    List <Term> getValues () {
        return null;
    }

    @Override
    public
    List <EntityType> getTypes () {
        return Arrays.asList(EntityType.values());//y
    }
}
