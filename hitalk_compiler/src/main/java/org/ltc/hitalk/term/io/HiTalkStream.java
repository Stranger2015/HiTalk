package org.ltc.hitalk.term.io;

import com.thesett.aima.logic.fol.Term;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.ltc.hitalk.compiler.bktables.Flag;
import org.ltc.hitalk.entities.IProperty;
import org.ltc.hitalk.entities.IPropertyOwner;
import org.ltc.hitalk.wam.compiler.HtTokenSource;
import sun.nio.cs.StreamDecoder;
import sun.nio.cs.StreamEncoder;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static java.nio.channels.Channels.newChannel;
import static java.nio.charset.Charset.*;

/**
 * JAVA NIO: Channels Files etc
 * open(+SrcDest, +Mode, --Stream)
 * ===============================
 * //convert byte buffer in given charset to char buffer in unicode
 * ByteBuffer bb = ByteBuffer.wrap(s.getBytes());
 * CharBuffer cb = charset.decode(bb);
 * //convert char buffer in unicode to byte buffer in given charset
 * ByteBuffer newbb = charset.encode(cb);
 */
public
class HiTalkStream extends FileChannel implements IPropertyOwner, PropertyChangeListener {
    private final FileChannel channel;
    private final HtTokenSource tokenSource;

    protected IProperty[] props;
    //    private ByteBuffer bbuf = ByteBuffer.allocate(8192);
    private Charset currentCharset = defaultCharset();// current cs is be saved in read manager due to include functionality
    protected StreamDecoder sd;
    protected StreamEncoder se;
    protected boolean isReading;
    private final List <PropertyChangeListener> listeners = new ArrayList <>();

    public
    HiTalkStream ( Path path, String encoding, long offset, StandardOpenOption... options ) throws IOException {
        channel = open(path, options);
        FileSystemManager manager = VFS.getManager();
        Charset charset = isSupported(encoding) ? forName(encoding) : defaultCharset();//currentCharset;
        if (charset.equals(currentCharset)) {
            offset = 0L;
        }
        EnumSet <StandardOpenOption> optionEnumSet = EnumSet.noneOf(StandardOpenOption.class);
        optionEnumSet.addAll(Arrays.asList(options));
        FileObject file = manager.resolveFile(path.toUri());
        InputStream input = file.getContent().getInputStream();
    }

    public
    HiTalkStream ( Path path, long offset, StandardOpenOption... options ) throws IOException {
        this(path, defaultCharset().name(), offset, options);
    }

    public
    HiTalkStream ( InputStream inputStream ) {
        channel = (FileChannel) newChannel(inputStream);
    }

    /**
     * @param path
     * @param options
     * @throws IOException
     */
    public
    HiTalkStream ( Path path, StandardOpenOption... options ) throws IOException {
        this(path, 0L, options);
    }

    public
    HiTalkStream ( InputStream inputStream, HtTokenSource tokenSource ) {
        this(inputStream);
        this.tokenSource = tokenSource;
    }

    /**
     * @return
     */
    @Override
    public
    int getPropLength () {
        return props.length;
    }

    @Override
    public
    void addListener ( PropertyChangeListener listener ) {
        listeners.add(listener);
    }

    @Override
    public
    void removeListener ( PropertyChangeListener listener ) {
        listeners.remove(listener);
    }

    @Override
    public
    void fireEvent ( IProperty property, Term newValue ) {
        for (PropertyChangeListener listener : listeners) {
            listener.propertyChange(new PropertyChangeEvent(property,
                    property.getName(),
                    property.getValue(),
                    newValue
            ));
        }
    }

    /**
     * @param property
     * @return
     */
    public
    Term getValue ( Properties property ) {
        return props[property.ordinal()].getValue();
    }

    public
    void setValue ( Properties property, Term value ) {
        if (!value.equals(getValue(property))) {//todo redundant
            props[property.ordinal()].setValue(value);
        }
    }

    public
    IProperty[] getProps () {
        return props;
    }

