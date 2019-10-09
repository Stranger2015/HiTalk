package org.ltc.hitalk.parser.jp.segfault.prolog.parser;


import org.ltc.hitalk.parser.jp.segfault.prolog.parser.Operator.Kind;

import java.util.*;

/**
 * 演算子テーブルです。
 *
 * @author shun
 */
public class OperatorTable1 {

    private LinkedHashMap <String, ArrayList <Operator>> table = new LinkedHashMap <>();

    public void setOperator ( Integer priority, Kind kind, String notation ) {
        ArrayList <Operator> l = table.computeIfAbsent(notation, k -> new ArrayList <>(2));
        Operator operator = new Operator(priority, kind, notation);
        l.remove(operator);
        if (priority != 0) {
            l.add(operator);
            // System.err.println("OperatorTable: add op("+ kind +"): '"+ notation +"'");
        }
    }

    public List <Operator> getOperator ( String notation ) {
        ArrayList <Operator> l = table.get(notation);
        return l != null ? Collections.unmodifiableList(l) : Collections. <Operator>emptyList();
    }

    public Set <String> keys () {
        return table.keySet();
    }

}
