package org.ltc.hitalk.entities;


import com.thesett.aima.logic.fol.FunctorName;

/**
 *
 */
@Deprecated
public
class HtDirective {
    /**
     *
     */
    public
    enum DirKind implements IKind {
        ALIAS_DIRECTIVE(),
        SYNCHRONIZED_DIRECTIVE,
        USES_DIRECTIVE,
        USE_MODULE_DIRECTIVE,
        SCOPE_DIRECTIVE,
        MODE_DIRECTIVE,
        META_PREDICATE_DIRECTIVE,
        META_NON_TERMINAL_DIRECTIVE,
        INFO_DIRECTIVE,
        DYNAMIC_DIRECTIVE,
        DISCONTIGUOUS_DIRECTIVE,
        MULTIFILE_DIRECTIVE,
        COINDUCTIVE_DIRECTIVE,
        OPERATOR_DIRECTIVE,
        ;

        private final DirSubkind[] subkinds;

        /**
         * @return
         */
        @Override
        public
        boolean isAbstract () {
            return false;
        }

        /**
         * @return
         */
        @Override
        public
        DirSubkind getParent () {
            return null;
        }

        public
        enum DirSubkind {
            ;

            DirSubkind ( FunctorName name ) {
                this.name = name;
            }

            private
            FunctorName name;
        }

        /**
         * @param subkinds
         */
        DirKind ( DirSubkind... subkinds ) {
            this.subkinds = subkinds;
        }
    }
}