    /**
     * Closes this channel.
     *
     * <p> This method is invoked by the {@link #close close} method in order
     * to perform the actual work of closing the channel.  This method is only
     * invoked if the channel has not yet been closed, and it is never invoked
     * more than once.
     *
     * <p> An implementation of this method must arrange for any other thread
     * that is blocked in an I/O operation upon this channel to return
     * immediately, either by throwing an exception or by returning normally.
     * </p>
     *
     * @throws IOException If an I/O error occurs while closing the channel
     */
    @Override
    protected
    void implCloseChannel () throws IOException {
        if (!this.isOpen()) {
            throw new ClosedChannelException();
        }
        this.force(false);//fixme
        this.close();
    }

    /**
     * Reads a sequence of bytes from this channel into the given buffer.
     *
     * <p> Bytes are read starting at this channel's current file position, and
     * then the file position is updated with the number of bytes actually
     * read.  Otherwise this method behaves exactly as specified in the {@link
     * ReadableByteChannel} interface. </p>
     *
     * @param dst
     */
    @Override
    public
    int read ( ByteBuffer dst ) throws IOException {
        return channel.read(dst);
    }

    /**
     * Reads a sequence of bytes from this channel into a subsequence of the
     * given buffers.
     *
     * <p> Bytes are read starting at this channel's current file position, and
     * then the file position is updated with the number of bytes actually
     * read.  Otherwise this method behaves exactly as specified in the {@link
     * ScatteringByteChannel} interface.  </p>
     *
     * @param dsts
     * @param offset
     * @param length
     */
    @Override
    public
    long read ( ByteBuffer[] dsts, int offset, int length ) throws IOException {
        return 0;
    }

//    /**
//     * Reads a sequence of bytes from this channel into a subsequence of the
//     * given buffers.
//     *
//     * <p> Bytes are read starting at this channel's current file position, and
//     * then the file position is updated with the number of bytes actually
//     * read.  Otherwise this method behaves exactly as specified in the {@link
//     * ScatteringByteChannel} interface.  </p>
//     *
//     * @param dsts
//     * @param offset
//     * @param length
////     */
////    @Override
////    public
////    long read ( ByteBuffer[] dsts, int offset, int length ) throws IOException {
////        return 0;
////    }

    /**
     * Writes a sequence of bytes to this channel from the given buffer.
     *
     * <p> Bytes are written starting at this channel's current file position
     * unless the channel is in append mode, in which case the position is
     * first advanced to the end of the file.  The file is grown, if necessary,
     * to accommodate the written bytes, and then the file position is updated
     * with the number of bytes actually written.  Otherwise this method
     * behaves exactly as specified by the {@link WritableByteChannel}
     * interface. </p>
     *
     * @param src
     */
    @Override
    public
    int write ( ByteBuffer src ) throws IOException {
        return channel.write(src);
    }

    /**
     * Writes a sequence of bytes to this channel from a subsequence of the
     * given buffers.
     *
     * <p> Bytes are written starting at this channel's current file position
     * unless the channel is in append mode, in which case the position is
     * first advanced to the end of the file.  The file is grown, if necessary,
     * to accommodate the written bytes, and then the file position is updated
     * with the number of bytes actually written.  Otherwise this method
     * behaves exactly as specified in the {@link GatheringByteChannel}
     * interface.  </p>
     *
     * @param srcs
     * @param offset
     * @param length
     */
    @Override
    public
    long write ( ByteBuffer[] srcs, int offset, int length ) throws IOException {
        return 0;
    }

    /**
     * Returns this channel's file position.
     *
     * @return This channel's file position,
     * a non-negative integer counting the number of bytes
     * from the beginning of the file to the current position
     * @throws ClosedChannelException If this channel is closed
     * @throws IOException            If some other I/O error occurs
     */
    @Override
    public
    long position () throws IOException {
        return channel.position();
    }

