package org.ltc.hitalk.term.io;

import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.entities.PropertyOwner;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.term.ITerm;

import java.beans.PropertyChangeListener;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class HtTermReader extends PropertyOwner implements PropertyChangeListener {

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
                        Map<String, HtProperty> map,
                        Map<String, HtMethodDef> mmap) throws Exception {
        super(props, methods, map, mmap);
        input = new HiTalkInputStream(
                Paths.get(getPropMap().get("file_name").getValue().toString()),
                getPropMap().get("encoding").getValue().toString());
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

    private HtProperty[] createOptions(String... options) {
        List<HtProperty> list = new ArrayList<>();
        for (String option : options) {
            list.add(PropertyOwner.createProperty("alias(Atom)", option));
        }
        return list.toArray(new HtProperty[list.size()]);
    }
}
