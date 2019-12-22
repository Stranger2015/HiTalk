package org.ltc.hitalk.term.io;

import org.jetbrains.annotations.NotNull;
import org.ltc.hitalk.entities.IProperty;
import org.ltc.hitalk.parser.PlTokenSource;
import org.ltc.hitalk.term.ITerm;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonReadableChannelException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class HiTalkInputStream extends HiTalkStream implements Readable {

    private BufferedReader input;
    //    private final int bufSize;
    protected FileInputStream inputStream;
    protected PushbackInputStream pushbackInputStream;
    private int bof;
    private PlTokenSource tokenSource;
    private int lineNumber;
    private int colNumber;

    /**
     * Creates a buffering character-input stream that uses a default-sized
     * input buffer.
     *
     * @param bufSize
     * @param path
     */
    public HiTalkInputStream ( int bufSize, Path path ) throws IOException {
        final FileInputStream fis = new FileInputStream(path.toFile());
        setInputStream(fis);
        this.input = new LineNumberReader(new BufferedReader(new InputStreamReader(pushbackInputStream)), bufSize);
        setTokenSource(PlTokenSource.getTokenSourceForIoFile(path.toFile()));
    }

    public HiTalkInputStream ( Path path, PlTokenSource tokenSource ) throws IOException {
//        options.add(READ);
        setInputStream(inputStream);
        setTokenSource(tokenSource);
        this.input = new LineNumberReader(new BufferedReader(new InputStreamReader(pushbackInputStream)), 8192);
    }

    public HiTalkInputStream ( FileDescriptor in ) {
        fd = in;
        setInputStream(new FileInputStream(in));
    }

    public HiTalkInputStream ( String path ) throws IOException {
        this(8192, Paths.get(path));
    }

    /**
     * Attempts to read characters into the specified character buffer.
     * The buffer is used as a repository of characters as-is: the only
     * changes made are the results of a put operation. No flipping or
     * rewinding of the buffer is performed.
     *
     * @param cb the buffer to read characters into
     * @return The number of {@code char} values added to the buffer,
     * or -1 if this source of characters is at its end
     * @throws IOException             if an I/O error occurs
     * @throws NullPointerException    if cb is null
     * @throws ReadOnlyBufferException if cb is a read only buffer
     */
    public int read ( @NotNull CharBuffer cb ) throws IOException {
        return 0;
    }

    /**
     * Read the byte from input stream
     *
     * @return
     * @throws IOException
     */
    public int read () throws IOException {
        return pushbackInputStream.read();
    }

    /**
     * Pushes back a byte by copying it to the front of the
     * pushback buffer. After this method returns, the next byte to be read
     * will have the value <code>(byte)c</code>.
     *
     * @param c The int value representing a byte to be pushed back
     * @throws IOException If the pushback buffer is full,
     *                     or if some other I/O error occurs
     */
    public void unread ( int c ) throws IOException {
        pushbackInputStream.unread(c);
    }

    /**
     * Reads some bytes from an input
     * stream and stores them into the buffer
     * array {@code b}. The number of bytes
     * read is equal
     * to the length of {@code b}.
     * <p>
     * This method blocks until one of the
     * following conditions occurs:
     * <ul>
     * <li>{@code b.length}
     * bytes of input data are available, in which
     * case a normal return is made.
     *
     * <li>End of
     * file is detected, in which case an {@code EOFException}
     * is thrown.
     *
     * <li>An I/O error occurs, in
     * which case an {@code IOException} other
     * than {@code EOFException} is thrown.
     * </ul>
     * <p>
     * If {@code b} is {@code null},
     * a {@code NullPointerException} is thrown.
     * If {@code b.length} is zero, then
     * no bytes are read. Otherwise, the first
     * byte read is stored into element {@code b[0]},
     * the next one into {@code b[1]}, and
     * so on.
     * If an exception is thrown from
     * this method, then it may be that some but
     * not all bytes of {@code b} have been
     * updated with data from the input stream.
     *
     * @param b the buffer into which the data is read.
     * @throws EOFException if this stream reaches the end before reading
     *                      all the bytes.
     * @throws IOException  if an I/O error occurs.
     */
    public void readFully ( @NotNull byte[] b ) throws IOException {

    }

    /**
     * Reads {@code len}
     * bytes from
     * an input stream.
     * <p>
     * This method
     * blocks until one of the following conditions
     * occurs:
     * <ul>
     * <li>{@code len} bytes
     * of input data are available, in which case
     * a normal return is made.
     *
     * <li>End of file
     * is detected, in which case an {@code EOFException}
     * is thrown.
     *
     * <li>An I/O error occurs, in
     * which case an {@code IOException} other
     * than {@code EOFException} is thrown.
     * </ul>
     * <p>
     * If {@code b} is {@code null},
     * a {@code NullPointerException} is thrown.
     * If {@code off} is negative, or {@code len}
     * is negative, or {@code off+len} is
     * greater than the length of the array {@code b},
     * then an {@code IndexOutOfBoundsException}
     * is thrown.
     * If {@code len} is zero,
     * then no bytes are read. Otherwise, the first
     * byte read is stored into element {@code b[off]},
     * the next one into {@code b[off+1]},
     * and so on. The number of bytes read is,
     * at most, equal to {@code len}.
     *
     * @param b   the buffer into which the data is read.
     * @param off an int specifying the offset into the data.
     * @param len an int specifying the number of bytes to read.
     * @throws EOFException if this stream reaches the end before reading
     *                      all the bytes.
     * @throws IOException  if an I/O error occurs.
     */
    //@Override
    public void readFully ( @NotNull byte[] b, int off, int len ) throws IOException {

    }

    /**
     * @return
     * @throws IOException
     */
    public char readChar () throws IOException {
        int ch1 = read();
        int ch2 = read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        colNumber++;
        return (char) ((ch1 & 0xff << 8) + ch2);
    }

    /**
     * Reads four input bytes and returns an
     * {@code int} value. Let {@code a-d}
     * be the first through fourth bytes read. The value returned is:
     * <pre>{@code
     * (((a & 0xff) << 24) | ((b & 0xff) << 16) |
     *  ((c & 0xff) <<  8) | (d & 0xff))
     * }</pre>
     * This method is suitable
     * for reading bytes written by the {@code writeInt}
     * method of interface {@code DataOutput}.
     *
     * @return the {@code int} value read.
     * @throws EOFException if this stream reaches the end before reading
     *                      all the bytes.
     * @throws IOException  if an I/O error occurs.
     */
    //@Override
    public int readInt () throws IOException {//fixme
        int ch1 = read();
        int ch2 = read();
        int ch3 = read();
        int ch4 = read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch1 & 0xff << 24) + (ch2 & 0xff << 16) + (ch3 << 8) + ch4);
    }

    /**
     * Reads the next line of text from the input stream.
     * It reads successive bytes, converting
     * each byte separately into a character,
     * until it encounters a line terminator or
     * end of
     * file; the characters read are then
     * returned as a {@code String}. Note
     * that because this
     * method processes bytes,
     * it does not support input of the full Unicode
     * character set.
     * <p>
     * If end of file is encountered
     * before even one byte can be read, then {@code null}
     * is returned. Otherwise, each byte that is
     * read is converted to type {@code char}
     * by zero-extension. If the character {@code '\n'}
     * is encountered, it is discarded and reading
     * ceases. If the character {@code '\r'}
     * is encountered, it is discarded and, if
     * the following byte converts &#32;to the
     * character {@code '\n'}, then that is
     * discarded also; reading then ceases. If
     * end of file is encountered before either
     * of the characters {@code '\n'} and
     * {@code '\r'} is encountered, reading
     * ceases. Once reading has ceased, a {@code String}
     * is returned that contains all the characters
     * read and not discarded, taken in order.
     * Note that every character in this string
     * will have a value less than {@code \u005Cu0100},
     * that is, {@code (char)256}.
     *
     * @return the next line of text from the input stream,
     * or {@code null} if the end of file is
     * encountered before a byte can be read.
     * @throws IOException if an I/O error occurs.
     */