    /**
     * Sets this channel's file position.
     *
     * <p> Setting the position to a value that is greater than the file's
     * current size is legal but does not change the size of the file.  A later
     * attempt to read bytes at such a position will immediately return an
     * end-of-file indication.  A later attempt to write bytes at such a
     * position will cause the file to be grown to accommodate the new bytes;
     * the values of any bytes between the previous end-of-file and the
     * newly-written bytes are unspecified.  </p>
     *
     * @param newPosition The new position, a non-negative integer counting
     *                    the number of bytes from the beginning of the file
     * @return This file channel
     * @throws ClosedChannelException   If this channel is closed
     * @throws IllegalArgumentException If the new position is negative
     * @throws IOException              If some other I/O error occurs
     */
    @Override
    public
    FileChannel position ( long newPosition ) throws IOException {
        return channel.position(newPosition);
    }

    /**
     * Returns the current size of this channel's file.
     *
     * @return The current size of this channel's file,
     * measured in bytes
     * @throws ClosedChannelException If this channel is closed
     * @throws IOException            If some other I/O error occurs
     */
    @Override
    public
    long size () throws IOException {
        return channel.size();
    }

    /**
     * Truncates this channel's file to the given size.
     *
     * <p> If the given size is less than the file's current size then the file
     * is truncated, discarding any bytes beyond the new end of the file.  If
     * the given size is greater than or equal to the file's current size then
     * the file is not modified.  In either case, if this channel's file
     * position is greater than the given size then it is set to that size.
     * </p>
     *
     * @param size The new size, a non-negative byte count
     * @return This file channel
     * @throws NonWritableChannelException If this channel was not opened for writing
     * @throws ClosedChannelException      If this channel is closed
     * @throws IllegalArgumentException    If the new size is negative
     * @throws IOException                 If some other I/O error occurs
     */
    @Override
    public
    FileChannel truncate ( long size ) throws IOException {
        return channel.truncate(size);
    }

    /**
     * Forces any updates to this channel's file to be written to the storage
     * device that contains it.
     *
     * <p> If this channel's file resides on a local storage device then when
     * this method returns it is guaranteed that all changes made to the file
     * since this channel was created, or since this method was last invoked,
     * will have been written to that device.  This is useful for ensuring that
     * critical information is not lost in the event of a system crash.
     *
     * <p> If the file does not reside on a local device then no such guarantee
     * is made.
     *
     * <p> The <tt>metaData</tt> parameter can be used to limit the number of
     * I/O operations that this method is required to perform.  Passing
     * <tt>false</tt> for this parameter indicates that only updates to the
     * file's content need be written to storage; passing <tt>true</tt>
     * indicates that updates to both the file's content and metadata must be
     * written, which generally requires at least one more I/O operation.
     * Whether this parameter actually has any effect is dependent upon the
     * underlying operating system and is therefore unspecified.
     *
     * <p> Invoking this method may cause an I/O operation to occur even if the
     * channel was only opened for reading.  Some operating systems, for
     * example, maintain a last-access time as part of a file's metadata, and
     * this time is updated whenever the file is read.  Whether or not this is
     * actually done is system-dependent and is therefore unspecified.
     *
     * <p> This method is only guaranteed to force changes that were made to
     * this channel's file via the methods defined in this class.  It may or
     * may not force changes that were made by modifying the content of a
     * {@link MappedByteBuffer <i>mapped byte buffer</i>} obtained by
     * invoking the {@link #map map} method.  Invoking the {@link
     * MappedByteBuffer#force force} method of the mapped byte buffer will
     * force changes made to the buffer's content to be written.  </p>
     *
     * @param metaData If <tt>true</tt> then this method is required to force changes
     *                 to both the file's content and metadata to be written to
     *                 storage; otherwise, it need only force content changes to be
     *                 written
     * @throws ClosedChannelException If this channel is closed
     * @throws IOException            If some other I/O error occurs
     */
    @Override
    public
    void force ( boolean metaData ) throws IOException {
        channel.force(metaData);
    }

