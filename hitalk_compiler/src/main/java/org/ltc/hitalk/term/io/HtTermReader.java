package org.ltc.hitalk.term.io;

import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.entities.PropertyOwner;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.term.ITerm;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class HtTermReader extends HtTermIO {
    private HiTalkInputStream stream;
    protected HtPrologParser parser;

    /**
     *
     */
    public HtTermReader(Path path, PlLexer lexer, HtPrologParser parser) throws Exception {
        super(path, lexer.getInputStream());
        this.parser = parser;
        this.parser.setTokenSource(lexer);
//       input = new HiTalkInputStream(path, HiTalkInputStream.defaultEncoding);
//                Paths.get(getPropMap().get("file_name").getValue().toString()),
//                getPropMap().get("encoding").getValue().toString());
        lexer.getInputStream().open();
    }

    public ITerm readTerm() throws Exception {
        final HtProperty[] properties = new HtProperty[]{

        };
        return readTerm(stream, properties);
    }

    public ITerm readTerm(HiTalkInputStream input, HtProperty... options) throws Exception {
        createOptions();
//        PlLexer lexer = new PlLexer(input, path);
//        parser.setTokenSource(lexer);
        return parser.parse();
    }

    private void createOptions(String... options) {
        List<HtProperty> list = new ArrayList<>();
        for (String option : options) {
            list.add(PropertyOwner.createProperty("alias(Atom)", option));
        }
    }

    /**
     * @param sb
     */
    public void toString0(StringBuilder sb) {

    }
}
