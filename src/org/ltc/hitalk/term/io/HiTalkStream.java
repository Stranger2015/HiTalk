package org.ltc.hitalk.term.io;

import org.ltc.hitalk.entities.IProperty;
import org.ltc.hitalk.entities.PropertyOwner;
import sun.nio.cs.HistoricallyNamedCharset;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.*;

/**
 * open(+SrcDest, +Mode, --Stream)
 * ===============================
 * //convert byte buffer in given charset to char buffer in unicode
 * ByteBuffer bb = ByteBuffer.wrap(s.getBytes());
 * CharBuffer cb = charset.decode(bb);
 * //convert char buffer in unicode to byte buffer in given charset
 * ByteBuffer newbb = charset.encode(cb);
 */
public
class HiTalkStream extends PropertyOwner <IProperty> {

    private final IProperty[] props;
    private RandomAccessFile raf;
    protected StreamDecoder sd;
//    protected StreamEncoder se;

    /**
     * Creates a random access file stream to read from, and optionally to
     * write to, the file specified by the {@link File} argument.  A new {@link
     * FileDescriptor} object is created to represent this file connection.
     *
     * <p>The <a name="mode"><tt>mode</tt></a> argument specifies the access mode
     * in which the file is to be opened.  The permitted values and their
     * meanings are:
     *
     * <table summary="Access mode permitted values and meanings">
     * <tr><th align="left">Value</th><th align="left">Meaning</th></tr>
     * <tr><td valign="top"><tt>"r"</tt></td>
     *     <td> Open for reading only.  Invoking any of the <tt>write</tt>
     *     methods of the resulting object will cause an {@link
     *     IOException} to be thrown. </td></tr>
     * <tr><td valign="top"><tt>"rw"</tt></td>
     *     <td> Open for reading and writing.  If the file does not already
     *     exist then an attempt will be made to create it. </td></tr>
     * <tr><td valign="top"><tt>"rws"</tt></td>
     *     <td> Open for reading and writing, as with <tt>"rw"</tt>, and also
     *     require that every update to the file's content or metadata be
     *     written synchronously to the underlying storage device.  </td></tr>
     * <tr><td valign="top"><tt>"rwd"&nbsp;&nbsp;</tt></td>
     *     <td> Open for reading and writing, as with <tt>"rw"</tt>, and also
     *     require that every update to the file's content be written
     *     synchronously to the underlying storage device. </td></tr>
     * </table>
     * <p>
     * The <tt>"rws"</tt> and <tt>"rwd"</tt> modes work much like the {@link
     * FileChannel#force(boolean) force(boolean)} method of
     * the {@link FileChannel} class, passing arguments of
     * <tt>true</tt> and <tt>false</tt>, respetively, except that they always
     * apply to every I/O operation and are therefore often more efficient.  If
     * the file resides on a local storage device then when an invocation of a
     * method of this class returns it is guaranteed that all changes made to
     * the file by that invocation will have been written to that device.  This
     * is useful for ensuring that critical information is not lost in the
     * event of a system crash.  If the file does not reside on a local device
     * then no such guarantee is made.
     *
     * <p>The <tt>"rwd"</tt> mode can be used to reduce the number of I/O
     * operations performed.  Using <tt>"rwd"</tt> only requires updates to the
     * file's content to be written to storage; using <tt>"rws"</tt> requires
     * updates to both the file's content and its metadata to be written, which
     * generally requires at least one more low-level I/O operation.
     *
     * <p>If there is a security manager, its {@code checkRead} method is
     * called with the pathname of the {@code file} argument as its
     * argument to see if read access to the file is allowed.  If the mode
     * allows writing, the security manager's {@code checkWrite} method is
     * also called with the path argument to see if write access to the file is
     * allowed.
     *
     * @param file the file object
     * @param mode the access mode, as described
     *             <a href="#mode">above</a>
     * @throws IllegalArgumentException if the mode argument is not equal
     *                                  to one of <tt>"r"</tt>, <tt>"rw"</tt>, <tt>"rws"</tt>, or
     *                                  <tt>"rwd"</tt>
     * @throws FileNotFoundException    if the mode is <tt>"r"</tt> but the given file object does
     *                                  not denote an existing regular file, or if the mode begins
     *                                  with <tt>"rw"</tt> but the given file object does not denote
     *                                  an existing, writable regular file and a new regular file of
     *                                  that name cannot be created, or if some other error occurs
     *                                  while opening or creating the file
     * @throws SecurityException        if a security manager exists and its
     *                                  {@code checkRead} method denies read access to the file
     *                                  or the mode is "rw" and the security manager's
     *                                  {@code checkWrite} method denies write access to the file
     * @revised 1.4
     * @spec JSR-51.
     * @see SecurityManager#checkRead(String)
     * @see SecurityManager#checkWrite(String)
     * @see FileChannel#force(boolean)
     */
    public
    HiTalkStream ( File file, String mode, IProperty... props ) throws FileNotFoundException {
        this.props = props;
        raf = new RandomAccessFile(file, mode);
    }