    /**
     * Transfers bytes from this channel's file to the given writable byte
     * channel.
     *
     * <p> An attempt is made to read up to <tt>count</tt> bytes starting at
     * the given <tt>position</tt> in this channel's file and write them to the
     * target channel.  An invocation of this method may or may not transfer
     * all of the requested bytes; whether or not it does so depends upon the
     * natures and states of the channels.  Fewer than the requested number of
     * bytes are transferred if this channel's file contains fewer than
     * <tt>count</tt> bytes starting at the given <tt>position</tt>, or if the
     * target channel is non-blocking and it has fewer than <tt>count</tt>
     * bytes free in its output buffer.
     *
     * <p> This method does not modify this channel's position.  If the given
     * position is greater than the file's current size then no bytes are
     * transferred.  If the target channel has a position then bytes are
     * written starting at that position and then the position is incremented
     * by the number of bytes written.
     *
     * <p> This method is potentially much more efficient than a simple loop
     * that reads from this channel and writes to the target channel.  Many
     * operating systems can transfer bytes directly from the filesystem cache
     * to the target channel without actually copying them.  </p>
     *
     * @param position The position within the file at which the transfer is to begin;
     *                 must be non-negative
     * @param count    The maximum number of bytes to be transferred; must be
     *                 non-negative
     * @param target   The target channel
     * @return The number of bytes, possibly zero,
     * that were actually transferred
     * @throws IllegalArgumentException    If the preconditions on the parameters do not hold
     * @throws NonReadableChannelException If this channel was not opened for reading
     * @throws NonWritableChannelException If the target channel was not opened for writing
     * @throws ClosedChannelException      If either this channel or the target channel is closed
     * @throws AsynchronousCloseException  If another thread closes either channel
     *                                     while the transfer is in progress
     * @throws ClosedByInterruptException  If another thread interrupts the current thread while the
     *                                     transfer is in progress, thereby closing both channels and
     *                                     setting the current thread's interrupt status
     * @throws IOException                 If some other I/O error occurs
     */
    @Override
    public
    long transferTo ( long position, long count, WritableByteChannel target ) throws IOException {
        return channel.transferTo(position, count, target);
    }

    /**
     * Transfers bytes into this channel's file from the given readable byte
     * channel.
     *
     * <p> An attempt is made to read up to <tt>count</tt> bytes from the
     * source channel and write them to this channel's file starting at the
     * given <tt>position</tt>.  An invocation of this method may or may not
     * transfer all of the requested bytes; whether or not it does so depends
     * upon the natures and states of the channels.  Fewer than the requested
     * number of bytes will be transferred if the source channel has fewer than
     * <tt>count</tt> bytes remaining, or if the source channel is non-blocking
     * and has fewer than <tt>count</tt> bytes immediately available in its
     * input buffer.
     *
     * <p> This method does not modify this channel's position.  If the given
     * position is greater than the file's current size then no bytes are
     * transferred.  If the source channel has a position then bytes are read
     * starting at that position and then the position is incremented by the
     * number of bytes read.
     *
     * <p> This method is potentially much more efficient than a simple loop
     * that reads from the source channel and writes to this channel.  Many
     * operating systems can transfer bytes directly from the source channel
     * into the filesystem cache without actually copying them.  </p>
     *
     * @param src      The source channel
     * @param position The position within the file at which the transfer is to begin;
     *                 must be non-negative
     * @param count    The maximum number of bytes to be transferred; must be
     *                 non-negative
     * @return The number of bytes, possibly zero,
     * that were actually transferred
     * @throws IllegalArgumentException    If the preconditions on the parameters do not hold
     * @throws NonReadableChannelException If the source channel was not opened for reading
     * @throws NonWritableChannelException If this channel was not opened for writing
     * @throws ClosedChannelException      If either this channel or the source channel is closed
     * @throws AsynchronousCloseException  If another thread closes either channel
     *                                     while the transfer is in progress
     * @throws ClosedByInterruptException  If another thread interrupts the current thread while the
     *                                     transfer is in progress, thereby closing both channels and
     *                                     setting the current thread's interrupt status
     * @throws IOException                 If some other I/O error occurs
     */
    @Override
    public
    long transferFrom ( ReadableByteChannel src, long position, long count ) throws IOException {
        return channel.transferFrom(src, position, count);
    }

