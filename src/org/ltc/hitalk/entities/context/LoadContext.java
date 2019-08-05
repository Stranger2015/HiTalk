package org.ltc.hitalk.entities.context;

import org.ltc.hitalk.compiler.bktables.HiTalkFlag;
import org.ltc.hitalk.entities.HtProperty;

/**
 *
 * Key	Description
 * ================
 *
 * directory 	    Directory in which source lives
 * dialect 	        Compatibility mode. See expects_dialect/1.
 * file 	        Similar to source, but returns the file being included when called while an include file is being processed
 * module 	        Module into which file is loaded
 * reload 	        true if the file is being reloaded. Not present on first load
 * script 	        Boolean that indicates whether the file is loaded as a script file (see -s)
 * source 	        File being loaded. If the system is processing an included file, the value is the main file. Returns the original Prolog file when loading a .qlf file.
 * stream 	        Stream identifier (see current_input/1)
 * term_position 	Start position of last term read. See also stream_property/2 (position property and stream_position_data/3.50
 * term 	        Term being expanded by expand_term/2.
 * variable_names	A list of `Name = Var' of the last term read. See read_term/2 for details.
 *
 */
public
class LoadContext extends Context {
    /**
     * @param flags
     */
    public
    LoadContext ( HiTalkFlag... flags ) {
        super(flags);

    }

    public
    LoadContext ( HtProperty... props ) {
        super(props);
    }

    /**
     * @return
     */
    @Override
    public
    HiTalkFlag[] getFlags () {
        return new HiTalkFlag[0];
    }

    public
    void setFlags ( HiTalkFlag[] flags ) {

    }
}
