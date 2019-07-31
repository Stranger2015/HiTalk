package org.ltc.hitalk.term.io;


import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.parser.HtPrologParser;

public
class TermIO {

    private final ITermFactory tf;
    private final HtPrologParser pp;

    public
    TermIO ( ITermFactory tf, HtPrologParser pp ) {
        this.tf = tf;
        this.pp = pp;
        initOptions("startup.pl");
//        expansionHooks = null;
    }


    private
    void initOptions ( String fn ) {

    }
}