    /**
     * Reads a sequence of bytes from this channel into the given buffer,
     * starting at the given file position.
     *
     * <p> This method works in the same manner as the {@link
     * #read(ByteBuffer)} method, except that bytes are read starting at the
     * given file position rather than at the channel's current position.  This
     * method does not modify this channel's position.  If the given position
     * is greater than the file's current size then no bytes are read.  </p>
     *
     * @param dst      The buffer into which bytes are to be transferred
     * @param position The file position at which the transfer is to begin;
     *                 must be non-negative
     * @return The number of bytes read, possibly zero, or <tt>-1</tt> if the
     * given position is greater than or equal to the file's current
     * size
     * @throws IllegalArgumentException    If the position is negative
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
    @Override
    public
    int read ( ByteBuffer dst, long position ) throws IOException {
        return channel.read(dst, position);
    }

    /**
     * Writes a sequence of bytes to this channel from the given buffer,
     * starting at the given file position.
     *
     * <p> This method works in the same manner as the {@link
     * #write(ByteBuffer)} method, except that bytes are written starting at
     * the given file position rather than at the channel's current position.
     * This method does not modify this channel's position.  If the given
     * position is greater than the file's current size then the file will be
     * grown to accommodate the new bytes; the values of any bytes between the
     * previous end-of-file and the newly-written bytes are unspecified.  </p>
     *
     * @param src      The buffer from which bytes are to be transferred
     * @param position The file position at which the transfer is to begin;
     *                 must be non-negative
     * @return The number of bytes written, possibly zero
     * @throws IllegalArgumentException    If the position is negative
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
    @Override
    public
    int write ( ByteBuffer src, long position ) throws IOException {
        return channel.write(src, position);
    }

    /**
     * Maps a region of this channel's file directly into memory.
     *
     * <p> A region of a file may be mapped into memory in one of three modes:
     * </p>
     *
     * <ul>
     *
     *   <li><p> <i>Read-only:</i> Any attempt to modify the resulting buffer
     *   will cause a {@link ReadOnlyBufferException} to be thrown.
     *   ({@link MapMode#READ_ONLY MapMode.READ_ONLY}) </p></li>
     *
     *   <li><p> <i>Read/write:</i> Changes made to the resulting buffer will
     *   eventually be propagated to the file; they may or may not be made
     *   visible to other programs that have mapped the same file.  ({@link
     *   MapMode#READ_WRITE MapMode.READ_WRITE}) </p></li>
     *
     *   <li><p> <i>Private:</i> Changes made to the resulting buffer will not
     *   be propagated to the file and will not be visible to other programs
     *   that have mapped the same file; instead, they will cause private
     *   copies of the modified portions of the buffer to be created.  ({@link
     *   MapMode#PRIVATE MapMode.PRIVATE}) </p></li>
     *
     * </ul>
     *
     * <p> For a read-only mapping, this channel must have been opened for
     * reading; for a read/write or private mapping, this channel must have
     * been opened for both reading and writing.
     *
     * <p> The {@link MappedByteBuffer <i>mapped byte buffer</i>}
     * returned by this method will have a position of zero and a limit and
     * capacity of <tt>size</tt>; its mark will be undefined.  The buffer and
     * the mapping that it represents will remain valid until the buffer itself
     * is garbage-collected.
     *
     * <p> A mapping, once established, is not dependent upon the file channel
     * that was used to create it.  Closing the channel, in particular, has no
     * effect upon the validity of the mapping.
     *
     * <p> Many of the details of memory-mapped files are inherently dependent
     * upon the underlying operating system and are therefore unspecified.  The
     * behavior of this method when the requested region is not completely
     * contained within this channel's file is unspecified.  Whether changes
     * made to the content or size of the underlying file, by this program or
     * another, are propagated to the buffer is unspecified.  The rate at which
     * changes to the buffer are propagated to the file is unspecified.
     *
     * <p> For most operating systems, mapping a file into memory is more
     * expensive than reading or writing a few tens of kilobytes of data via
     * the usual {@link #read read} and {@link #write write} methods.  From the
     * standpoint of performance it is generally only worth mapping relatively
     * large files into memory.  </p>
     *
     * @param mode     One of the constants {@link MapMode#READ_ONLY READ_ONLY}, {@link
     *                 MapMode#READ_WRITE READ_WRITE}, or {@link MapMode#PRIVATE
     *                 PRIVATE} defined in the {@link MapMode} class, according to
     *                 whether the file is to be mapped read-only, read/write, or
     *                 privately (copy-on-write), respectively
     * @param position The position within the file at which the mapped region
     *                 is to start; must be non-negative
     * @param size     The size of the region to be mapped; must be non-negative and
     *                 no greater than {@link Integer#MAX_VALUE}
     * @return The mapped byte buffer
     * @throws NonReadableChannelException If the <tt>mode</tt> is {@link MapMode#READ_ONLY READ_ONLY} but
     *                                     this channel was not opened for reading
     * @throws NonWritableChannelException If the <tt>mode</tt> is {@link MapMode#READ_WRITE READ_WRITE} or
     *                                     {@link MapMode#PRIVATE PRIVATE} but this channel was not opened
     *                                     for both reading and writing
     * @throws IllegalArgumentException    If the preconditions on the parameters do not hold
     * @throws IOException                 If some other I/O error occurs
     * @see MapMode
     * @see MappedByteBuffer
     */
    @Override
    public
    MappedByteBuffer map ( MapMode mode, long position, long size ) throws IOException {
        return channel.map(mode, position, size);
    }

