package org.ltc.hitalk.term.io;

import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.entities.PropertyOwner;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.term.ITerm;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class HtTermReader extends PropertyOwner {

    HiTalkInputStream input;
    //    HtProperty[] properties;//options
    PlPrologParser parser;

    /**
     * @param props
     * @param methods
     * @param map
     * @param mmap
     */
    public HtTermReader(HtMethodDef[] methods,
                        HtProperty[] props,
                        Set<HtProperty> set,
                        Map<String, HtMethodDef> mmap) throws IOException {
        super(props, methods, set, mmap);
        input = new HiTalkInputStream(
                Paths.get((String) getMap().CO("file_name").getValue()),
                (String) getMap().get("encoding").getValue());
//        input.setInputStream(new FileInputStream(this.));
        input.open();
    }

    /**
     * @param methods
     * @param props
     */
    public HtTermReader(HtMethodDef[] methods, HtProperty[] props) {
        super(methods, props);


    }

    public ITerm readTerm() {
        final HtProperty[] properties = new HtProperty[]{

        };
        return readTerm(input, properties);
    }

    public ITerm readTerm(HiTalkInputStream input, HtProperty... options) {
        createOptions();
        return null;
    }

    private HtProperty[] createOptions(HtProperty... properties) {
    }
}
