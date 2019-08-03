package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.Functor;

import static org.ltc.hitalk.entities.HtEntityKind.*;
import static org.ltc.hitalk.entities.HtPredicateDirective.DirKind;

public
class HtEntityDirective extends Clause <Functor>/* implements Hierarchy */ {
    private final Kind kind;
    private final HtPredicateDirective.DirKind dirKind;
    private final HtEntityKind entityKind;

    /**
     * Creates a program sentence in L2.
     *
     * @param body       The functors that make up the query body of the program, if any. May be <tt>null</tt>
     * @param dirKind
     * @param entityKind
     */
    public
    HtEntityDirective ( Functor[] body, Kind kind, DirKind dirKind, HtEntityKind entityKind ) {
        super(null, body);

        this.kind = kind;
        this.dirKind = dirKind;
        this.entityKind = entityKind;
    }

    public
    DirKind getKind () {
        return dirKind;
    }

    public
    HtEntityKind getEntityKind () {
        return entityKind;
    }

    enum Kind {
        BUILT_IN,
        DYNAMIC,
        INCLUDE, //                include(” source_file_name “),
        INFO, //         info(” entity_info_list “),
        INITIALIZATION(OBJECT),
        SET_LOGTALK_FLAG, //       set_logtalk_flag(” atom “,” nonvar “),
        THREADED(OBJECT),
        USES(OBJECT_OR_CATEGORY) //uses(” object_alias_list “),
        ;    //

        //******************* category directive ************

//                “:- built_in.” |
//                “:- dynamic.” |
//                “:- info(” entity_info_list “).” |
//                “:- set_logtalk_flag(” atom “,” nonvar “).” |
//                “:- include(” source_file_name “).” |
//                “:- uses(” object_alias_list “).” |

        //******************* protocol directive ************

//                “:- built_in.” |
//                “:- dynamic.” |
//                “:- info(” entity_info_list “).” |
//                “:- set_logtalk_flag(” atom “,” nonvar “).” |
//                “:- include(” source_file_name “).” |

        private final HtEntityKind entityKind;

        private
        Kind () {
            this(ENTITY);
        }

        private
        Kind ( HtEntityKind entityKind ) {
            this.entityKind = entityKind;
        }

        /**
         * @return
         */
        public
        HtEntityKind getEntityKind () {
            return entityKind;
        }
    }
}
