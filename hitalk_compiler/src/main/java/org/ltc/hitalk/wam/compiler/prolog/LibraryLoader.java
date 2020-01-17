package org.ltc.hitalk.wam.compiler.prolog;

import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.ITokenSource;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.LibParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 *
 */
public class LibraryLoader {
    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    /**
     * @param classPath wambuiltins.pl
     * @return
     * @throws Exception
     */
    public Path loadWAMBuiltIns ( Path classPath ) throws Exception {
//        final String classPath = Paths.get(System.getProperties().getProperty("CLASSPATH",);
        ITokenSource tokenSource = ITokenSource.getTokenSourceForIoFile(classPath.toFile());
        // Set up a parser on the token source.
        LibParser libParser = new LibParser();
        libParser.setTokenSource(tokenSource);
        logger.info("Loading " + classPath);
        // Load the built-ins into the domain
        while (tokenSource.isOpen()) {
            final ITerm term = libParser.parse();
            if (term == PlPrologParser.BEGIN_OF_FILE_ATOM) {
                logger.info("begin_of_file");
            } else if (term == PlPrologParser.END_OF_FILE_ATOM) {
                logger.info("end_of_file");
                libParser.popTokenSource();
            } else {
                HtClause clause = libParser.convert(term);
//                    inCompiler.compile((T) clause);
            }
        }
//        wamCompiler.endScope();
//         There should not be any errors in the built in library, if there are then the prolog engine just
//         isn't going to work, so report this as a bug.
//        throw new IllegalStateException("Got an exception whilst loading the built-in library.", e);

        return classPath;
    }
}
