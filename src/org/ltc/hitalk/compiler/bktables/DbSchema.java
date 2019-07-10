package org.ltc.hitalk.compiler.bktables;

public
class DbSchema {

    private int recordNum;
    ///  private short[] entityIdexes;

    DbRecord[] records;

    public
    DbRecord[] getData () {

        return records;
    }

    class DbRecord {
        int entity1;
        //  int entity2;
        //  Enum <HtRelationKind> kind;//pub pri pro

    }
}
