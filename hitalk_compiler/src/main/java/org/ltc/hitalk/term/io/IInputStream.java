package org.ltc.hitalk.term.io;

import com.thesett.aima.logic.fol.Term;

import java.io.DataInput;
import java.nio.channels.ReadableByteChannel;

/**
 *
 */
public interface IInputStream extends DataInput, ReadableByteChannel {
    /**
     * @return
     */
    Term readTerm ();
}
