package org.ltc.hitalk.wam.compiler;

public enum Language {
    PROLOG("Prolog"),
    HILOG("HiLog"),
    HITALK("HiTalk"),
    LOGTALK("Logtalk");

    private final String name;

    /**
     * @param name
     */
    Language ( String name ) {
        this.name = name;
    }

    /**
     * @return
     */
    public String getName () {
        return name;
    }
}
