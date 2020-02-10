package org.ltc.hitalk.wam.compiler;

import javax.lang.model.SourceVersion;
import javax.tools.Tool;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

/**
 *
 */
public class Tools implements Tool {
    public enum Kind {
        COMPILER("compiler"),
        INTERPRETER("interpreter"),
        ;

        private final String name;

        Kind ( String name ) {
            this.name = name;
        }

        public String getName () {
            return name;
        }
    }

    /**
     * @param in
     * @param out
     * @param err
     * @param arguments
     * @return
     */
    @Override
    public int run ( InputStream in, OutputStream out, OutputStream err, String... arguments ) {
        return 0;
    }

    /**
     * @return
     */
    @Override
    public Set <SourceVersion> getSourceVersions () {
        return null;
    }
}
