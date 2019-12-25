package org.ltc.hitalk.term.io;

import org.ltc.hitalk.core.IHitalkObject;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.entities.IProperty;
import org.ltc.hitalk.entities.IPropertyOwner;
import org.ltc.hitalk.entities.PropertyOwner;
import org.ltc.hitalk.term.ITerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.nio.cs.StreamDecoder;

import java.beans.PropertyChangeEvent;
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
import java.util.Properties;

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


    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    public static final int BB_ALLOC_SIZE = 32768;

    protected FileDescriptor fd;
    protected FileChannel channel;

    protected EnumSet <StandardOpenOption> options = EnumSet.noneOf(StandardOpenOption.class);

    protected boolean isOpen;

    protected PropertyOwner <HtProperty> owner;
    protected Charset currentCharset = defaultCharset();
    protected StreamDecoder sd;
//    private PlTokenSource tokenSource;

    /**
     * @param path
     * @param encoding
     * @param options
     * @throws IOException
     */
    protected HiTalkStream ( Path path, String encoding, StandardOpenOption... options ) throws IOException {

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
        this.fd = fd;
        init(fd);
    }

    protected HiTalkStream () {
    }

    /**
     * @param fd
     * @throws IOException
     */
    protected abstract void init ( FileDescriptor fd ) throws IOException;

    /**
     * This method gets called when a bound property is changed.
     *
     * @param evt A PropertyChangeEvent object describing the event source
     *            and the property that has changed.
     */
    @Override
    public void propertyChange ( PropertyChangeEvent evt ) {

    }

    /**
     * Closes this stream and releases any system resources associated
     * with it. If the stream is already closed then invoking this
     * method has no effect.
     *
     * <p> As noted in {@link AutoCloseable#close()}, cases where the
     * close may fail require careful attention. It is strongly advised
     * to relinquish the underlying resources and to internally
     * <em>mark</em> the {@code Closeable} as closed, prior to throwing
     * the {@code IOException}.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close () throws IOException {

    }

    /**
     * @return
     */
    @Override
    public HtProperty[] getFlags () {
        return new HtProperty[0];
    }

    /**
     * @return
     */
    @Override
    public int getPropLength () {
        return 0;
    }

    @Override
    public void addListener ( PropertyChangeListener listener ) {

    }

    @Override
    public void removeListener ( PropertyChangeListener listener ) {

    }

    @Override
    public void fireEvent ( IProperty property, ITerm value ) {

    }

    @Override
    public ITerm getValue ( Properties property ) {
        return null;
    }

    /**
     * @param property
     * @param value
     */
    @Override
    public void setValue ( Properties property, ITerm value ) {

    }

    public final String toString () {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append('{');
        toString0(sb);
        sb.append('}');
        return sb.toString();
    }

    /**
     * @param sb
     */
    public void toString0 ( StringBuilder sb ) {
        sb.append(", fd=").append(fd);
        sb.append(", channel=").append(channel);
        sb.append(", currentCharset=").append(currentCharset);
        sb.append(", sd=").append(sd);
    }
}