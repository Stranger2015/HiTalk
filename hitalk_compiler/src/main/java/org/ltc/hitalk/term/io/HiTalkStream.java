package org.ltc.hitalk.term.io;

import org.ltc.hitalk.core.IHitalkObject;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.entities.IProperty;
import org.ltc.hitalk.entities.IPropertyOwner;
import org.ltc.hitalk.entities.PropertyOwner;
import org.ltc.hitalk.term.HtNonVar;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.compiler.IFunctor;
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
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Predicate;

import static java.nio.charset.Charset.*;
import static org.ltc.hitalk.core.BaseApp.appContext;

/**
 * open(+SrcDest, +Mode, --Stream)
 * ===============================
 * //convert byte buffer in given charset to char buffer in unicode
 * ByteBuffer bb = ByteBuffer.wrap(s.getBytes());
 * CharBuffer cb = charset.decode(bb);
 * //convert char buffer in unicode to byte buffer in given charset
 * ByteBuffer newbb = charset.encode(cb);
 * <p>
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
public abstract /*abstract*/
class HiTalkStream extends PropertyOwner implements PropertyChangeListener, Cloneable, Closeable, IHitalkObject {
    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    public static final int BB_ALLOC_SIZE = 32768;

    protected FileDescriptor fd;
    protected FileChannel channel;

    protected EnumSet<StandardOpenOption> options = EnumSet.noneOf(StandardOpenOption.class);

    protected boolean isOpen;

    protected IPropertyOwner owner = this;
    private List<PropertyChangeListener> listeners = new ArrayList<>();

    //    private String methodName;
//    private Predicate<IFunctor> body;
//    private int arity;
//
    public HiTalkStream(HtProperty... properties) {
        super(properties);
    }

    /**
     * @param methods
     * @param props
     */
    public HiTalkStream(HtMethodDef[] methods, HtProperty[] props) {
        super(methods, props);
    }

