/**
 * Copyright © 2018 - 2018 SSHTOOLS Limited (support@sshtools.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ltc.hitalk.com.sshtools.vfs2nio;

//import org.apache.commons.vfs2.FileSystem;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;
import java.util.concurrent.ExecutorService;

public
class Vfs2NioFileSystemProvider extends FileSystemProvider {
    public final static String FILE_SYSTEM_OPTIONS = "org.ltc.hitalk.com.sshtools.vfs2nio.fileSystemOptions";
    public final static String VFS_MANAGER = "org.ltc.hitalk.com.sshtools.vfs2nio.vfsManager";

    // Checks that the given file is a UnixPath
    static
    Vfs2NioPath toVFSPath ( Path path ) {
        Objects.requireNonNull(path);
        if (!(path instanceof Vfs2NioPath)) {
            throw new ProviderMismatchException();
        }
        return (Vfs2NioPath) path;
    }

    private final Map <URI, FileSystem> filesystems = Collections.synchronizedMap(new HashMap <>());

    public
    Vfs2NioFileSystemProvider () {
    }

    //	//@override
    public
    void checkAccess ( Path path, AccessMode... modes ) throws IOException {
        Vfs2NioPath p = toVFSPath(path);
        FileObject fo = p.toFileObject();
        for (AccessMode m : modes) {
            switch (m) {
                case EXECUTE:
                    if (!fo.isExecutable())
                        throw new AccessDeniedException(String.format("No %s access to %s", m, path));
                    break;
                case READ:
                    if (!fo.isReadable()) throw new AccessDeniedException(String.format("No %s access to %s", m, path));
                    break;
                case WRITE:
                    if (!fo.isWriteable())
                        throw new AccessDeniedException(String.format("No %s access to %s", m, path));
                    break;
                default:
                    break;
            }
        }
    }

    //@override
    public
    void copy ( Path src, Path target, CopyOption... options ) throws IOException {
        /*
         * TODO: Support REPLACE_EXISTING, COPY_ATTRIBUTES, ATOMIC_MOVE if
         * possible
         */
        toVFSPath(target).toFileObject().copyFrom(toVFSPath(src).toFileObject(), new AllFileSelector());
    }

    //@override
    public
    void createDirectory ( Path path, FileAttribute <?>... attrs ) throws IOException {
        /* TODO: Support attributes */
        Vfs2NioPath p = toVFSPath(path);
        checkAccess(p, AccessMode.WRITE);
        FileObject fo = p.toFileObject();
        fo.createFolder();
    }

    //@override
    public final
    void delete ( Path path ) throws IOException {
        Vfs2NioPath p = toVFSPath(path);
        checkAccess(p, AccessMode.WRITE);
        FileObject fo = p.toFileObject();
        fo.deleteAll();
    }

    //@override
    public
    <V extends FileAttributeView> V getFileAttributeView ( Path path, Class <V> type, LinkOption... options ) {
        return Vfs2NioFileAttributeView.get(toVFSPath(path), type);
    }

    //@override
    public
    FileStore getFileStore ( Path path ) throws IOException {
        return toVFSPath(path).getFileStore();
    }

    //@override
    public
    FileSystem getFileSystem ( URI uri ) {
        synchronized (filesystems) {
            FileSystem vfs = null;
            URI path = toFsUri(uri);
            vfs = filesystems.get(path);
            if (vfs == null)
                throw new FileSystemNotFoundException(String.format("Cannot find file system for %s", uri));
            return vfs;
        }
    }

    //@override
    public
    Path getPath ( URI uri ) {
        FileSystem fileSystem;
        try {
            fileSystem = getFileSystem(uri);
        } catch (FileSystemNotFoundException fsnfe) {
            try {
                fileSystem = newFileSystem(uri, new HashMap <>());
            } catch (IOException e) {
                throw new Vfs2NioException("Failed to create new file system.", e);
            }
        }
        return fileSystem.getPath(toFsUri(uri).getSchemeSpecificPart());
    }

    //@override
    public
    String getScheme () {
        return "vfs";
    }

    //@override
    public
    boolean isHidden ( Path path ) {
        try {
            return toVFSPath(path).toFileObject().isHidden();
        } catch (FileSystemException e) {
            return false;
        }
    }

    //@override
    public
    boolean isSameFile ( Path path, Path other ) throws IOException {
        return toVFSPath(path).toFileObject().equals(toVFSPath(other).toFileObject());
    }

    //@override
    public
    void move ( Path src, Path target, CopyOption... options ) throws IOException {
        toVFSPath(src).toFileObject().moveTo(toVFSPath(target).toFileObject());
    }

    //@override
    public
    AsynchronousFileChannel newAsynchronousFileChannel ( Path path, Set <? extends OpenOption> options, ExecutorService exec, FileAttribute <?>... attrs ) throws IOException {
        throw new UnsupportedOperationException();
    }

    //@override
    public
    SeekableByteChannel newByteChannel ( Path path, Set <? extends OpenOption> options, FileAttribute <?>... attrs ) throws IOException {
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Opens a directory, returning a {@code DirectoryStream} to iterate over
     * the entries in the directory. This method works in exactly the manner
     * specified by the {@link
     * Files#newDirectoryStream(Path, DirectoryStream.Filter)}
     * method.
     *
     * @param dir    the path to the directory
     * @param filter the directory stream filter
     * @return a new and open {@code DirectoryStream} object
     * @throws NotDirectoryException if the file could not otherwise be opened because it is not
     *                               a directory <i>(optional specific exception)</i>
     * @throws IOException           if an I/O error occurs
     * @throws SecurityException     In the case of the default provider, and a security manager is
     *                               installed, the {@link SecurityManager#checkRead(String) checkRead}
     *                               method is invoked to check read access to the directory.
     */
    @Override
    public
    DirectoryStream <Path> newDirectoryStream ( Path dir, DirectoryStream.Filter <? super Path> filter ) throws IOException {
        return new Vfs2NioDirectoryStream(toVFSPath(dir), filter);
    }

//	//@override
//	public DirectoryStream<Path> newDirectoryStream(Path path, DirectoryStream.Filter <? super Path> filter) throws IOException {
//		return new Vfs2NioDirectoryStream(toVFSPath(path), filter);
//	}

    //@override
    public
    FileChannel newFileChannel ( Path path, Set <? extends OpenOption> options, FileAttribute <?>... attrs ) throws IOException {
        // TODO
        throw new UnsupportedOperationException();
    }

    //@override
    public
    FileSystem newFileSystem ( Path path, Map <String, ?> env ) throws IOException {
        // TODO
        throw new UnsupportedOperationException();
    }

    //@override
    public
    FileSystem newFileSystem ( URI uri, Map <String, ?> env ) throws IOException {
        URI path = toFsUri(uri);
        if (filesystems.containsKey(path)) throw new FileSystemAlreadyExistsException();
        synchronized (filesystems) {
            FileSystemManager mgr = env == null ? null : (FileSystemManager) env.get(VFS_MANAGER);
            if (mgr == null) mgr = VFS.getManager();
            FileSystemOptions opts = env == null ? null : (FileSystemOptions) env.get(FILE_SYSTEM_OPTIONS);
            Vfs2NioFileSystem vfs = new Vfs2NioFileSystem(this, opts == null ? mgr.resolveFile(path) : mgr.resolveFile(path.toString(), opts), path);
            filesystems.put(path, vfs);
            return vfs;
        }
    }

    //@override
    public
    InputStream newInputStream ( Path path, OpenOption... options ) throws IOException {
        List <OpenOption> optlist = Arrays.asList(options);
        if (optlist.contains(StandardOpenOption.WRITE))
            throw new IllegalArgumentException(String.format("%s is not supported by this method.", StandardOpenOption.WRITE));
        checkAccess(path, AccessMode.READ);
        return toVFSPath(path).toFileObject().getContent().getInputStream();
    }

    //@override
    public
    OutputStream newOutputStream ( Path path, OpenOption... options ) throws IOException {
        List <OpenOption> optlist = Arrays.asList(options);
        if (optlist.contains(StandardOpenOption.READ))
            throw new IllegalArgumentException(String.format("%s is not supported by this method.", StandardOpenOption.READ));
        FileObject fo = toVFSPath(path).toFileObject();
        if (optlist.contains(StandardOpenOption.CREATE_NEW) && fo.exists())
            throw new IOException(String.format("%s already exists, and the option %s was specified.", fo, StandardOpenOption.CREATE_NEW));
        checkAccess(path, AccessMode.WRITE);
        return fo.getContent().getOutputStream(optlist.contains(StandardOpenOption.APPEND));
    }

    @SuppressWarnings("unchecked")
    //@override
    public
    <A extends BasicFileAttributes> A readAttributes ( Path path, Class <A> type, LinkOption... options ) throws IOException {
        if (type == BasicFileAttributes.class || type == Vfs2NioFileAttributes.class)
            return (A) toVFSPath(path).getAttributes();
        return null;
    }

    //@override
    public
    Map <String, Object> readAttributes ( Path path, String attribute, LinkOption... options ) throws IOException {
        return toVFSPath(path).readAttributes(attribute, options);
    }

    //@override
    public
    Path readSymbolicLink ( Path link ) throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    //@override
    public
    void setAttribute ( Path path, String attribute, Object value, LinkOption... options ) throws IOException {
        toVFSPath(path).setAttribute(attribute, value, options);
    }

    protected
    URI toFsUri ( URI uri ) {
        String scheme = uri.getScheme();
        if ((scheme == null) || !scheme.equalsIgnoreCase(getScheme())) {
            throw new IllegalArgumentException(String.format("URI scheme must be %s", getScheme()));
        }
        try {
            String spec = uri.getSchemeSpecificPart();
            int sep = spec.indexOf("!/");
            if (sep != -1) spec = spec.substring(0, sep);
            URI u = new URI(spec);
            return u;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    protected
    Path XXuriToPath ( URI uri ) {
        String scheme = uri.getScheme();
        if ((scheme == null) || !scheme.equalsIgnoreCase(getScheme())) {
            throw new IllegalArgumentException("URI scheme is not '" + getScheme() + "'");
        }
        try {
            // only support legacy JAR URL syntax vfs:{uri}!/{entry} for now
            String spec = uri.getSchemeSpecificPart();
            int sep = spec.indexOf("!/");
            if (sep != -1) spec = spec.substring(0, sep);
            URI u = new URI(spec);
            return Paths.get(u).toAbsolutePath();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    void removeFileSystem ( URI path ) throws IOException {
        filesystems.remove(path);
    }
}
