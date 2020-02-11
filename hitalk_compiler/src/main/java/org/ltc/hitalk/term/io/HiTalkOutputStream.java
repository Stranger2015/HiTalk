package org.ltc.hitalk.term.io;

import org.jetbrains.annotations.NotNull;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.term.HtNonVar;
import org.ltc.hitalk.wam.compiler.IFunctor;
import sun.nio.cs.StreamEncoder;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import static java.nio.charset.Charset.*;

/**
 *
 */
public class HiTalkOutputStream extends HiTalkStream {

    protected BufferedWriter output;
    private FileOutputStream outputStream;

    /**
     * @param path
     * @param encoding
     * @param options
     */
    public HiTalkOutputStream(BufferedWriter output, Path path, String encoding, StandardOpenOption... options) {
        super(path, encoding, options);
        this.output = output;
        Charset charset = isSupported(encoding) ? forName(encoding) : defaultCharset();//currentCharset;
        CharsetEncoder encoder = charset.newEncoder();
        StreamEncoder se = StreamEncoder.forEncoder(channel, encoder, BB_ALLOC_SIZE);
    }

    public HiTalkOutputStream(int i, FileDescriptor out) {
        super();
        setOutputStream(new FileOutputStream(out));
    }

    /**
     * @param path
     * @param encoding
     * @param options
     */
    protected HiTalkOutputStream(Path path, String encoding, StandardOpenOption... options) {
        super(path, encoding, options);
    }

    public HiTalkOutputStream(Path fileName) throws Exception {
        super(fileName);
    }

    /**
     * @param b
     * @throws IOException
     */
//    @Override
    public void write(int b) throws IOException {
        writeChar(b);
    }

    /**
     * Writes to the output stream all the bytes in array <code>b</code>.
     * If <code>b</code> is <code>null</code>,
     * a <code>NullPointerException</code> is thrown.
     * If <code>b.length</code> is zero, then
     * no bytes are written. Otherwise, the byte
     * <code>b[0]</code> is written first, then
     * <code>b[1]</code>, and so on; the last byte
     * written is <code>b[b.length-1]</code>.
     *
     * @param b the data.
     * @throws IOException if an I/O error occurs.
     */
    public void write(@NotNull byte[] b) throws IOException {

    }

    /**
     * Writes <code>len</code> bytes from array
     * <code>b</code>, in order,  to
     * the output stream.  If <code>b</code>
     * is <code>null</code>, a <code>NullPointerException</code>
     * is thrown.  If <code>off</code> is negative,
     * or <code>len</code> is negative, or <code>off+len</code>
     * is greater than the length of the array
     * <code>b</code>, then an <code>IndexOutOfBoundsException</code>
     * is thrown.  If <code>len</code> is zero,
     * then no bytes are written. Otherwise, the
     * byte <code>b[off]</code> is written first,
     * then <code>b[off+1]</code>, and so on; the
     * last byte written is <code>b[off+len-1]</code>.
     *
     * @param b   the data.
     * @param off the start offset in the data.
     * @param len the number of bytes to write.
     * @throws IOException if an I/O error occurs.
     */
    public void write(@NotNull byte[] b, int off, int len) throws IOException {

    }

    /**
     * Writes a <code>boolean</code> value to this output stream.
     * If the argument <code>v</code>
     * is <code>true</code>, the value <code>(byte)1</code>
     * is written; if <code>v</code> is <code>false</code>,
     * the  value <code>(byte)0</code> is written.
     * The byte written by this method may
     * be read by the <code>readBoolean</code>
     * method of interface <code>DataInput</code>,
     * which will then return a <code>boolean</code>
     * equal to <code>v</code>.
     *
     * @param v the boolean to be written.
     * @throws IOException if an I/O error occurs.
     */
    public void writeBoolean(boolean v) throws IOException {

    }

