package org.ltc.hitalk.compiler.bktables.db;

import org.ltc.hitalk.compiler.bktables.IRegistry;
import org.ltc.hitalk.entities.HtEntityIdentifier;
import org.ltc.hitalk.entities.HtRelation;
import org.ltc.hitalk.entities.HtRelationKind;

import java.util.EnumSet;

/**
 *
 */
public
class BkRelations<T extends HtRelation> extends DbEntity2RecOfArrays <T> {
    /**
     * @param recordNum
     * @param rels1
     * @param rels2
     * @param relationKinds
     */
    public
    BkRelations ( int recordNum,
                  T[] rels1,
                  T[] rels2,
                  EnumSet <HtRelationKind> relationKinds
    ) {
        super(recordNum, rels1, rels2, relationKinds);
    }

    /**
     * @param rel
     * @param registry
     * @return
     */
    public
    HtRelation addRecord ( HtRelation rel, IRegistry registry ) {
        HtEntityIdentifier e1 = rel.getEntityIdentifier1();
        HtEntityIdentifier e2 = rel.getEntityIdentifier2();
        HtRelationKind kind = rel.getRelationKind();
        int idx1 = e1.getName();
        int idx2 = e2.getName();
//        EnumSet <HtRelationKind> newKinds = EnumSet.allOf(kind.getDeclaringClass());
//        getRelationKinds().addAll(newKinds);

        if (!registry.isRegistered(rel.getClass())) {
//            recordNum++;
//            return rel;
            return (HtRelation) registry.register(rel);
        }

        return null;
    }
}