//    public String readLine () throws IOException {
//        return null;
//    }

    /**
     * Reads in a string that has been encoded using a
     * <a href="#modified-utf-8">modified UTF-8</a>
     * format.
     * The general contract of {@code readUTF}
     * is that it reads a representation of a Unicode
     * character string encoded in modified
     * UTF-8 format; this string of characters
     * is then returned as a {@code String}.
     * <p>
     * First, two bytes are read and used to
     * construct an unsigned 16-bit integer in
     * exactly the manner of the {@code readUnsignedShort}
     * method . This integer value is called the
     * <i>UTF length</i> and specifies the number
     * of additional bytes to be read. These bytes
     * are then converted to characters by considering
     * them in groups. The length of each group
     * is computed from the value of the first
     * byte of the group. The byte following a
     * group, if any, is the first byte of the
     * next group.
     * <p>
     * If the first byte of a group
     * matches the bit pattern {@code 0xxxxxxx}
     * (where {@code x} means "may be {@code 0}
     * or {@code 1}"), then the group consists
     * of just that byte. The byte is zero-extended
     * to form a character.
     * <p>
     * If the first byte
     * of a group matches the bit pattern {@code 110xxxxx},
     * then the group consists of that byte {@code a}
     * and a second byte {@code b}. If there
     * is no byte {@code b} (because byte
     * {@code a} was the last of the bytes
     * to be read), or if byte {@code b} does
     * not match the bit pattern {@code 10xxxxxx},
     * then a {@code UTFDataFormatException}
     * is thrown. Otherwise, the group is converted
     * to the character:
     * <pre>{@code (char)(((a & 0x1F) << 6) | (b & 0x3F))
     * }</pre>
     * If the first byte of a group
     * matches the bit pattern {@code 1110xxxx},
     * then the group consists of that byte {@code a}
     * and two more bytes {@code b} and {@code c}.
     * If there is no byte {@code c} (because
     * byte {@code a} was one of the last
     * two of the bytes to be read), or either
     * byte {@code b} or byte {@code c}
     * does not match the bit pattern {@code 10xxxxxx},
     * then a {@code UTFDataFormatException}
     * is thrown. Otherwise, the group is converted
     * to the character:
     * <pre>{@code
     * (char)(((a & 0x0F) << 12) | ((b & 0x3F) << 6) | (c & 0x3F))
     * }</pre>
     * If the first byte of a group matches the
     * pattern {@code 1111xxxx} or the pattern
     * {@code 10xxxxxx}, then a {@code UTFDataFormatException}
     * is thrown.
     * <p>
     * If end of file is encountered
     * at any time during this entire process,
     * then an {@code EOFException} is thrown.
     * <p>
     * After every group has been converted to
     * a character by this process, the characters
     * are gathered, in the same order in which
     * their corresponding groups were read from
     * the input stream, to form a {@code String},
     * which is returned.
     * <p>
     * The {@code writeUTF}
     * method of interface {@code DataOutput}
     * may be used to write data that is suitable
     * for reading by this method.
     *
     * @return a Unicode string.
     * @throws EOFException           if this stream reaches the end
     *                                before reading all the bytes.
     * @throws IOException            if an I/O error occurs.
     * @throws UTFDataFormatException if the bytes do not represent a
     *                                valid modified UTF-8 encoding of a string.
     */
    @NotNull
    public String readUTF () throws IOException {
        return null;
    }

    public void unreadChar ( int ch ) throws IOException {
        unread(((ch >>> 8) & 0xFF));
        unread(((ch) & 0xFF));
        colNumber--;
    }

    /**
     * @return
     */
    public PlTokenSource getTokenSource () {
        return tokenSource;
    }

    /**
     * @param tokenSource
     */
    public void setTokenSource ( PlTokenSource tokenSource ) {
        this.tokenSource = tokenSource;
    }

    //@Override
    public int skipBytes ( int n ) throws IOException {
        return Math.toIntExact(pushbackInputStream.skip(n));
    }

    //@Override
    public boolean readBoolean () throws IOException {
        return read() != 0;
    }

    //@Override
    public byte readByte () throws IOException {
        return (byte) read();
    }

    //@Override
    public int readUnsignedByte () throws IOException {
        int ch = read();
        if (ch < 0) {
            throw new EOFException();
        }
        return ch;
    }

    //@Override
    public short readShort () throws IOException {
        int ch1 = read();
        int ch2 = read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (short) ((ch1 << 8) + ch2);
    }

    //@Override
    public int readUnsignedShort () throws IOException {
        int ch = read();
        if (ch < 0) {
            throw new EOFException();
        }
        return ch;
    }

    //
