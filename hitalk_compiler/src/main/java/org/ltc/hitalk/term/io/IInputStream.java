package org.ltc.hitalk.term.io;

import java.io.DataInput;
import java.nio.channels.ReadableByteChannel;

/**
 *
 */
public interface IInputStream extends DataInput, ReadableByteChannel {
    HiTalkStream copy () throws CloneNotSupportedException;
}