    /**
     * ISO Input and Output Streams
     * open/4
     * open/3
     * open_null_stream/1
     * close/1
     * close/2
     * stream_property/2
     * current_stream/3
     * is_stream/1
     * stream_pair/3
     * set_stream_position/2
     * stream_position_data/3
     * seek/4
     * set_stream/2
     * set_prolog_IO/3
     *
     * @return
     */
    protected IPropertyOwner createPropertyOwner() {
        HtProperty[] props = new HtProperty[]{
                createProperty("alias(Atom)", ""),
//                SWI-Prolog extension to query the buffering mode of this stream.
//                Buffering is one of full, line or false.
                createProperty("buffer(Buffering).", "full", "line", "false"),
                createProperty("buffer_size(Integer)", ""),
                createProperty("bom(Bool)", "\ufeff"),//utf1i6
                createProperty("close_on_abort(Bool)", ""),
                createProperty("close_on_exec(Bool)", ""),
                createProperty("encoding(Encoding)",
                        "octet",
                        "octet", //Default encoding for binary streams.
                        // This causes the stream to be read and written fully untranslated.
                        "ascii",//7-bit encoding in 8-bit bytes. //
                        // Equivalent to iso_latin_1, but generates errors and warnings on encountering values above 127.
                        "text",/* mbrtowc()*/
                        "utf8",
//          Multi-byte encoding of full UCS, compatible with ascii. See above.
                        "unicode_be", // Unicode Big Endian. Reads input in pairs of bytes, most significant byte first.
                        // Can only represent 16-bit characters.
                        "unicode_le" //Unicode Little Endian. Reads input in pairs of bytes, least significant byte first.
                        // Can only represent 16-bit characters.
                ),

//                Note that not all encodings can represent all characters.
//                This implies that writing text to a stream may cause errors because the stream cannot represent these
//                characters.
//                The behaviour of a stream on these errors can be controlled using set_stream/2.
//                Initially the terminal stream writes the characters using Prolog escape sequences while other streams
//                generate an I/O exception.
//        };
//        C library default locale encoding for text files. Files are read and written using the C library functions  and wcrtomb().
//        This may be the same as one of the other locales, notably it may be the same as iso_latin_1 for Western
//        languages and utf8 in a UTF-8 context.
                createProperty("end_of_stream(E)", "not", "not", "at", "past"),
                createProperty("eof_action(A)", "eof_code", "eof_code", "reset", "error"),
                createProperty("file_name(Atom)", ""),
                createProperty("file_no(Integer)", ""),
                createProperty("input(Bool)", ""),
                createProperty("locale(Locale)", ""),
                createProperty("mode(IOMode)", "", "read", "write", "append", "update"),
                createProperty("newline(NewlineMode)", "", "posix", "dos"),//If dos, text streams will emit \r\n for \n and discard \r from input streams.
                createProperty("nlink(-Count)", ""),
                createProperty("output(Bool)", "Bool"),//True if Stream has mode write, append or update.
                createProperty("position(Pos)", "Pos"),// Unify Pos with the current stream position. A stream position is an opaque term whose fields can be extracted using stream_position_data/3. See also set_stream_position/2.
                createProperty("reposition(Bool)", "Bool"),//Unify Bool with true if the position of the stream can be set (see seek/4). It is assumed the position can be set
//if the stream has a seek-function and is not based on a POSIX file descriptor that is not associated to a regular file.
                createProperty("representation_errors(Mode)", "",
                        "prolog", "error"),//Determines behaviour of character output if the stream cannot
                // represent a character.
                // For example, an ISO Latin-1 stream cannot represent Cyrillic characters. The behaviour is one of
                // error (throw an I/O error exception),
//prolog (write \...\ escape code) or xml (write &#...; XML character entity).
//The initial mode is prolog for the user streams and error for all other streams.
//See also section 2.20.1 and set_stream/2.
                createProperty("timeout(-Time)", ""),//Time is the timeout currently associated with the stream.
                // See set_stream/2 with the same option.

//If no timeout is specified, Time is unified to the atom infinite.
                createProperty("type(Type)", "",
                        "text",
                        "binary"),//Unify Bool with true if the position of the stream can be set (see seek/4).
                // It is assumed the position can be set
                createProperty("tty(Bool)", "Bool"),// This property is reported with Bool equal to true if the stream is associated with a terminal.
                createProperty("write_errors(Errors)", "error ", "error", "ignored")};// Atom is one of error (default) or ignore. The latter is intended to deal with service processes for which the
//standard output handles are not connected to valid streams. In these cases write errors may be ignored on
//user_error. See also set_stream/2.
        final Map<String, HtProperty> map = new HashMap<>();

//        final IFunctor name;
//        Predicate<IFunctor> body;
//        final int arity;


        // open_null_stream/1
        // close/1
        // close/2
        // stream_property/2
        // current_stream/3
        // is_stream/1
        // stream_pair/3
        // set_stream_position/2
        // stream_position_data/3
        // seek/4
        // set_stream/2
        // set_prolog_IO/3
        final HtMethodDef[] methods = new HtMethodDef[]{
                createMethod("open/3", HiTalkStream::open_3_4, 3),
                createMethod("open/4", HiTalkStream::open_3_4, 4,
                        /*createOptions(*/"alias(Atom)",
                        "bom(Bool)",
                        "buffer(Buffering)",
                        "close_on_abort(Bool)",
                        "create(+List)",
                        "encoding(Encoding)",
                        "eof_action(Action)",
                        "locale(+Locale)",
                        "lock(LockingMode)",
                        "type(Type)",
                        "wait(Bool)"
//                        )
                ),//
                createMethod("open_null_stream/1", HiTalkStream::open_null_stream_1, 1),
                createMethod("close/1", HiTalkStream::close_1_2, 1),
                createMethod("close/2", HiTalkStream::close_1_2, 2),
                createMethod("stream_property/2", HiTalkStream::stream_property_2, 2),
                createMethod("current_stream/3", HiTalkStream::current_stream_3, 3),
                createMethod("is_stream/1", HiTalkStream::is_stream_1, 1),
                createMethod("stream_pair/3", HiTalkStream::stream_pair_3, 3),
                createMethod("set_stream_position/2", HiTalkStream::set_stream_position_2, 2),

                createMethod("stream_position_data/3", HiTalkStream::stream_position_data_3, 3),

                createMethod("seek/4", HiTalkStream::seek_4, 4),
                createMethod("set_prolog_IO/3", HiTalkStream::set_prolog_io_3, 3)};

        final Map<String, HtMethodDef> mmap = new HashMap<>();

//                Gives the stream a name. Below is an example. Be careful with this option as stream names are global. See also set_stream/2.
//
//                ?- open(data, read, Fd, [alias(input)]).
//
//                ...,
//        read(input, Term),
//                ...
//
//        bom(Bool)
//        Check for a BOM (Byte Order Marker) or write one. If omitted, the default is true for mode read and false for mode write. See also stream_property/2 and especially section 2.20.1.1 for a discussion of this feature.
//                buffer(Buffering)
//        Defines output buffering. The atom full (default) defines full buffering, line buffering by line, and false implies the stream is fully unbuffered. Smaller buffering is useful if another process or the user is waiting for the output as it is being produced. See also flush_output/[0,1]. This option is not an ISO option.
//                close_on_abort(Bool)
//        If true (default), the stream is closed on an abort (see abort/0). If false, the stream is not closed.
//        If it is an output stream, however, it will be flushed. Useful for logfiles and if the stream is associated to
//        a process (using the pipe/1 construct).
//                create(+List)
//        Specifies how a new file is created when opening in write, append or update mode. Currently, List is a list
//        of atoms that describe the permissions of the created file.
//        Defined values are below. Not recognised values are silently ignored, allowing for adding platform specific
//        extensions to this set.
//
//                read
//        Allow read access to the file.
//        write
//        Allow write access to the file.
//        execute
//        Allow execution access to the file.
//        default
//        Allow read and write access to the file.
//        all
//        Allow any access provided by the OS.
//
//                Note that if List is empty, the created file has no associated access permissions.
//                The create options map to the POSIX mode option of open(), where read map to 0444, write to 0222 and
//                execute to 0111. On POSIX systems, the final permission is defined as (mode & ~umask).

//                encoding(Encoding)
//        Define the encoding used for reading and writing text to this stream. The default encoding for type text is
//        derived from the Prolog flag encoding. For binary streams the default encoding is octet. For details on
//        encoding issues, see section 2.20.1.
//                eof_action(Action)
//        Defines what happens if the end of the input stream is reached. The default value for Action is eof_code,
//        which makes get0/1 and friends return -1, and read/1 and friends return the atom end_of_file.
//        Repetitive reading keeps yielding the same result. Action error is like eof_code, but repetitive reading will
//        raise an error. With action reset, Prolog will examine the file again and return more data if the file has grown.
//        locale(+Locale)
//        Set the locale that is used by notably format/2 for output on this stream. See section 4.23.
//                lock(LockingMode)
//        Try to obtain a lock on the open file. Default is none, which does not lock the file.
//        The value read or shared means other processes may read the file, but not write it. The value write or
//        exclusive means no other process may read or write the file.
//
//                Locks are acquired through the POSIX function fcntl() using the command F_SETLKW, which makes
//                a blocked call wait for the lock to be released. Please note that fcntl() locks are advisory and
//                therefore only other applications using the same advisory locks honour your lock. As there are many
//                issues around locking in Unix, especially related to NFS (network file system), please study the
//                fcntl() manual page before trusting your locks!
//
//                The lock option is a SWI-Prolog extension.
//
//        Using type text (default), Prolog will write a text file in an operating system compatible way.
//        Using type binary the bytes will be read or written without any translation. See also the option encoding.
//                wait(Bool)
//        This option can be combined with the lock option. If false (default true), the open call returns immediately
//        with an exception if the file is locked. The exception has the format permission_error(lock, source_sink, SrcDest).
//
//        The option reposition is not supported in SWI-Prolog. All streams connected to a file may be repositioned.


        return new PropertyOwner(props, methods, map, mmap);
    }

