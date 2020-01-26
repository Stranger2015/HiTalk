package org.ltc.hitalk.term.io;

import org.jetbrains.annotations.NotNull;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.term.HtNonVar;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.nio.CharBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import static java.nio.file.StandardOpenOption.READ;
import static org.apache.commons.lang3.CharEncoding.UTF_8;

/**
 *
 */
public class HiTalkInputStream extends HiTalkStream implements Readable {

    public final static int defaultBufSize = 8192;
    public static final String defaultEncoding = UTF_8;

//    private final int bufferSize;

    protected FileInputStream inputStream;
    protected PushbackReader pushbackReader;
    private int bof;
    private PlLexer tokenSource;

    private int reads;
    private int lineNumber;
    private int colNumber;
    private LineNumberReader reader;

    /**
     * Creates a buffering character-input stream that uses a default-sized
     * input buffer.
     *
     * @param encoding
     * @param path
     */
    public HiTalkInputStream(Path path, String encoding) throws Exception {
        super(path, encoding, READ);
        final FileInputStream fis = new FileInputStream(path.toFile());
        setInputStream(fis);
    }

    /**
     * @param fd
     * @param bufferSize
     * @throws IOException
     */
    public HiTalkInputStream(FileDescriptor fd, int bufferSize) throws Exception {
        super(fd);
    }

    /**
     * @param path
     * @param bufferSize
     * @throws IOException
     */
    public HiTalkInputStream(String path, int bufferSize) throws Exception {
        super(Paths.get(path), defaultEncoding, READ);
    }

    /**
     * @param file
     * @param bufferSize
     * @throws FileNotFoundException
     */
    public HiTalkInputStream(File file, int bufferSize) throws Exception {
        this(file.getAbsolutePath(), bufferSize);
        setInputStream(new FileInputStream(file));
    }

    /**
     * @param file
     * @throws FileNotFoundException
     */
    public HiTalkInputStream(File file) throws Exception {
        this(file, defaultBufSize);
    }

    /**
     * @param string
     * @throws FileNotFoundException
     */
    public HiTalkInputStream(String string) throws Exception {
        this(new File(string), defaultBufSize);
        final FileInputStream fis = new FileInputStream(string);
        setInputStream(fis);
    }

    public HiTalkInputStream(Path path, String s, StandardOpenOption read) throws Exception {
        super(path, s, read);
        setInputStream(new FileInputStream(path.toFile()));
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
    public int read(@NotNull CharBuffer cb) throws IOException {
        return -1;
    }

    /**
     * Read the byte from input stream
     *
     * @return
     * @throws IOException
     */
    public int read() throws IOException {
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
    public void unread(int c) throws IOException {
        if (reads-- < 0) {
            throw new IllegalStateException("reads == " + reads);
        }
        pushbackReader.unread(c);
    }

    /**
     * @param tokenSource
     */
    public void setTokenSource(PlLexer tokenSource) {
        this.tokenSource = tokenSource;
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
    public void close() throws IOException {
        super.close();
        inputStream.close();
        isOpen = false;
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
     * @throws IOException if an I/O error occurs.
     */
    public void readLine() throws IOException {
        setColNumber(0);
        reader.readLine();
    }

    public LineNumberReader getReader() {
        return reader;
    }

    /**
     * @param inputStream
     */
    public void setInputStream(FileInputStream inputStream) {
        this.inputStream = inputStream;
        channel = inputStream.getChannel();
        reader = new LineNumberReader(new BufferedReader(new InputStreamReader(inputStream), defaultBufSize));
        pushbackReader = new PushbackReader(reader, 4);
    }

    /**
     * @return
     */
    public FileInputStream getInputStream() {
        return inputStream;
    }

    protected void doOpen() throws FileNotFoundException {
        if (getInputStream() == null) {
            setInputStream(new FileInputStream(tokenSource.getPath()));
        }
//        IVafInterner interner= BaseApp.appContext.getInterner();
//        String fn = interner.getFunctorName(getPropMap().get("file_name").getValue());
        isOpen = true;
    }

    @Override
    protected void init(FileDescriptor fd) {
        setInputStream(new FileInputStream(fd));
    }

    /**
     * This method gets called when a bound property is changed.
     *
     * @param evt A PropertyChangeEvent object describing the event source
     *            and the property that has changed.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);
    }

    /**
     * @return
     */
    @Override
    public int getPropLength() {
        return owner.getPropLength();
    }

    /**
     * @param listener
     */
    @Override
    public void addListener(PropertyChangeListener listener) {
        super.addListener(listener);
    }

    /**
     * @param listener
     */
    @Override
    public void removeListener(PropertyChangeListener listener) {
        super.removeListener(listener);
    }

    /**
     * @param propertyName
     * @param value
     */
    @Override
    public void setValue(IFunctor propertyName, HtNonVar value) {
        owner.setValue(propertyName, value);
    }

    /**
     * @return
     */
    @Override
    public HtProperty[] getProps() {
        return owner.getProps();
    }

    @Override
    public HtMethodDef[] getMethods() {
        return owner.getMethods();
    }

    public Map<String, HtMethodDef> getMethodMap() {
        return owner.getMethodMap();
    }

    public Map<String, HtProperty> getPropMap() {
        return owner.getPropMap();
    }

    @Override
    public HtNonVar getValue(IFunctor property) {
        return owner.getValue(property);
    }

    //@Override
    public HiTalkStream copy() {
        return null;//FIXME
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getColNumber() {
        return colNumber;
    }

    public void setColNumber(int colNumber) {
        this.colNumber = colNumber;
    }

    public boolean isBOFNotPassed() {
        return bof++ == 0;
    }

    public PlLexer getTokenSource() {
        return tokenSource;
    }

    public void toString0(StringBuilder sb) {
        super.toString0(sb);
        sb.append(", inputStream=").append(inputStream);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final HiTalkInputStream that = (HiTalkInputStream) o;

        return getInputStream().equals(that.getInputStream());
    }

    public int hashCode() {
        return getInputStream().hashCode();
    }
}
