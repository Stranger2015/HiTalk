package org.ltc.hitalk.term.io;

import com.thesett.aima.logic.fol.Term;

import java.io.DataOutput;
import java.nio.channels.WritableByteChannel;

public interface IOutputStream extends DataOutput, WritableByteChannel {
    void writeTerm ( Term term );
}