    //===================================================================================================================
    private static boolean open_3_4(IFunctor functor) {
        return false;
    }

    private static boolean open_null_stream_1(IFunctor functor) {
        return false;
    }

    private static boolean close_1_2(IFunctor functor) {
        return false;
    }

    private static boolean stream_property_2(IFunctor functor) {
        return false;
    }

    private static boolean current_stream_3(IFunctor functor) {
        return true;
    }

    private static boolean is_stream_1(IFunctor functor) {
        return true;
    }

    private static boolean stream_pair_3(IFunctor functor) {
        return true;
    }

    private static boolean set_stream_position_2(IFunctor functor) {
        return true;
    }

    private static boolean stream_position_data_3(IFunctor functor) {
        return true;
    }

    private static boolean seek_4(IFunctor functor) {
        return true;
    }

    private static boolean set_prolog_io_3(IFunctor functor) {
        return true;
    }

    //===================================================================================================================
    private HtMethodDef createMethod(String methodName, Predicate<IFunctor> body, int arity, String... options) {

        String[] pid = methodName.split("/");
        final IFunctor functor = appContext.getTermFactory().newFunctor(pid[1], new ListTerm(2));
        functor.setArgument(0, appContext.getTermFactory().createAtom(pid[0]));
        functor.setArgument(1, appContext.getTermFactory().newAtomic(arity));
        HtNonVar[] opts = Arrays.stream(options).map(option ->
                appContext.getTermFactory().createNonvar(option)).toArray(HtNonVar[]::new);

        return new HtMethodDef(functor, arity, body, opts);
    }

