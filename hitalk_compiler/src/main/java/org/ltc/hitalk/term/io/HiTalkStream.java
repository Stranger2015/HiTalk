package org.ltc.hitalk.term.io;

import org.ltc.hitalk.core.IHitalkObject;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.entities.IPropertyOwner;
import org.ltc.hitalk.entities.PropertyOwner;
import sun.nio.cs.StreamDecoder;

import java.beans.PropertyChangeListener;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;

import static java.nio.charset.Charset.*;

/**
 * open(+SrcDest, +Mode, --Stream)
 * ===============================
 * //convert byte buffer in given charset to char buffer in unicode
 * ByteBuffer bb = ByteBuffer.wrap(s.getBytes());
 * CharBuffer cb = charset.decode(bb);
 * //convert char buffer in unicode to byte buffer in given charset
 * ByteBuffer newbb = charset.encode(cb);
 */
public abstract
class HiTalkStream
        implements IPropertyOwner, PropertyChangeListener, Cloneable, Closeable, IHitalkObject {

    public static final int BB_ALLOC_SIZE = 32768;

    protected FileDescriptor fd;
    protected FileChannel channel;

    protected EnumSet <StandardOpenOption> options = EnumSet.noneOf(StandardOpenOption.class);

    protected boolean isOpen;
//    protected boolean isReading;


    protected PropertyOwner <HtProperty> owner;
    protected Charset currentCharset = defaultCharset();
    protected StreamDecoder sd;

    /**
     * @param path
     * @param encoding
     * @param options
     * @throws IOException
     */
    protected HiTalkStream ( Path path, String encoding, StandardOpenOption... options ) throws IOException {
//        super();
//        this.options.addAll(Arrays.asList(options));
//        if (this.options.contains(READ)) {
//            setInputStream(new FileInputStream(path.toFile()));
//        }
//        if (this.options.contains(WRITE)) {
//            setOutputStream(new FileOutputStream(path.toFile()));
//        }
        Charset charset = isSupported(encoding) ? forName(encoding) : defaultCharset();//currentCharset;
        CharsetDecoder decoder = charset.newDecoder();
        sd = StreamDecoder.forDecoder(channel, decoder, BB_ALLOC_SIZE);
    }

    /**
     * @param path
     * @param options
     * @throws IOException
     */
    protected HiTalkStream ( Path path, StandardOpenOption... options ) throws IOException {
        this(path, defaultCharset().name(), options);
    }

    /**
     * @param fd
     * @throws IOException
     */
    public HiTalkStream ( FileDescriptor fd ) throws IOException {
//        this.lock = this;
        this.fd = fd;
//        options.add(isReading ? READ : WRITE);
        init(fd);
    }

    protected HiTalkStream () {
    }


    /**
     * @param fd
     * @throws IOException
     */
    protected abstract void init ( FileDescriptor fd ) throws IOException;
}