    /**
     * Writes to the output stream the eight low-
     * order bits of the argument <code>v</code>.
     * The 24 high-order bits of <code>v</code>
     * are ignored. (This means  that <code>writeByte</code>
     * does exactly the same thing as <code>write</code>
     * for an integer argument.) The byte written
     * by this method may be read by the <code>readByte</code>
     * method of interface <code>DataInput</code>,
     * which will then return a <code>byte</code>
     * equal to <code>(byte)v</code>.
     *
     * @param v the byte value to be written.
     * @throws IOException if an I/O error occurs.
     */
    public void writeByte(int v) throws IOException {

    }

    /**
     * Writes two bytes to the output
     * stream to represent the value of the argument.
     * The byte values to be written, in the  order
     * shown, are:
     * <pre>{@code
     * (byte)(0xff & (v >> 8))
     * (byte)(0xff & v)
     * }</pre> <p>
     * The bytes written by this method may be
     * read by the <code>readShort</code> method
     * of interface <code>DataInput</code> , which
     * will then return a <code>short</code> equal
     * to <code>(short)v</code>.
     *
     * @param v the <code>short</code> value to be written.
     * @throws IOException if an I/O error occurs.
     */
    public void writeShort(int v) throws IOException {

    }

    /**
     * Writes a <code>char</code> value, which
     * is comprised of two bytes, to the
     * output stream.
     * The byte values to be written, in the  order
     * shown, are:
     * <pre>{@code
     * (byte)(0xff & (v >> 8))
     * (byte)(0xff & v)
     * }</pre><p>
     * The bytes written by this method may be
     * read by the <code>readChar</code> method
     * of interface <code>DataInput</code> , which
     * will then return a <code>char</code> equal
     * to <code>(char)v</code>.
     *
     * @param v the <code>char</code> value to be written.
     * @throws IOException if an I/O error occurs.
     */
    public void writeChar(int v) throws IOException {

    }

    /**
     * Writes an <code>int</code> value, which is
     * comprised of four bytes, to the output stream.
     * The byte values to be written, in the  order
     * shown, are:
     * <pre>{@code
     * (byte)(0xff & (v >> 24))
     * (byte)(0xff & (v >> 16))
     * (byte)(0xff & (v >>  8))
     * (byte)(0xff & v)
     * }</pre><p>
     * The bytes written by this method may be read
     * by the <code>readInt</code> method of interface
     * <code>DataInput</code> , which will then
     * return an <code>int</code> equal to <code>v</code>.
     *
     * @param v the <code>int</code> value to be written.
     * @throws IOException if an I/O error occurs.
     */
    public void writeInt(int v) throws IOException {

    }

    /**
     * Writes a <code>long</code> value, which is
     * comprised of eight bytes, to the output stream.
     * The byte values to be written, in the  order
     * shown, are:
     * <pre>{@code
     * (byte)(0xff & (v >> 56))
     * (byte)(0xff & (v >> 48))
     * (byte)(0xff & (v >> 40))
     * (byte)(0xff & (v >> 32))
     * (byte)(0xff & (v >> 24))
     * (byte)(0xff & (v >> 16))
     * (byte)(0xff & (v >>  8))
     * (byte)(0xff & v)
     * }</pre><p>
     * The bytes written by this method may be
     * read by the <code>readLong</code> method
     * of interface <code>DataInput</code> , which
     * will then return a <code>long</code> equal
     * to <code>v</code>.
     *
     * @param v the <code>long</code> value to be written.
     * @throws IOException if an I/O error occurs.
     */
    public void writeLong(long v) throws IOException {

    }

    /**
     * Writes a <code>float</code> value,
     * which is comprised of four bytes, to the output stream.
     * It does this as if it first converts this
     * <code>float</code> value to an <code>int</code>
     * in exactly the manner of the <code>Float.floatToIntBits</code>
     * method  and then writes the <code>int</code>
     * value in exactly the manner of the  <code>writeInt</code>
     * method.  The bytes written by this method
     * may be read by the <code>readFloat</code>
     * method of interface <code>DataInput</code>,
     * which will then return a <code>float</code>
     * equal to <code>v</code>.
     *
     * @param v the <code>float</code> value to be written.
     * @throws IOException if an I/O error occurs.
     */
    public void writeFloat(float v) throws IOException {

    }