    protected Charset currentCharset = defaultCharset();
    protected StreamDecoder sd;
    protected Path path;

    public final void open() throws FileNotFoundException {
        if (!isOpen) {
            doOpen();
        }
    }

    abstract protected void doOpen() throws FileNotFoundException;

    /**
     * @param path
     * @param encoding
     * @param options
     */
    protected HiTalkStream(Path path, String encoding, StandardOpenOption... options) throws Exception {
        this();
        this.path = path;
        Charset charset = isSupported(encoding) ? forName(encoding) : defaultCharset();
        CharsetDecoder decoder = charset.newDecoder();
        sd = StreamDecoder.forDecoder(channel, decoder, BB_ALLOC_SIZE);

    }

    /**
     * @param path
     * @param options
     */
    protected HiTalkStream(Path path, StandardOpenOption... options) throws Exception {
        this(path, defaultCharset().name(), options);
    }

    /**
     * @param fd
     * @throws IOException
     */
    public HiTalkStream(FileDescriptor fd) throws Exception {
        this();
        this.fd = fd;
        init(fd);
    }

    /**
     * @param fd
     * @throws IOException
     */
    protected abstract void init(FileDescriptor fd) throws IOException;

    /**
     * This method gets called when a bound property is changed.
     *
     * @param evt A PropertyChangeEvent object describing the event source
     *            and the property that has changed.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("file_name")) {
            path = Paths.get(evt.getNewValue().toString());
        }
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
    public void close() throws IOException {
        if (isOpen) {
            isOpen = false;
            channel.close();
        }
    }

    /**
     * @return
     */
    public int getPropLength() {
        return owner.getPropLength();
    }

