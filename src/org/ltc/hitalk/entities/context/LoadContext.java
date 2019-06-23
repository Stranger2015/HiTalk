package org.ltc.hitalk.entities.context;

import com.thesett.aima.logic.fol.Functor;
import org.ltc.hitalk.entities.IPropertyOwner;

/**
 * logtalk_load_context/2
 * Description
 * <p>
 * logtalk_load_context(Key, Value)
 * <p>
 * Provides access to the Logtalk compilation/loading context. The following keys are currently supported:
 * <p>
 * entity_identifier - identifier of the entity being compiled if any
 * <p>
 * entity_prefix - internal prefix for the entity compiled code
 * <p>
 * entity_type - returns the value module when compiling a module as an object
 * <p>
 * source - full path of the source file being compiled
 * <p>
 * file - the actual file being compiled, different from source only when processing an include/1 directive
 * <p>
 * basename - source file basename
 * <p>
 * directory - source file directory
 * <p>
 * stream - input stream being used to read source file terms
 * <p>
 * target - the full path of the intermediate Prolog file
 * <p>
 * flags - the list of the explicit flags used for the compilation of the source file
 * <p>
 * term - the source file term being compiled
 * <p>
 * term_position - the position of the term being compiled (StartLine-EndLine)
 * <p>
 * variable_names - the variable names of the term being compiled ([Name1=Variable1, ...])
 */
public
class LoadContext implements IPropertyOwner <Functor> {

    @Override
    public
    int getPropLength () {
        return 12;
    }
}