    /**
     * Writes a <code>double</code> value,
     * which is comprised of eight bytes, to the output stream.
     * It does this as if it first converts this
     * <code>double</code> value to a <code>long</code>
     * in exactly the manner of the <code>Double.doubleToLongBits</code>
     * method  and then writes the <code>long</code>
     * value in exactly the manner of the  <code>writeLong</code>
     * method. The bytes written by this method
     * may be read by the <code>readDouble</code>
     * method of interface <code>DataInput</code>,
     * which will then return a <code>double</code>
     * equal to <code>v</code>.
     *
     * @param v the <code>double</code> value to be written.
     * @throws IOException if an I/O error occurs.
     */
    public void writeDouble(double v) throws IOException {

    }

    /**
     * Writes a string to the output stream.
     * For every character in the string
     * <code>s</code>,  taken in order, one byte
     * is written to the output stream.  If
     * <code>s</code> is <code>null</code>, a <code>NullPointerException</code>
     * is thrown.<p>  If <code>s.length</code>
     * is zero, then no bytes are written. Otherwise,
     * the character <code>s[0]</code> is written
     * first, then <code>s[1]</code>, and so on;
     * the last character written is <code>s[s.length-1]</code>.
     * For each character, one byte is written,
     * the low-order byte, in exactly the manner
     * of the <code>writeByte</code> method . The
     * high-order eight bits of each character
     * in the string are ignored.
     *
     * @param s the string of bytes to be written.
     * @throws IOException if an I/O error occurs.
     */
    public void writeBytes(@NotNull String s) throws IOException {

    }

    /**
     * Writes every character in the string <code>s</code>,
     * to the output stream, in order,
     * two bytes per character. If <code>s</code>
     * is <code>null</code>, a <code>NullPointerException</code>
     * is thrown.  If <code>s.length</code>
     * is zero, then no characters are written.
     * Otherwise, the character <code>s[0]</code>
     * is written first, then <code>s[1]</code>,
     * and so on; the last character written is
     * <code>s[s.length-1]</code>. For each character,
     * two bytes are actually written, high-order
     * byte first, in exactly the manner of the
     * <code>writeChar</code> method.
     *
     * @param s the string value to be written.
     * @throws IOException if an I/O error occurs.
     */
    public void writeChars(@NotNull String s) throws IOException {

    }

    /**
     * Writes two bytes of length information
     * to the output stream, followed
     * by the
     * <a href="DataInput.html#modified-utf-8">modified UTF-8</a>
     * representation
     * of  every character in the string <code>s</code>.
     * If <code>s</code> is <code>null</code>,
     * a <code>NullPointerException</code> is thrown.
     * Each character in the string <code>s</code>
     * is converted to a group of one, two, or
     * three bytes, depending on the value of the
     * character.<p>
     * If a character <code>c</code>
     * is in the range <code>&#92;u0001</code> through
     * <code>&#92;u007f</code>, it is represented
     * by one byte:
     * <pre>(byte)c </pre>  <p>
     * If a character <code>c</code> is <code>&#92;u0000</code>
     * or is in the range <code>&#92;u0080</code>
     * through <code>&#92;u07ff</code>, then it is
     * represented by two bytes, to be written
     * in the order shown: <pre>{@code
     * (byte)(0xc0 | (0x1f & (c >> 6)))
     * (byte)(0x80 | (0x3f & c))
     * }</pre> <p> If a character
     * <code>c</code> is in the range <code>&#92;u0800</code>
     * through <code>uffff</code>, then it is
     * represented by three bytes, to be written
     * in the order shown: <pre>{@code
     * (byte)(0xe0 | (0x0f & (c >> 12)))
     * (byte)(0x80 | (0x3f & (c >>  6)))
     * (byte)(0x80 | (0x3f & c))
     * }</pre>  <p> First,
     * the total number of bytes needed to represent
     * all the characters of <code>s</code> is
     * calculated. If this number is larger than
     * <code>65535</code>, then a <code>UTFDataFormatException</code>
     * is thrown. Otherwise, this length is written
     * to the output stream in exactly the manner
     * of the <code>writeShort</code> method;
     * after this, the one-, two-, or three-byte
     * representation of each character in the
     * string <code>s</code> is written.<p>  The
     * bytes written by this method may be read
     * by the <code>readUTF</code> method of interface
     * <code>DataInput</code> , which will then
     * return a <code>String</code> equal to <code>s</code>.
     *
     * @param s the string value to be written.
     * @throws IOException if an I/O error occurs.
     */
    public void writeUTF(@NotNull String s) throws IOException {

    }

