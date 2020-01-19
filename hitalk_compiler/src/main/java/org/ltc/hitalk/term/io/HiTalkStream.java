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
import java.io.FileNotFoundException;
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
 * <p>
 * True when StreamProperty is a property of Stream. If enumeration of streams or properties is demanded because either
 * Stream or StreamProperty are unbound, the implementation enumerates all candidate streams and properties while
 * locking the stream database.
 * Properties are fetched without locking the stream and may be outdated before this predicate returns due to
 * asynchronous activity.
 * <p>
 * alias(Atom)
 * If Atom is bound, test if the stream has the specified alias. Otherwise unify Atom with the first alias of
 * the stream.bug
 * <p>
 * buffer(Buffering)
 * SWI-Prolog extension to query the buffering mode of this stream. Buffering is one of full, line or false.
 * See also open/4.
 * <p>
 * buffer_size(Integer)
 * SWI-Prolog extension to query the size of the I/O buffer associated to a stream in bytes. Fails if the stream
 * is not buffered.
 * <p>
 * bom(Bool)
 * If present and true, a BOM (Byte Order Mark) was detected while opening the file for reading, or a BOM was
 * written while opening the stream. See section 2.20.1.1 for details.
 * <p>
 * close_on_abort(Bool)
 * Determine whether or not abort/0 closes the stream. By default streams are closed.
 * <p>
 * close_on_exec(Bool)
 * Determine whether or not the stream is closed when executing a new process (exec() in Unix, CreateProcess()
 * in Windows). Default is to close streams. This maps to fcntl() F_SETFD using the flag FD_CLOEXEC on Unix and
 * (negated) HANDLE_FLAG_INHERIT on Windows.
 * <p>
 * encoding(Encoding)
 * Query the encoding used for text. See section 2.20.1 for an overview of wide character and encoding issues in
 * SWI-Prolog.
 * <p>
 * end_of_stream(E)
 * If Stream is an input stream, unify E with one of the atoms not, at or past. See also at_end_of_stream/[0,1].
 * <p>
 * eof_action(A)
 * Unify A with one of eof_code, reset or error. See open/4 for details.
 * <p>
 * file_name(Atom)
 * If Stream is associated to a file, unify Atom to the name of this file.
 * <p>
 * file_no(Integer)
 * If the stream is associated with a POSIX file descriptor, unify Integer with the descriptor number.
 * SWI-Prolog extension used primarily for integration with foreign code. See also Sfileno() from SWI-Stream.h.
 * <p>
 * input
 * True if Stream has mode read.
 * <p>
 * locale(Locale)
 * True when Locale is the current locale associated with the stream. See section 4.23.
 * <p>
 * mode(IOMode)
 * Unify IOMode to the mode given to open/4 for opening the stream. Values are: read, write, append and
 * the SWI-Prolog extension update.
 * <p>
 * newline(NewlineMode)
 * One of posix or dos. If dos, text streams will emit \r\n for \n and discard \r from input streams.
 * Default depends on the operating system.
 * <p>
 * nlink(-Count)
 * Number of hard links to the file. This expresses the number of `names' the file has.
 * Not supported on all operating systems and the value might be bogus. See the documentation of fstat()
 * for your OS and the value st_nlink.
 * <p>
 * output
 * True if Stream has mode write, append or update.
 * <p>
 * position(Pos)
 * Unify Pos with the current stream position. A stream position is an opaque term whose fields can be extracted
 * using stream_position_data/3. See also set_stream_position/2.
 * <p>
 * reposition(Bool)
 * Unify Bool with true if the position of the stream can be set (see seek/4). It is assumed the position can be set
 * if the stream has a seek-function and is not based on a POSIX file descriptor that is not associated
 * to a regular file.
 * <p>
 * representation_errors(Mode)
 * Determines behaviour of character output if the stream cannot represent a character. For example, an ISO Latin-1
 * stream cannot represent Cyrillic characters. The behaviour is one of error (throw an I/O error exception),
 * prolog (write \...\ escape code) or xml (write &#...; XML character entity).
 * The initial mode is prolog for the user streams and error for all other streams.
 * See also section 2.20.1 and set_stream/2.
 * <p>
 * timeout(-Time)
 * Time is the timeout currently associated with the stream. See set_stream/2 with the same option.
 * If no timeout is specified, Time is unified to the atom infinite.
 * <p>
 * type(Type)
 * Unify Type with text or binary.
 * <p>
 * tty(Bool)
 * This property is reported with Bool equal to true if the stream is associated with a terminal.
 * See also set_stream/2.
 * <p>
 * write_errors(Atom)
 * Atom is one of error (default) or ignore. The latter is intended to deal with service processes for which the
 * standard output handles are not connected to valid streams. In these cases write errors may be ignored on
 * user_error.
 */
public abstract
class HiTalkStream implements IPropertyOwner, PropertyChangeListener, Cloneable, Closeable, IHitalkObject {
    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    public static final int BB_ALLOC_SIZE = 32768;

    protected FileDescriptor fd;
    protected FileChannel channel;

    protected EnumSet<StandardOpenOption> options = EnumSet.noneOf(StandardOpenOption.class);

    protected boolean isOpen;

    protected PropertyOwner<HtProperty> owner = new PropertyOwner<>(createProperties());

    protected HtProperty[] createProperties() {
        return new HtProperty[]{
                new HtProperty("alias", "Atom"),
//                SWI-Prolog extension to query the buffering mode of this stream. Buffering is one of full, line or false.
                new HtProperty("buffer", "Buffering"),
                new HtProperty("buffer_size", "Integer"),
                new HtProperty("bom", "Bool"),
                new HtProperty("close_on_abort", "Bool"),
                new HtProperty("close_on_exec", "Bool"),
                new HtProperty("encoding", "Encoding", "not", "at", "past"),
                new HtProperty("end_of_stream", "E"),
                new HtProperty("eof_action", "A"),

//                *     If Stream is an input stream, unify E with one of the atoms . See also at_end_of_stream/[0,1].
// *
// * eof_action(A)
//                *     Unify A with one of eof_code, reset or error. See open/4 for details.
//                *
// * file_name(Atom)
//                *     If Stream is associated to a file, unify Atom to the name of this file.
//                *
// * file_no(Integer)
//                *     If the stream is associated with a POSIX file descriptor, unify Integer with the descriptor number.
//                *     SWI-Prolog extension used primarily for integration with foreign code. See also Sfileno() from SWI-Stream.h.
//                *
// * input
//                *     True if Stream has mode read.
// *
// * locale(Locale)
//                *     True when Locale is the current locale associated with the stream. See section 4.23.
//                *
// * mode(IOMode)
//                *     Unify IOMode to the mode given to open/4 for opening the stream. Values are: read, write, append and
//                *     the SWI-Prolog extension update.
// *
// * newline(NewlineMode)
//                *     One of posix or dos. If dos, text streams will emit \r\n for \n and discard \r from input streams.
// *     Default depends on the operating system.
// *
// * nlink(-Count)
//                *     Number of hard links to the file. This expresses the number of `names' the file has.
//                *     Not supported on all operating systems and the value might be bogus. See the documentation of fstat()
//                *     for your OS and the value st_nlink.
// *
// * output
//                *     True if Stream has mode write, append or update.
// *
// * position(Pos)
//                *     Unify Pos with the current stream position. A stream position is an opaque term whose fields can be extracted
// *     using stream_position_data/3. See also set_stream_position/2.
//                *
// * reposition(Bool)
//                *     Unify Bool with true if the position of the stream can be set (see seek/4). It is assumed the position can be set
// *     if the stream has a seek-function and is not based on a POSIX file descriptor that is not associated
//                *     to a regular file.
//                *
// * representation_errors(Mode)
//                *     Determines behaviour of character output if the stream cannot represent a character. For example, an ISO Latin-1
//                *     stream cannot represent Cyrillic characters. The behaviour is one of error (throw an I/O error exception),
// *     prolog (write \...\ escape code) or xml (write &#...; XML character entity).
// *     The initial mode is prolog for the user streams and error for all other streams.
//                *     See also section 2.20.1 and set_stream/2.
//                *
// * timeout(-Time)
//                *     Time is the timeout currently associated with the stream. See set_stream/2 with the same option.
// *     If no timeout is specified, Time is unified to the atom infinite.
// *
// * type(Type)
//                *     Unify Type with text or binary.
//                *
// * tty(Bool)
//                *     This property is reported with Bool equal to true if the stream is associated with a terminal.
//                *     See also set_stream/2.
//                *
// * write_errors(Atom)
//                *     Atom is one of error (default) or ignore. The latter is intended to deal with service processes for which the
// *     standard output handles are not connected to valid streams. In these cases write errors may be ignored on
//                *     user_error.
        };
    }

    protected Charset currentCharset = defaultCharset();
    protected StreamDecoder sd;
    protected Path path;

    public final void open() throws FileNotFoundException {
        if (!isOpen) {
            doOpen();
        }
    }

    abstract protected void doOpen() throws FileNotFoundException;//{    }

    /**
     * @param path
     * @param encoding
     * @param options
     */
    protected HiTalkStream(Path path, String encoding, StandardOpenOption... options) {
        this.path = path;
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
        if (isOpen) {
            isOpen = false;
            channel.close();
        }
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
//        sb.append(", channel=").append(channel);
        sb.append(", currentCharset=").append(currentCharset);
        sb.append(", sd=").append(sd);
    }

    public boolean isOpen () {
        return isOpen;
    }
}