    /**
     * @param listener
     */
    @Override
    public void addListener(PropertyChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * @param listener
     */
    @Override
    public void removeListener(PropertyChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * @param property
     * @param value
     */
    @Override
    public void fireEvent(IProperty property, HtNonVar value) {
        PropertyChangeEvent event = new PropertyChangeEvent(
                this,
                property.toString(),
                value,
                property.getValue()
        );
        for (PropertyChangeListener listener : listeners) {
            listener.propertyChange(event);
        }
    }

    public HtNonVar getValue(IFunctor propertyName) {
        return this.getPropMap().get(propertyName.toString()).getValue();
    }

    public void setValue(IFunctor propertyName, HtNonVar newValue) {
//        final HtProperty prop = createProperty(propertyName, String.valueOf(newValue));
//        final ITerm oldValue = getMap().get(propertyName).getValue();
//        final PropertyChangeEvent event = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
//        for (PropertyChangeListener listener : listeners) {
//            listener.propertyChange(event);
//        }
    }

    public HtProperty[] getProps() {
        return owner.getProps();
    }

    public HtMethodDef[] getMethods() {
        return owner.getMethods();
    }

    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append('{');
        toString0(sb);
        sb.append('}');
        return sb.toString();
    }

    /**
     * @param sb
     */
    public void toString0(StringBuilder sb) {
        sb.append(", fd=").append(fd);
        sb.append(", currentCharset=").append(currentCharset);
        sb.append(", sd=").append(sd);
    }

    /**
     * @return
     */
    public boolean isOpen() {
        return isOpen;
    }
//    open(+SrcDest, +Mode, --Stream, +Options)
//    True when SrcDest can be opened in Mode and Stream is an I/O stream to/from the object.
//    SrcDest is normally the name of a file, represented as an atom or string.
//    Mode is one of read, write, append or update.
//    Mode append opens the file for writing, positioning the file pointer at the end.
//    Mode update opens the file for writing, positioning the file pointer at the beginning of the file
//    without truncating the file.
//    Stream is either a variable, in which case it is bound to an integer identifying the stream, or an atom,
//    in which case this atom will be the stream identifier.
//    New code should use the alias(Alias) option for compatibility with the ISO standard.
//
//            SWI-Prolog also allows SrcDest to be a term pipe(Command). In this form, Command is started as a child process and if Mode is write, output written to Stream is sent to the standard input of Command. Viso versa, if Mode is read, data written by Command to the standard output may be read from Stream. On Unix systems, Command is handed to popen() which hands it to the Unix shell. On Windows, Command is executed directly. See also process_create/3 from library(process).
//
//    If SrcDest is an IRI, i.e., starts with <scheme>://, where <scheme> is a non-empty sequence of lowercase ASCII letters open/3,4 calls hooks registered by register_iri_scheme/3. Currently the only predefined IRI scheme is res, providing access to the resource database. See section 13.4.
//
//    The following Options are recognised by open/4:
//
//    alias(Atom)
//    Gives the stream a name. Below is an example. Be careful with this option as stream names are global. See also set_stream/2.
//
//            ?- open(data, read, Fd, [alias(input)]).
//
//            ...,
//    read(input, Term),
//                ...
//
//    Check for a BOM (Byte Order Marker) or write one. If omitted, the default is true for mode read and false for mode write. See also stream_property/2 and especially section 2.20.1.1 for a discussion of this feature.
//
//    Defines output buffering. The atom full (default) defines full buffering, line buffering by line, and false implies the stream is fully unbuffered. Smaller buffering is useful if another process or the user is waiting for the output as it is being produced. See also flush_output/[0,1]. This option is not an ISO option.

//    If true (default), the stream is closed on an abort (see abort/0). If false, the stream is not closed.
//    If it is an output stream, however, it will be flushed.
//    Useful for logfiles and if the stream is associated to a process (using the pipe/1 construct).

//    Specifies how a new file is created when opening in write, append or update mode.
//    Currently, List is a list of atoms that describe the permissions of the created file.
//    Defined values are below. Not recognised values are silently ignored, allowing for adding platform
//    specific extensions to this set.
//
//
//    Allow any access provided by the OS.
//
//    Note that if List is empty, the created file has no associated access permissions. The create options map to the POSIX mode option of open(), where read map to 0444, write to 0222 and execute to 0111. On POSIX systems, the final permission is defined as (mode & ~umask).
//    encoding(Encoding)
//    Define the encoding used for reading and writing text to this stream. The default encoding for type text is derived from the Prolog flag encoding. For binary streams the default encoding is octet. For details on encoding issues, see section 2.20.1.
//    eof_action(Action)
//    Defines what happens if the end of the input stream is reached. The default value for Action is eof_code, which makes get0/1 and friends return -1, and read/1 and friends return the atom end_of_file. Repetitive reading keeps yielding the same result. Action error is like eof_code, but repetitive reading will raise an error. With action reset, Prolog will examine the file again and return more data if the file has grown.
//    locale(+Locale)
//    Set the locale that is used by notably format/2 for output on this stream. See section 4.23.
//    lock(LockingMode)
//    Try to obtain a lock on the open file. Default is none, which does not lock the file. The value read or shared means other processes may read the file, but not write it. The value write or exclusive means no other process may read or write the file.
//
//    Locks are acquired through the POSIX function fcntl() using the command F_SETLKW, which makes a blocked call wait for the lock to be released. Please note that fcntl() locks are advisory and therefore only other applications using the same advisory locks honour your lock. As there are many issues around locking in Unix, especially related to NFS (network file system), please study the fcntl() manual page before trusting your locks!
//
//    The lock option is a SWI-Prolog extension.
//    type(Type)
//    Using type text (default), Prolog will write a text file in an operating system compatible way. Using type binary the bytes will be read or written without any translation. See also the option encoding.
//            wait(Bool)
//    This option can be combined with the lock option.
//    If false (default true), the open call returns immediately with an exception if the file is locked.
//    The exception has the format permission_error(lock, source_sink, SrcDest).
//
//    The option reposition is not supported in SWI-Prolog. All streams connected to a file may be repositioned.

}