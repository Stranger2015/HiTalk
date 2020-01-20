package org.ltc.hitalk.parser;

import com.thesett.common.util.Source;
import org.ltc.hitalk.term.io.HiTalkInputStream;

import java.beans.PropertyChangeListener;
import java.io.*;
import java.nio.file.Paths;

import static java.nio.file.StandardOpenOption.READ;
import static org.ltc.hitalk.core.BaseApp.getAppContext;

/**
 *
 */
public interface ITokenSource extends Source<PlToken>, PropertyChangeListener, Closeable {

    /**
     * @throws IOException
     */
    void close() throws IOException;

    static ITokenSource getITokenSourceForString(String string) throws IOException {
        HiTalkInputStream inputStream = new HiTalkInputStream(string);
        return new PlLexer(inputStream);
    }

    static ITokenSource getITokenSourceForStdin() throws FileNotFoundException {
        HiTalkInputStream stream = getAppContext().currentInput();
        stream.setInputStream(new FileInputStream(FileDescriptor.in));

        return new PlLexer(stream, "stdin");
    }

    /**
     * @param encoding
     * @throws IOException
     */
    void onEncodingChanged(String encoding) throws IOException;

    /**
     * @param b
     */
    void setEncodingPermitted(boolean b);

    /**
     * @param file
     * @return
     * @throws IOException
     */
    static ITokenSource getTokenSourceForIoFile(File file) throws IOException {
        HiTalkInputStream stream = new HiTalkInputStream(file);
        return new PlLexer(stream, file.getAbsolutePath());
    }

    /**
     * @param fileName
     * @return
     * @throws IOException
     */
    static ITokenSource getTokenSourceForIoFileName(String fileName) throws IOException {
        HiTalkInputStream stream = new HiTalkInputStream(Paths.get(fileName), "UTF-8", READ);
        return new PlLexer(stream, fileName);
    }

    /**
     * @return
     */
    boolean isEncodingPermitted();

    /**
     * @return
     */
    HiTalkInputStream getInputStream();

    /**
     * @return
     */
    boolean isOpen();

    String getPath();
}