    public
    void setStreamDecoder ( StreamDecoder sd ) {
        this.sd = sd;
    }

    public
    StreamDecoder getStreamDecoder () {
        return sd;
    }

    /**
     * @return
     */
    @Override
    public
    int getPropLength () {
        return props.length;
    }

    public
    IProperty[] getProps () {
        return props;
    }

    public
    RandomAccessFile getRaf () {
        return raf;
    }


    /**
     * public InputStreamReader(InputStream in, String charsetName)
     * throws UnsupportedEncodingException
     * {
     * super(in);
     * if (charsetName == null)
     * throw new NullPointerException("charsetName");
     * sd = StreamDecoder.forInputStreamReader(in, this, charsetName);
     * }
     */
    public static
    class StreamDecoder extends Reader {
        private static final int MIN_BYTE_BUFFER_SIZE = 32;
        private static final int DEFAULT_BYTE_BUFFER_SIZE = 8192;
        private volatile boolean isOpen;
        private boolean haveLeftoverChar;
        private char leftoverChar;
        private static volatile boolean channelsAvailable = true;
        private Charset cs;
        private CharsetDecoder decoder;
        private ByteBuffer bb;
        private InputStream in;
        private ReadableByteChannel ch;

        public
        StreamDecoder ( InputStream in, Object lock, String encoding ) {
            super(lock);
            this.isOpen = true;
            this.haveLeftoverChar = false;
            this.cs = Charset.forName(encoding);
            this.decoder = new CharsetDecoder.(cs, 2f, 2f);
            if (this.ch == null) {
                this.in = in;
                this.ch = null;
                this.bb = ByteBuffer.allocate(8192);
            }

            this.bb.flip();
        }

        private
        void ensureOpen () throws IOException {
            if (!this.isOpen) {
                throw new IOException("Stream closed");
            }
        }

        public static
        StreamDecoder forInputStreamReader ( HiTalkStream in, Object lock, final String encoding )
                throws UnsupportedEncodingException {
            String charsetName = encoding;
            if (encoding == null) {
                charsetName = Charset.defaultCharset().name();
            }

            try {
                if (Charset.isSupported(charsetName)) {
                    return new StreamDecoder(in, lock, Charset.forName(charsetName));
                }
            } catch (IllegalCharsetNameException ignored) {
            }

            throw new UnsupportedEncodingException(charsetName);
        }

        public static
        StreamDecoder forInputStreamReader ( InputStream in, Object lock, Charset charset ) {
            return new StreamDecoder(in, lock, charset);
        }

        public static
        StreamDecoder forInputStreamReader ( InputStream in, Object lock, CharsetDecoder decoder ) {
            return new StreamDecoder(in, lock, decoder);
        }