//    /**
//     * @return
//     * @throws IOException
//     */
    //@Override
    public long readLong () throws IOException {//fixme
        int ch1 = read();
        int ch2 = read();
        int ch3 = read();
        int ch4 = read();
        int ch5 = read();
        int ch6 = read();
        int ch7 = read();
        int ch8 = read();
        if ((ch1 | ch2 | ch3 | ch4 | ch5 | ch6 | ch7 | ch8) < 0) {
            throw new EOFException();
        }
        return (ch1 & 0xffL << 56) + (ch2 & 0xffL << 48) + (ch3 & 0xffL << 40) + (ch4 & 0xffL << 32) +
                (ch5 & 0xffL << 24) + (ch6 & 0xffL << 16) + (ch7 & 0xffL << 8) + ch8;
    }

    /**
     * Reads four input bytes and returns
     * a {@code float} value. It does this
     * by first constructing an {@code int}
     * value in exactly the manner
     * of the {@code readInt}
     * method, then converting this {@code int}
     * value to a {@code float} in
     * exactly the manner of the method {@code Float.intBitsToFloat}.
     * This method is suitable for reading
     * bytes written by the {@code writeFloat}
     * method of interface {@code DataOutput}.
     *
     * @return the {@code float} value read.
     * @throws EOFException if this stream reaches the end before reading
     *                      all the bytes.
     * @throws IOException  if an I/O error occurs.
     */
    public float readFloat () throws IOException {//fixme
        return Float.intBitsToFloat(readInt());
    }

    /**
     * Reads eight input bytes and returns
     * a {@code double} value. It does this
     * by first constructing a {@code long}
     * value in exactly the manner
     * of the {@code readLong}
     * method, then converting this {@code long}
     * value to a {@code double} in exactly
     * the manner of the method {@code Double.longBitsToDouble}.
     * This method is suitable for reading
     * bytes written by the {@code writeDouble}
     * method of interface {@code DataOutput}.
     *
     * @return the {@code double} value read.
     * @throws EOFException if this stream reaches the end before reading
     *                      all the bytes.
     * @throws IOException  if an I/O error occurs.
     */

    //@Override
    public double readDouble () throws IOException {//fixme
        return Double.longBitsToDouble(readLong());
    }

    /**
     * Reads the next line of text from the input stream.
     * It reads successive bytes, converting
     * each byte separately into a character,
     * until it encounters a line terminator or
     * end of
     * file; the characters read are then
     * returned as a {@code String}. Note
     * that because this
     * method processes bytes,
     * it does not support input of the full Unicode
     * character set.
     * <p>
     * If end of file is encountered
     * before even one byte can be read, then {@code null}
     * is returned. Otherwise, each byte that is
     * read is converted to type {@code char}
     * by zero-extension. If the character {@code '\n'}
     * is encountered, it is discarded and reading
     * ceases. If the character {@code '\r'}
     * is encountered, it is discarded and, if
     * the following byte converts &#32;to the
     * character {@code '\n'}, then that is
     * discarded also; reading then ceases. If
     * end of file is encountered before either
     * of the characters {@code '\n'} and
     * {@code '\r'} is encountered, reading
     * ceases. Once reading has ceased, a {@code String}
     * is returned that contains all the characters
     * read and not discarded, taken in order.
     * Note that every character in this string
     * will have a value less than {@code \u005Cu0100},
     * that is, {@code (char)256}.
     *
     * @return the next line of text from the input stream,
     * or {@code null} if the end of file is
     * encountered before a byte can be read.
     * @throws IOException if an I/O error occurs.
     */
    public String readLine () throws IOException {
        return input.readLine();
    }

    public void setInputStream ( FileInputStream inputStream ) {
        this.inputStream = inputStream;
        channel = inputStream.getChannel();
        pushbackInputStream = new PushbackInputStream(inputStream);
    }

    public FileInputStream getInputStream () {
        return inputStream;
    }

    /**
     * Reads a sequence of bytes from this channel into the given buffer.
     *
     * <p> An attempt is made to read up to <i>r</i> bytes from the channel,
     * where <i>r</i> is the number of bytes remaining in the buffer, that is,
     * <tt>dst.remaining()</tt>, at the moment this method is invoked.
     *
     * <p> Suppose that a byte sequence of length <i>n</i> is read, where
     * <tt>0</tt>&nbsp;<tt>&lt;=</tt>&nbsp;<i>n</i>&nbsp;<tt>&lt;=</tt>&nbsp;<i>r</i>.
     * This byte sequence will be transferred into the buffer so that the first
     * byte in the sequence is at index <i>p</i> and the last byte is at index
     * <i>p</i>&nbsp;<tt>+</tt>&nbsp;<i>n</i>&nbsp;<tt>-</tt>&nbsp;<tt>1</tt>,
     * where <i>p</i> is the buffer's position at the moment this method is
     * invoked.  Upon return the buffer's position will be equal to
     * <i>p</i>&nbsp;<tt>+</tt>&nbsp;<i>n</i>; its limit will not have changed.
     *
     * <p> A read operation might not fill the buffer, and in fact it might not
     * read any bytes at all.  Whether or not it does so depends upon the
     * nature and state of the channel.  A socket channel in non-blocking mode,
     * for example, cannot read any more bytes than are immediately available
     * from the socket's input buffer; similarly, a file channel cannot read
     * any more bytes than remain in the file.  It is guaranteed, however, that
     * if a channel is in blocking mode and there is at least one byte
     * remaining in the buffer then this method will block until at least one
     * byte is read.
     *
     * <p> This method may be invoked at any time.  If another thread has
     * already initiated a read operation upon this channel, however, then an
     * invocation of this method will block until the first operation is
     * complete. </p>
     *
     * @param dst The buffer into which bytes are to be transferred
     * @return The number of bytes read, possibly zero, or <tt>-1</tt> if the
     * channel has reached end-of-stream
     * @throws NonReadableChannelException If this channel was not opened for reading
     * @throws ClosedChannelException      If this channel is closed
     * @throws AsynchronousCloseException  If another thread closes this channel
     *                                     while the read operation is in progress
     * @throws ClosedByInterruptException  If another thread interrupts the current thread
     *                                     while the read operation is in progress, thereby
     *                                     closing the channel and setting the current thread's
     *                                     interrupt status
     * @throws IOException                 If some other I/O error occurs
     */
    //@Override
    public int read ( ByteBuffer dst ) throws IOException {
        return 0;
    }

    /**
     * Tells whether or not this channel is open.
     *
     * @return <tt>true</tt> if, and only if, this channel is open
     */
    //@Override
    public boolean isOpen () {
        return false;
    }

    /**
     * Closes this channel.
     *
     * <p> After a channel is closed, any further attempt to invoke I/O
     * operations upon it will cause a {@link ClosedChannelException} to be
     * thrown.
     *
     * <p> If this channel is already closed then invoking this method has no
     * effect.
     *
     * <p> This method may be invoked at any time.  If some other thread has
     * already invoked it, however, then another invocation will block until
     * the first invocation is complete, after which it will return without
     * effect. </p>
     *
     * @throws IOException If an I/O error occurs
     */
    //@Override
    public void close () throws IOException {

    }

    //@Override
    protected void init ( FileDescriptor fd ) throws IOException {
        setInputStream(new FileInputStream(fd));
    }

    /**
     * This method gets called when a bound property is changed.
     *
     * @param evt A PropertyChangeEvent object describing the event source
     *            and the property that has changed.
     */
    //@Override
    public void propertyChange ( PropertyChangeEvent evt ) {

    }

    /**
     * @return
     */
    //@Override
    public int getPropLength () {
        return 0;
    }

    //@Override
    public void addListener ( PropertyChangeListener listener ) {

    }

    //@Override
    public void removeListener ( PropertyChangeListener listener ) {

    }

    //@Override
    public void fireEvent ( IProperty property, ITerm value ) {

    }

    //@Override
    public ITerm getValue ( Properties property ) {
        return null;
    }

    /**
     * @param property
     * @param value
     */
    //@Override
    public void setValue ( Properties property, ITerm value ) {

    }

    //@Override
    public HiTalkStream copy () throws CloneNotSupportedException {
        return null;
    }

    public int getLineNumber () {
        return lineNumber;
    }

    public void setLineNumber ( int lineNumber ) {
        this.lineNumber = lineNumber;
    }

    public int getColNumber () {
        return colNumber;
    }

    public void setColNumber ( int colNumber ) {
        this.colNumber = colNumber;
    }
//
//    //@Override
//    public String readUTF () throws IOException {
//        return dis.readUTF();//fixme
//    }
}
