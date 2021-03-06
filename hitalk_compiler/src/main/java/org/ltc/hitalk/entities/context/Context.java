package org.ltc.hitalk.entities.context;

import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.entities.PropertyOwner;

/**
 *
 */
public abstract
class Context extends PropertyOwner {
    private HtProperty[] props;

    public
    Context ( HtProperty... props ) {
        super(props);
    }

    /**
     *
     */
    public
    void reset () {

    }

    public
    void setProps ( HtProperty... props ) {
        this.props = props;
    }

    public
    HtProperty[] getProps () {
        return props;
    }

    public
    enum Kind {
        /**
         * CompCtx(Ctx, Head, _, Entity, Sender, This, Self, Prefix, MetaVars, MetaCallCtx, ExCtx, _, Stack, Lines),
         */
        COMPILATION,
        /**
         * executionContext(ExCtx, user, user, user, HookEntity, [], []),
         */
        EXECUTION,
        /**
         *
         */
        LOADING,
        ;

        public
        enum CompilationContext {
            COINDUCTION_STACK,
            CTX, //this context
            CTX1,
            CTX2,
            ENTITY_IDENTIFIER,
            ENTITY_PREFIX,
            FOO,
            LINES,
            METACALL_CONTEXT,
            METAVARS,
            RUNTIME,
            SELF,
            SENDER,
            THIS,
        }

        public
        enum Loading {
            BASENAME,
            DIRECTORY,
            ENTITY_IDENTIFIER,
            ENTITY_PREFIX,
            ENTITY_TYPE,
            FILE,
            FLAGS,
            SOURCE,
            STREAM,
            TARGET,
            TERM,
            TERM_POSITION,
            VARIABLE_NAMES,
        }

        /**
         *
         */
        public
        enum ExecutionCtx {
            COINDUCTION_STACK,
            //  CONTEXT_ID,
            ENTITY,
            METACALL_CONTEXT,
            SELF,
            SENDER,
            THIS,
        }
    }
}
