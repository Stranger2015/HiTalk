/**
 * COMMON(atom_t) lookup_clref(Clause clause);
 * COMMON(Clause) clause_clref(atom_t aref);
 * COMMON(int)   PL_put_clref(term_t t, Clause clause);
 * COMMON(int)   PL_unify_clref(term_t t, Clause clause);
 * COMMON(int)   PL_unify_recref(term_t t, RecordRef rec);
 * COMMON(void*) PL_get_dbref(term_t t, db_ref_type *type);
 * COMMON(int)   PL_get_clref(term_t t, Clause *cl);
 * COMMON(int)   PL_get_recref(term_t t, RecordRef *rec);
 * COMMON(int)   PL_is_dbref(term_t t);
 * COMMON(void)  initDBRef(void);
 */
public class Database {
    enum DbRefType {
        DB_REF_CLAUSE,
        DB_REF_RECORD,
    }
}
