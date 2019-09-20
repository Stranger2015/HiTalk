package org.ltc.hitalk.term.io;


import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.parser.HtPrologParser;

import java.util.ArrayList;
import java.util.List;

public
class TermIO {
    //    private final
//    private final InputStream in;
    private final ITermFactory tf;
    private final HtPrologParser pp;

    protected static final List <HiTalkStream> streams = new ArrayList <>();

    public static
    HiTalkStream currentInput () {
        return streams.get(0);
    }

    /**
     * @return
     */
    public static
    HiTalkStream currentOutput () {
        return streams.get(1);
    }

    /**
     * @param tf
     * @param pp
     */
    public
    TermIO ( ITermFactory tf, HtPrologParser pp ) {
        this.tf = tf;
        this.pp = pp;
        initOptions("startup.pl");
//        in=currentInput();
//        expansionHooks = null;
    }


    private
    void initOptions ( String fn ) {

    }
}