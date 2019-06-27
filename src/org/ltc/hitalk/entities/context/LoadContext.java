package org.ltc.hitalk.entities.context;

import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.entities.PropertyOwner;

/**
 * logtalk_load_context/2
 * Description
 * <p>
 * logtalk_load_context(Key, Value)
 * <p>
 * Provides access to the Logtalk compilation/loading context. The following keys are currently supported:
 * <p>
 * entity_identifier    - identifier of the entity being compiled if any<p>
 * entity_prefix        - internal prefix for the entity compiled code
 * entity_type          - returns the value module when compiling a module as an objec
 * source               - full path of the source file being compile
 * file                 - the actual file being compiled, different from source only when processing an include/1 directiv
 * basename             - source file basename
 * directory            - source file directory
 * stream               - input stream being used to read source file terms
 * target               - the full path of the intermediate Prolog file
 * flags                - the list of the explicit flags used for the compilation of the source file
 * term                 - the source file term being compiled
 * term_position        - the position of the term being compiled (StartLine-EndLine)
 * variable_names       - the variable names of the term being compiled ([Name1=Variable1, ...])
 */
@Deprecated
public
class LoadContext extends PropertyOwner {

    public
    LoadContext ( HtProperty... props ) {
        super(props);
    }
}
