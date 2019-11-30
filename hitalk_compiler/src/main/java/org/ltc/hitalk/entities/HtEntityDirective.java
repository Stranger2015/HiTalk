package org.ltc.hitalk.entities;


import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.ListTerm;

import static org.ltc.hitalk.entities.HtEntityKind.*;

public
class HtEntityDirective extends HtClause/* implements Hierarchy */ {
    private final Kind kind;
    //    private final HtPredicateDirective.DirKind dirKind;
    private final HtEntityKind entityKind;
    private HtDirective.DirKind dirKind;

    /**
     * Creates a program sentence in L2.
     *
     * @param body       The functors that make up the query body of the program, if any. May be <tt>null</tt>
     * @param entityKind
     */
    public HtEntityDirective ( ListTerm body, Kind kind, /*HtDirective.DirKind dirKind,*/ HtEntityKind entityKind ) {
        super(null, body);

        this.kind = kind;
//        this.dirKind = dirKind;
        this.entityKind = entityKind;
    }

    public
    HtDirective.DirKind getKind () {
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