        public static
        StreamDecoder forDecoder ( ReadableByteChannel in, CharsetDecoder lock, int encoding ) {
            return new StreamDecoder(in, lock, encoding);
        }

        public
        String getEncoding () {
            return this.isOpen() ? this.encodingName() : null;
        }

        public
        int read () throws IOException {
            return this.read0();
        }

        private
        int read0 () throws IOException {
            synchronized (this.lock) {
                if (this.haveLeftoverChar) {
                    this.haveLeftoverChar = false;
                    return this.leftoverChar;
                }
                else {
                    char[] encoding = new char[2];
                    int var3 = this.read(encoding, 0, 2);
                    switch (var3) {
                        case -1:
                            return -1;
                        case 0:
                        default:
                            assert false : var3;

                            return -1;
                        case 2:
                            this.leftoverChar = encoding[1];
                            this.haveLeftoverChar = true;
                        case 1:
                            return encoding[0];
                    }
                }
            }
        }

        public
        int read ( char[] array, int encoding, int var3 ) throws IOException {
            int var4 = encoding;
            int var5 = var3;
            synchronized (this.lock) {
                this.ensureOpen();
                if (var4 >= 0 && var4 <= array.length && var5 >= 0 && var4 + var5 <= array.length && var4 + var5 >= 0) {
                    if (var5 == 0) {
                        return 0;
                    }
                    else {
                        byte var7 = 0;
                        if (this.haveLeftoverChar) {
                            array[var4] = this.leftoverChar;
                            ++var4;
                            --var5;
                            this.haveLeftoverChar = false;
                            var7 = 1;
                            if (var5 == 0 || !this.implReady()) {
                                return var7;
                            }
                        }

                        if (var5 == 1) {
                            int var8 = this.read0();
                            if (var8 == -1) {
                                return var7 == 0 ? -1 : var7;
                            }
                            else {
                                array[var4] = (char) var8;
                                return var7 + 1;
                            }
                        }
                        else {
                            return var7 + this.implRead(array, var4, var4 + var5);
                        }
                    }
                }
                else {
                    throw new IndexOutOfBoundsException();
                }
            }
        }

        public
        boolean ready () throws IOException {
            synchronized (this.lock) {
                this.ensureOpen();
                return this.haveLeftoverChar || this.implReady();
            }
        }

        public
        void close () throws IOException {
            synchronized (this.lock) {
                if (this.isOpen) {
                    this.implClose();
                    this.isOpen = false;
                }
            }
        }

        private
        boolean isOpen () {
            return this.isOpen;
        }

        private static
        FileChannel getChannel ( FileInputStream in ) {
            if (!channelsAvailable) {
                return null;
            }
            else {
                try {
                    return in.getChannel();
                } catch (UnsatisfiedLinkError encoding) {
                    channelsAvailable = false;
                    return null;
                }
            }
        }

        StreamDecoder ( InputStream in, Object encoding, Charset var3 ) {
            this(in, encoding, var3.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE));
        }

        StreamDecoder ( InputStream in, Object encoding, CharsetDecoder var3 ) {
            super(encoding);
            this.isOpen = true;
            this.haveLeftoverChar = false;
            this.cs = var3.charset();
            this.decoder = var3;
            if (this.ch == null) {
                this.in = in;
                this.ch = null;
                this.bb = ByteBuffer.allocate(8192);
            }

            this.bb.flip();
        }

        StreamDecoder ( ReadableByteChannel channel, CharsetDecoder encoding, int var3 ) {
            this.isOpen = true;
            this.haveLeftoverChar = false;
            this.in = null;
            this.ch = channel;
            this.decoder = encoding;
            this.cs = encoding.charset();
            this.bb = ByteBuffer.allocate(var3 < 0 ? 8192 : (var3 < 32 ? 32 : var3));
            this.bb.flip();
        }

