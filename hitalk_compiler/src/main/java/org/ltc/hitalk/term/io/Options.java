package org.ltc.hitalk.term.io;

import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.entities.PropertyOwner;

import java.util.Map;

/**
 *
 */
public class Options extends PropertyOwner {
    /**
     * @param methods
     * @param props
     * @param map
     * @param mmap
     */
    public Options(HtMethodDef[] methods,
                   HtProperty[] props,
                   Map<String, HtProperty> map,
                   Map<String, HtMethodDef> mmap) {

        super(props, methods, map, mmap);
    }

    /**
     * @param methods
     * @param props
     */
    public Options(HtMethodDef[] methods, HtProperty[] props) {
        super(methods, props);
    }

    /**
     *
     */
    public static class Option extends HtProperty {

        public Option(String name, String... values) {
            super(name, values);
        }
    }
}
