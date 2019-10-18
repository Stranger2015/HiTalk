package org.ltc.hitalk.wam.compiler;

import javax.lang.model.SourceVersion;
import javax.tools.Tool;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public enum Tools implements Tool {
    COMPILER("compiler"),
    INTERPRETER("interpreter"),
    ;

    private final String name;

    Tools ( String name ) {
        this.name = name;
    }

    @Override
    public int run ( InputStream in, OutputStream out, OutputStream err, String... arguments ) {
        return 0;
    }

    @Override
    public Set <SourceVersion> getSourceVersions () {
        return null;
    }
}
