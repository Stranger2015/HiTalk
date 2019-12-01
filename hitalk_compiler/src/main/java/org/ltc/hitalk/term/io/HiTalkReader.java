package org.ltc.hitalk.term.io;

import org.jetbrains.annotations.NotNull;
import org.ltc.hitalk.entities.IProperty;
import org.ltc.hitalk.entities.IPropertyOwner;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.io.HiTalkStream.Properties;

import java.beans.PropertyChangeListener;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public
class HiTalkReader extends InputStreamReader implements IPropertyOwner {
    /**
     * Creates an InputStreamReader that uses the named charset.
     *
     * @param in          An InputStream
     * @param charsetName The name of a supported
     *                    {@link Charset charset}
     * @throws UnsupportedEncodingException If the named charset is not supported
     */
    public
    HiTalkReader ( @NotNull InputStream in, @NotNull String charsetName ) throws UnsupportedEncodingException {
        super(in, charsetName);
    }

    /**
     * @return
     */
    @Override
    public
    int getPropLength () {
        return 0;
    }

    @Override
    public
    void addListener ( PropertyChangeListener listener ) {

    }

    @Override
    public
    void removeListener ( PropertyChangeListener listener ) {

    }

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
}