        private
        int readBytes () throws IOException {
            this.bb.compact();

            int var1;
            try {
                int encoding;
                if (this.ch != null) {
                    var1 = this.ch.read(this.bb);
                    if (var1 < 0) {
                        encoding = var1;
                        return encoding;
                    }
                }
                else {
                    var1 = this.bb.limit();
                    encoding = this.bb.position();

                    assert encoding <= var1;

                    int var3 = encoding <= var1 ? var1 - encoding : 0;

                    assert var3 > 0;

                    int var4 = this.in.read(this.bb.array(), this.bb.arrayOffset() + encoding, var3);
                    if (var4 < 0) {
                        int var5 = var4;
                        return var5;
                    }

                    if (var4 == 0) {
                        throw new IOException("Underlying input stream returned zero bytes");
                    }

                    assert var4 <= var3 : "n = " + var4 + ", rem = " + var3;

                    this.bb.position(encoding + var4);
                }
            } finally {
                this.bb.flip();
            }

            var1 = this.bb.remaining();

            assert var1 != 0 : var1;

            return var1;
        }

        int implRead ( char[] var1, int encoding, int var3 ) throws IOException {
            assert var3 - encoding > 1;

            CharBuffer var4 = CharBuffer.wrap(var1, encoding, var3 - encoding);
            if (var4.position() != 0) {
                var4 = var4.slice();
            }

            boolean var5 = false;

            while (true) {
                CoderResult var6 = this.decoder.decode(this.bb, var4, var5);
                if (var6.isUnderflow()) {
                    if (var5 || !var4.hasRemaining() || var4.position() > 0 && !this.inReady()) {
                        break;
                    }

                    int var7 = this.readBytes();
                    if (var7 < 0) {
                        var5 = true;
                        if (var4.position() == 0 && !this.bb.hasRemaining()) {
                            break;
                        }

                        this.decoder.reset();
                    }
                }
                else {
                    if (var6.isOverflow()) {
                        assert var4.position() > 0;
                        break;
                    }

                    var6.throwException();
                }
            }

            if (var5) {
                this.decoder.reset();
            }

            if (var4.position() == 0) {
                if (var5) {
                    return -1;
                }

                assert false;
            }

            return var4.position();
        }

        String encodingName () {
            return this.cs instanceof HistoricallyNamedCharset ? ((HistoricallyNamedCharset) this.cs).historicalName() : this.cs.name();
        }

        private
        boolean inReady () {
            try {
                return this.in != null && this.in.available() > 0 || this.ch instanceof FileChannel;
            } catch (IOException encoding) {
                return false;
            }
        }

        boolean implReady () {
            return this.bb.hasRemaining() || this.inReady();
        }