    /**
     * Writes a sequence of bytes to this channel from the given buffer.
     *
     * <p> An attempt is made to write up to <i>r</i> bytes to the channel,
     * where <i>r</i> is the number of bytes remaining in the buffer, that is,
     * <tt>src.remaining()</tt>, at the moment this method is invoked.
     *
     * <p> Suppose that a byte sequence of length <i>n</i> is written, where
     * <tt>0</tt>&nbsp;<tt>&lt;=</tt>&nbsp;<i>n</i>&nbsp;<tt>&lt;=</tt>&nbsp;<i>r</i>.
     * This byte sequence will be transferred from the buffer starting at index
     * <i>p</i>, where <i>p</i> is the buffer's position at the moment this
     * method is invoked; the index of the last byte written will be
     * <i>p</i>&nbsp;<tt>+</tt>&nbsp;<i>n</i>&nbsp;<tt>-</tt>&nbsp;<tt>1</tt>.
     * Upon return the buffer's position will be equal to
     * <i>p</i>&nbsp;<tt>+</tt>&nbsp;<i>n</i>; its limit will not have changed.
     *
     * <p> Unless otherwise specified, a write operation will return only after
     * writing all of the <i>r</i> requested bytes.  Some types of channels,
     * depending upon their state, may write only some of the bytes or possibly
     * none at all.  A socket channel in non-blocking mode, for example, cannot
     * write any more bytes than are free in the socket's output buffer.
     *
     * <p> This method may be invoked at any time.  If another thread has
     * already initiated a write operation upon this channel, however, then an
     * invocation of this method will block until the first operation is
     * complete. </p>
     *
     * @param src The buffer from which bytes are to be retrieved
     * @return The number of bytes written, possibly zero
     * @throws NonWritableChannelException If this channel was not opened for writing
     * @throws ClosedChannelException      If this channel is closed
     * @throws AsynchronousCloseException  If another thread closes this channel
     *                                     while the write operation is in progress
     * @throws ClosedByInterruptException  If another thread interrupts the current thread
     *                                     while the write operation is in progress, thereby
     *                                     closing the channel and setting the current thread's
     *                                     interrupt status
     * @throws IOException                 If some other I/O error occurs
     */
    public int write(ByteBuffer src) throws IOException {
        return 0;
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
    public void close() throws IOException {
        super.close();
        outputStream.flush();
        outputStream.close();
    }

    protected void doOpen() throws FileNotFoundException {

    }

    protected void init(FileDescriptor fd) throws IOException {
        setOutputStream(new FileOutputStream(fd));
    }

    /**
     * @param outputStream
     */
    public void setOutputStream(FileOutputStream outputStream) {
        this.outputStream = outputStream;
    }

    /**
     * @return
     */
    public FileOutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * @param propertyName
     * @return
     */
    public HtNonVar getValue(IFunctor propertyName) {
        return null;
    }

    /**
     * @param propertyName
     * @param value
     */
    public void setValue(IFunctor propertyName, HtNonVar value) {

    }

    public Map<String, HtMethodDef> getMethodMap() {
        return null;
    }

    public Map<String, HtProperty> getPropMap() {
        return null;
    }
}