    /**
     * Acquires a lock on the given region of this channel's file.
     *
     * <p> An invocation of this method will block until the region can be
     * locked, this channel is closed, or the invoking thread is interrupted,
     * whichever comes first.
     *
     * <p> If this channel is closed by another thread during an invocation of
     * this method then an {@link AsynchronousCloseException} will be thrown.
     *
     * <p> If the invoking thread is interrupted while waiting to acquire the
     * lock then its interrupt status will be set and a {@link
     * FileLockInterruptionException} will be thrown.  If the invoker's
     * interrupt status is set when this method is invoked then that exception
     * will be thrown immediately; the thread's interrupt status will not be
     * changed.
     *
     * <p> The region specified by the <tt>position</tt> and <tt>size</tt>
     * parameters need not be contained within, or even overlap, the actual
     * underlying file.  Lock regions are fixed in size; if a locked region
     * initially contains the end of the file and the file grows beyond the
     * region then the new portion of the file will not be covered by the lock.
     * If a file is expected to grow in size and a lock on the entire file is
     * required then a region starting at zero, and no smaller than the
     * expected maximum size of the file, should be locked.  The zero-argument
     * {@link #lock()} method simply locks a region of size {@link
     * Long#MAX_VALUE}.
     *
     * <p> Some operating systems do not support shared locks, in which case a
     * request for a shared lock is automatically converted into a request for
     * an exclusive lock.  Whether the newly-acquired lock is shared or
     * exclusive may be tested by invoking the resulting lock object's {@link
     * FileLock#isShared() isShared} method.
     *
     * <p> File locks are held on behalf of the entire Java virtual machine.
     * They are not suitable for controlling access to a file by multiple
     * threads within the same virtual machine.  </p>
     *
     * @param position The position at which the locked region is to start; must be
     *                 non-negative
     * @param size     The size of the locked region; must be non-negative, and the sum
     *                 <tt>position</tt>&nbsp;+&nbsp;<tt>size</tt> must be non-negative
     * @param shared   <tt>true</tt> to request a shared lock, in which case this
     *                 channel must be open for reading (and possibly writing);
     *                 <tt>false</tt> to request an exclusive lock, in which case this
     *                 channel must be open for writing (and possibly reading)
     * @return A lock object representing the newly-acquired lock
     * @throws IllegalArgumentException      If the preconditions on the parameters do not hold
     * @throws ClosedChannelException        If this channel is closed
     * @throws AsynchronousCloseException    If another thread closes this channel while the invoking
     *                                       thread is blocked in this method
     * @throws FileLockInterruptionException If the invoking thread is interrupted while blocked in this
     *                                       method
     * @throws OverlappingFileLockException  If a lock that overlaps the requested region is already held by
     *                                       this Java virtual machine, or if another thread is already
     *                                       blocked in this method and is attempting to lock an overlapping
     *                                       region
     * @throws NonReadableChannelException   If <tt>shared</tt> is <tt>true</tt> this channel was not
     *                                       opened for reading
     * @throws NonWritableChannelException   If <tt>shared</tt> is <tt>false</tt> but this channel was not
     *                                       opened for writing
     * @throws IOException                   If some other I/O error occurs
     * @see #lock()
     * @see #tryLock()
     * @see #tryLock(long, long, boolean)
     */
    @Override
    public
    FileLock lock ( long position, long size, boolean shared ) throws IOException {
        return channel.lock(position, size, shared);
    }