        void implClose () throws IOException {
            if (this.ch != null) {
                this.ch.close();
            }
            else {
                this.in.close();
            }
        }
    }

    /**
     *
     */
    public
    enum Property {
        alias, //        alias(Atom)
        buffer,//    buffer(Buffering)full, line or false
        buffer_size,//buffer_size(Integer)
        bom,//bom( Bool )
        close_on_abort,//close_on_abort(Bool)
        close_on_exec,
        encoding,//encoding(Encoding)
        end_of_stream,//end_of_stream(E)
        eof_action,
        file_name,
        file_no,
        input,
        locale,//     locale( Locale )
        mode,//mode(IOMode)
        newline,//newline(NewlineMode)
        nlink,//nlink(-Count
        output,
        //        True if Stream has mode write, append or update.
        position, //( Pos )
        //        Unify Pos with the current stream position. A stream position is an opaque term whose fields can be extracted using stream_position_data/3. See also set_stream_position/2.
        reposition,//(Bool)
        //        Unify Bool with true if the position of the stream can be set (see seek/4). It is assumed the position can be set if the stream has a seek-function and is not based on a POSIX file descriptor that is not associated to a regular file.
        representation_errors,//(Mode)
        //        Determines behaviour of character output if the stream cannot represent a character.
//        For example, an ISO Latin-1 stream cannot represent Cyrillic characters.
//        The behaviour is one of error (throw an I/O error exception), prolog (write \...\ escape code) or xml (write &#...; XML character entity).
//        The initial mode is prolog for the user streams and error for all other streams. See also section 2.20.1 and set_stream/2.
        timeout, //(-Time)
        //        Time is the timeout currently associated with the stream. See set_stream/2 with the same option. If no timeout is specified, Time is unified to the atom infinite.
        type,//(Type)
        //        Unify Type with text or binary.
        tty,//        This property is reported with Bool equal to true if the stream is associated with a terminal. See also set_stream/2.
        write_errors, //( Atom )
//        SWI-Prolog extension to query the buffering mode of this stream. Buffering is one of . See also open/4.
//
////        SWI-Prolog extension to query the size of the I/O buffer associated to a stream in bytes.
//                //Fails if the stream is not buffered.
//
//  or a BOM was written while opening the stream. See section 2.20.1.1 for details.
//    close_on_abort(Bool)
//        Determine whether or not abort/0 closes the stream. By default streams are closed.
////    close_on_exec(Bool)
//        Determine whether or not the stream is closed when executing a new process (exec() in Unix, CreateProcess() in Windows). Default is to close streams. This maps to fcntl() F_SETFD using the flag FD_CLOEXEC on Unix and (negated) HANDLE_FLAG_INHERIT on Windows.
//    encoding(Encoding)
//        Query the encoding used for text. See section 2.20.1 for an overview of wide character and encoding issues in SWI-Prolog.
//    end_of_stream(E)
//        If Stream is an input stream, unify E with one of the atoms not, at or past. See also at_end_of_stream/[0,1].
        //    eof_action(A)
//        Unify A with one of eof_code, reset or error. See open/4 for details.
//    file_name(Atom)
//        If Stream is associated to a file, unify Atom to the name of this file.
//    file_no(Integer)
//        If the stream is associated with a POSIX file descriptor, unify Integer with the descriptor number.
//        SWI-Prolog extension used primarily for integration with foreign code. See also Sfileno() from SWI-Stream.h.
//    input
//        True if Stream has mode read.
        //    locale( Locale )
//        True when Locale is the current locale associated with the stream. See section 4.23.
        //    mode(IOMode)
//        Unify IOMode to the mode given to open/4 for opening the stream. Values are: read, write, append and the SWI-Prolog extension update.
//    newline(NewlineMode)
//        One of posix or dos. If dos, text streams will emit \r\n for \n and discard \r from input streams.
        //Default depends on the operating system.
        //    nlink(-Count)
//        Number of hard links to the file. This expresses the number of `names' the file has.
//Not supported on all operating systems and the value might be bogus.
//See the documentation of fstat() for your OS and the value st_nlink.
//    output
//        True if Stream has mode write, append or update.
//    position(Pos)
//        Unify Pos with the current stream position. A stream position is an opaque term whose fields can be extracted using stream_position_data/3. See also set_stream_position/2.
//    reposition(Bool)
//        Unify Bool with true if the position of the stream can be set (see seek/4). It is assumed the position can be set if the stream has a seek-function and is not based on a POSIX file descriptor that is not associated to a regular file.
//    representation_errors(Mode)
//        Determines behaviour of character output if the stream cannot represent a character. For example, an ISO Latin-1 stream cannot represent Cyrillic characters. The behaviour is one of error (throw an I/O error exception), prolog (write \...\ escape code) or xml (write &#...; XML character entity). The initial mode is prolog for the user streams and error for all other streams. See also section 2.20.1 and set_stream/2.
//    timeout(-Time)
//        Time is the timeout currently associated with the stream. See set_stream/2 with the same option. If no timeout is specified, Time is unified to the atom infinite.
//    type(Type)
//        Unify Type with text or binary.
//    tty(Bool)
//        This property is reported with Bool equal to true if the stream is associated with a terminal. See also set_stream/2.
//    write_errors(Atom)
    }
}
