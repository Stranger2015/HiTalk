package org.ltc.hitalk.term.io;

import org.jetbrains.annotations.NotNull;
import org.ltc.hitalk.entities.IPropertyOwner;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public
class HiTalkReader extends InputStreamReader implements IPropertyOwner <I> {
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
}
