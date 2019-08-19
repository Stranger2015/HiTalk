package org.ltc.hitalk.wam.compiler;

/**
 * /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * read_term(?term, ReadData rd)
 * Common part of all read variations.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 */

//      static bool
//      read_term(term_t term,ReadData rd ARG_LD)
//      {int rc2,rc=FALSE;
//      term_t*result;
//      Token token;
//      Word p;
//      fid_t fid;
//
//      if(!raw_read(rd,&rd->end PASS_LD))
//      fail;
//
//      if(!(fid=PL_open_foreign_frame()))
//      return FALSE;
//
//      rd->here=rd->base;
//      rd->strictness=truePrologFlag(PLFLAG_ISO);
//      if((rc2=complex_term(NULL,OP_MAXPRIORITY+1,
//      rd->subtpos,rd PASS_LD))!=TRUE)
//      {rc=raiseStackOverflow(rc2);
//      goto out;
//      }
//      assert(rd->term_stack.top==1);
//      result=term_av(-1,rd);
//      p=valTermRef(result[0]);
//      if(varInfo(*p,rd))        /* reading a single variable */
//      {if((rc2=ensureSpaceForTermRefs(1PASS_LD))!=TRUE)
//      {rc=raiseStackOverflow(rc2);
//      goto out;
//      }
//      p=valTermRef(result[0]);        /* may be shifted */
//      readValHandle(result[0],p,rd PASS_LD);
//      }
//
//      if(!(token=get_token(FALSE,rd)))
//      goto out;
//      if(token->type!=T_FULLSTOP)
//      {errorWarning("end_of_clause_expected",0,rd);
//      goto out;
//      }
//
//      if(rd->cycles&&PL_is_functor(result[0],FUNCTOR_xpceref2))
//      rc=instantiate_template(term,result[0]PASS_LD);
//      else
//      rc=PL_unify(term,result[0]);
//
//      truncate_term_stack(result,rd);
//      if(!rc)
//      goto out;
//      if(rd->varnames&&!(rc=bind_variable_names(rd PASS_LD)))
//      goto out;
//       #ifdef O_QUASIQUOTATIONS
//      if(!(rc=parse_quasi_quotations(rd PASS_LD)))
//      goto out;
//       #endif
//      if(rd->variables&&!(rc=bind_variables(rd PASS_LD)))
//      goto out;
//      if(rd->singles&&!(rc=check_singletons(term,rd PASS_LD)))
//      goto out;
//
//      rc=TRUE;
//
//      out:
//      PL_close_foreign_frame(fid);
//
//      return rc;
//      }

public
class HitalkReader {
}
