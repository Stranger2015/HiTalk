package org.ltc.hitalk.term.io;

import org.jetbrains.annotations.NotNull;
import org.ltc.hitalk.entities.IProperty;
import org.ltc.hitalk.parser.PlTokenSource;
import org.ltc.hitalk.term.ITerm;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.nio.CharBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 *
 */
public class HiTalkInputStream extends HiTalkStream implements Readable {

    public final static int defaultBufSize = 8192;

//    private final int bufferSize;

    protected FileInputStream inputStream;
    protected PushbackReader pushbackReader;
    private int bof;
    private PlTokenSource tokenSource;

    private int reads;
    private int lineNumber;
    private int colNumber;
    private LineNumberReader reader;

    /**
     * Creates a buffering character-input stream that uses a default-sized
     * input buffer.
     *
     * @param bufSize
     * @param path
     */
    public HiTalkInputStream ( Path path, int bufSize ) throws FileNotFoundException {
        final FileInputStream fis = new FileInputStream(path.toFile());
        setInputStream(fis);
        /*setTokenSource(tokenSource);*/
    }

//    /**
//     * @param path
//     * @param tokenSource
//     * @param bufferSize
//     * @throws IOException
//     */
//    public HiTalkInputStream ( Path path, PlTokenSource tokenSource, int bufferSize ) throws IOException {
//        this(bufferSize, path, tokenSource);
//    }

    public HiTalkInputStream ( FileDescriptor fd, /*PlTokenSource tokenSource, */int bufferSize ) {
//        this.bufferSize = bufferSize;
        this.fd = fd;
        final FileInputStream fis = new FileInputStream(fd);
        setInputStream(fis);
//        setTokenSource(tokenSource);
    }

    public HiTalkInputStream ( String path, /*PlTokenSource tokenSource, */int bufferSize ) throws IOException {
        this(Paths.get(path),/* tokenSource,*/ bufferSize);
    }

    public HiTalkInputStream ( File file, int bufferSize ) throws FileNotFoundException {
//        setTokenSource(tokenSource);
//     /**/   this.bufferSize = bufferSize;
        setInputStream(new FileInputStream(file));
    }

    public HiTalkInputStream ( File file ) throws FileNotFoundException {
        this(file, defaultBufSize);
    }

    public HiTalkInputStream ( String string ) {

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
        reads++;
        return pushbackReader.read();
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
        if (reads-- < 0) {
            throw new IllegalStateException("reads == " + reads);
        }
        pushbackReader.unread(c);
    }

    /**
     * @param tokenSource
     */
    public void setTokenSource ( PlTokenSource tokenSource ) {
        this.tokenSource = tokenSource;
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
        setColNumber(0);
        return reader.readLine();
    }

    /**
     * @param inputStream
     */
    public void setInputStream ( FileInputStream inputStream ) {
        this.inputStream = inputStream;
        channel = inputStream.getChannel();
        reader = new LineNumberReader(new BufferedReader(new InputStreamReader(inputStream), defaultBufSize));
        pushbackReader = new PushbackReader(reader, 4);
    }

    public FileInputStream getInputStream () {
        return inputStream;
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

    @Override
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

    public boolean isBOFNotPassed () {
        return bof++ == 0;
    }

    public PlTokenSource getTokenSource () {
        return tokenSource;
    }

    public void toString0 ( StringBuilder sb ) {
        super.toString0(sb);
        sb.append(", inputStream=").append(inputStream);
        sb.append(", pushbackReader=").append(pushbackReader);
        sb.append(", tokenSource=").append(tokenSource);
        sb.append(", reader=").append(reader);
    }
}