    /**
     * Attempts to acquire a lock on the given region of this channel's file.
     *
     * <p> This method does not block.  An invocation always returns
     * immediately, either having acquired a lock on the requested region or
     * having failed to do so.  If it fails to acquire a lock because an
     * overlapping lock is held by another program then it returns
     * <tt>null</tt>.  If it fails to acquire a lock for any other reason then
     * an appropriate exception is thrown.
     *
     * <p> The region specified by the <tt>position</tt> and <tt>size</tt>
     * parameters need not be contained within, or even overlap, the actual
     * underlying file.  Lock regions are fixed in size; if a locked region
     * initially contains the end of the file and the file grows beyond the
     * region then the new portion of the file will not be covered by the lock.
     * If a file is expected to grow in size and a lock on the entire file is
     * required then a region starting at zero, and no smaller than the
     * expected maximum size of the file, should be locked.  The zero-argument
     * {@link #tryLock()} method simply locks a region of size {@link
     * Long#MAX_VALUE}.
     *
     * <p> Some operating systems do not support shared locks, in which case a
     * request for a shared lock is automatically converted into a request for
     * an exclusive lock.  Whether the newly-acquired lock is shared or
     * exclusive may be tested by invoking the resulting lock object's {@link
     * FileLock#isShared() isShared} method.
     *
     * <p> File locks are held on behalf of the entire Java virtual machine.
     * They are not suitable for controlling access to a file by multiple
     * threads within the same virtual machine.  </p>
     *
     * @param position The position at which the locked region is to start; must be
     *                 non-negative
     * @param size     The size of the locked region; must be non-negative, and the sum
     *                 <tt>position</tt>&nbsp;+&nbsp;<tt>size</tt> must be non-negative
     * @param shared   <tt>true</tt> to request a shared lock,
     *                 <tt>false</tt> to request an exclusive lock
     * @return A lock object representing the newly-acquired lock,
     * or <tt>null</tt> if the lock could not be acquired
     * because another program holds an overlapping lock
     * @throws IllegalArgumentException     If the preconditions on the parameters do not hold
     * @throws ClosedChannelException       If this channel is closed
     * @throws OverlappingFileLockException If a lock that overlaps the requested region is already held by
     *                                      this Java virtual machine, or if another thread is already
     *                                      blocked in this method and is attempting to lock an overlapping
     *                                      region of the same file
     * @throws IOException                  If some other I/O error occurs
     * @see #lock()
     * @see #lock(long, long, boolean)
     * @see #tryLock()
     */
    @Override
    public
    FileLock tryLock ( long position, long size, boolean shared ) throws IOException {
        return channel.tryLock(position, size, shared);
    }

    /**
     * Forces any updates to this channel's file to be written to the storage
     * device that contains it.
     *
     * <p> If this channel's file resides on a local storage device then when
     * this method returns it is guaranteed that all changes made to the file
     * since this channel was created, or since this method was last invoked,
     * will have been written to that device.  This is useful for ensuring that
     * critical information is not lost in the event of a system crash.
     *
     * <p> If the file does not reside on a local device then no such guarantee
     * is made.
     *
     * <p> The <tt>metaData</tt> parameter can be used to limit the number of
     * I/O operations that this method is required to perform.  Passing
     * <tt>false</tt> for this parameter indicates that only updates to the
     * file's content need be written to storage; passing <tt>true</tt>
     * indicates that updates to both the file's content and metadata must be
     * written, which generally requires at least one more I/O operation.
     * Whether this parameter actually has any effect is dependent upon the
     * underlying operating system and is therefore unspecified.
     *
     * <p> Invoking this method may cause an I/O operation to occur even if the
     * channel was only opened for reading.  Some operating systems, for
     * example, maintain a last-access time as part of a file's metadata, and
     * this time is updated whenever the file is read.  Whether or not this is
     * actually done is system-dependent and is therefore unspecified.
     *
     * <p> This method is only guaranteed to force changes that were made to
     * this channel's file via the methods defined in this class.  It may or
     * may not force changes that were made by modifying the content of a
     * {@link MappedByteBuffer <i>mapped byte buffer</i>} obtained by
     * invoking the {@link # map} method.  Invoking the {@link
     * MappedByteBuffer#force force} method of the mapped byte buffer will
     * force changes made to the buffer's content to be written.  </p>
     *
     * @param metaData If <tt>true</tt> then this method is required to force changes
     *                 to both the file's content and metadata to be written to
     *                 storage; otherwise, it need only force content changes to be
     *                 written
     * @throws ClosedChannelException If this channel is closed
     * @throws IOException            If some other I/O error occurs
     */
////    @Override
//    public
//    void force ( boolean metaData ) throws IOException {
//        channel.force(metaData);
//    }

    /**
     * Retrieves and removes the head of this queue, or <tt>null</tt> if this queue is empty.
     *
     * @return The head of this queue, or <tt>null</tt> if this queue is empty.
     */
    /**
     * @return
     */
    @Override
    public
    Flag[] getFlags () {
        return new Flag[0];
    }

    /**
     * This method gets called when a bound property is changed.
     *
     * @param event A PropertyChangeEvent object describing the event source
     *            and the property that has changed.
     */
    @Override
    public
    void propertyChange ( PropertyChangeEvent event ) {
        switch ((Properties) event.getSource()) {
            case alias:

                break;
            case buffer:
                break;
            case buffer_size:
                break;
            case bom:
                break;
            case close_on_abort:
                break;
            case close_on_exec:
                break;
            case encoding:
                Term term = props[Properties.encoding.ordinal()].getValue();
                break;
            case end_of_stream:
                break;
            case bof_action:
                break;
            case eof_action:
                break;
            case file_name:
                break;
            case file_no:
                break;
            case input:
                break;
            case locale:
                break;
            case mode:
                break;
            case newline:
                break;
            case nlink:
                break;
            case output:
                break;
            case position:
                break;
            case reposition:
                break;
            case representation_errors:
                break;
            case timeout:
                break;
            case type:
                break;
            case tty:
                break;
            case write_errors:
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + event.getSource());
        }
    }

    /**
     * @param name
     * @param oldValue
     * @param newValue
     */

    /**
     *
     */
    public
    enum Properties {
        alias, //        alias(Atom)
        buffer,//    buffer(Buffering)full, line or false
        buffer_size,//buffer_size(Integer)
        bom,//bom( Bool )
        close_on_abort,//close_on_abort(Bool)
        close_on_exec,
        encoding,//encoding(Encoding)
        end_of_stream,//end_of_stream(E)
        bof_action,
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
    }
}