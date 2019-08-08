 /*
  * Copyright The Sett Ltd, 2005 to 2014.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.ltc.hitalk.wam.compiler;

 import com.thesett.aima.logic.fol.*;
 import com.thesett.aima.logic.fol.wam.builtins.BuiltInFunctor;
 import com.thesett.common.util.Function;
 import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
 import org.ltc.hitalk.core.HtConstants;
 import org.ltc.hitalk.entities.*;
 import org.ltc.hitalk.term.ListTerm;

 import java.util.*;
 import java.util.function.Predicate;

 import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;
 import static org.ltc.hitalk.core.HtConstants.*;
 import static org.ltc.hitalk.entities.IRelation.*;

 /**
  * BuiltInTransform implements a compilation transformation over term syntax trees, that substitutes for functors that
  * map onto Prolog built-ins, an extension of the functor entityKind that implements the built-in.
  *
  * <pre><p/><table id="crc"><caption>CRC Card</caption>
  * <tr><th> Responsibilities <th> Collaborations
  * <tr><td> Transform functors to built in functors where appropriate.
  *     <td> {@link BuiltInFunctor}, {@link VariableAndFunctorInterner}.
  * </table></pre>
  *
  * @author Rupert Smith
  */
 public
 class HiTalkBuiltInTransform implements Function <Functor, Functor> {
     public static final Map <String, HtRelationKind> relationKindMap;

     static {
         Map <String, HtRelationKind> tmp = new HashMap <>();

         tmp.put(EXTENDS, HtRelationKind.EXTENDS);
         tmp.put(IMPLEMENTS, HtRelationKind.IMPLEMENTS);
         tmp.put(IMPORTS, HtRelationKind.IMPORTS);
         tmp.put(COMPLEMENTS, HtRelationKind.COMPLEMENTS);
         tmp.put(INSTANTIATES, HtRelationKind.INSTANTIATES);
         tmp.put(SPECIALIZES, HtRelationKind.SPECIALIZES);

         relationKindMap = Collections.unmodifiableMap(tmp);
     }

     /**
      * Holds a mapping from functor names to built-in implementations.
      */
     private final Map <HtFunctorName, Predicate <Functor>> builtIns = new HashMap <>();
     /**
      * Holds the default built in, for standard compilation and interners and symbol tables.
      */
     private final HiTalkDefaultBuiltIn defaultBuiltIn;
     private final VariableAndFunctorInterner interner;
     ///////////////////////////
     private HtEntityIdentifier entityCompiling;
     private IRelation lastRelation;
     //     private boolean noLists;
     private Clause <?> lastDirective;

     /**
      * Initializes the built-in transformation by population the the table of mappings of functors onto their built-in
      * implementations.
      *
      * @param defaultBuiltIn The default built in, for standard compilation and interners and symbol tables.
      */
     public
     HiTalkBuiltInTransform ( HiTalkDefaultBuiltIn defaultBuiltIn ) {
         this.defaultBuiltIn = defaultBuiltIn;
         interner = defaultBuiltIn.getInterner();

         builtIns.put(new HtFunctorName("true", 0), this::true_p);
         builtIns.put(new HtFunctorName("fail", 0), this::fail_p);
         builtIns.put(new HtFunctorName("!", 0), this::cut_p);
         builtIns.put(new HtFunctorName("\\+", 0), this::not_p);

         builtIns.put(new HtFunctorName("=", 2), this::unifies_p);
         builtIns.put(new HtFunctorName(":=", 2), this::assign_p);
         builtIns.put(new HtFunctorName("\\=", 2), this::nonUnifies_p);
         builtIns.put(new HtFunctorName(";", 2), this::disjunction_p);
         builtIns.put(new HtFunctorName(",", 2), this::conjunction_p);
         builtIns.put(new HtFunctorName("call", 1), this::call_p);
         builtIns.put(new HtFunctorName("object", 1, 5), this::object_p);//todo 1-5
         builtIns.put(new HtFunctorName("protocol", 1, 2), this::protocol_p);//todo 1-2
         builtIns.put(new HtFunctorName("category", 1, 4), this::category_p);//todo 1-4
         builtIns.put(new HtFunctorName("end_object", 0), this::endObject_p);
         builtIns.put(new HtFunctorName("end_protocol", 0), this::endProtocol_p);
         builtIns.put(new HtFunctorName("end_category", 0), this::endCategory_p);

         builtIns.put(new HtFunctorName("op", 3), this::op_p);
         builtIns.put(new HtFunctorName("current_op", 3), this::current_op_p);

         builtIns.put(new HtFunctorName("initialization", 1), this::initialization_p);
         builtIns.put(new HtFunctorName(HtConstants.PUBLIC, 1), this::public_p);
         builtIns.put(new HtFunctorName(PROTECTED, 1), this::protected_p);
         builtIns.put(new HtFunctorName(PRIVATE, 1), this::private_p);

         builtIns.put(new HtFunctorName(EXTENDS, 1), this::extends_p);
         builtIns.put(new HtFunctorName(IMPLEMENTS, 1), this::implements_p);
         builtIns.put(new HtFunctorName(IMPORTS, 1), this::imports_p);
         builtIns.put(new HtFunctorName(COMPLEMENTS, 1), this::complements_p);
         builtIns.put(new HtFunctorName(INSTANTIATES, 1), this::instantiates_p);
         builtIns.put(new HtFunctorName(SPECIALIZES, 1), this::specializes_p);
         builtIns.put(new HtFunctorName(CREATE_OBJECT, 1), this::create_object_p);

         builtIns.put(new HtFunctorName(INCLUDE, 1), this::include_p);
         builtIns.put(new HtFunctorName(ENCODING, 1), this::encoding_p);

         builtIns.put(new HtFunctorName(MULTIFILE, 1), this::multifile_p);
         builtIns.put(new HtFunctorName(DISCONTIGUOUS, 1), this::discontiguous_p);
         builtIns.put(new HtFunctorName(DYNAMIC, 1), this::dynamic_p);
         builtIns.put(new HtFunctorName(STATIC, 1), this::static_p);

         builtIns.put(new HtFunctorName(HILOG, 1), this::hilog_p);

         builtIns.put(new HtFunctorName(EXPAND_GOAL, 1), this::expand_goal_p);
         builtIns.put(new HtFunctorName(EXPAND_TERM, 1), this::expand_term_p);


     }

     /**
      * Returns the result of type Y from applying this function to an argument of type X.
      *
      * @param functor The argument to the function.
      * @return The result of applying the function to its argument.
      */
     @Override
     public
     Functor apply ( Functor functor ) {
         return null;
     }

     /**
      * @return
      */
     public
     HiTalkDefaultBuiltIn getDefaultBuiltIn () {
         return defaultBuiltIn;
     }

//     ==================================================================

     private
     boolean discontiguous_p ( Functor functor ) {
         return true;
     }

     private
     boolean not_p ( Functor functor ) {
         return false;
     }

     private
     boolean expand_term_p ( Functor functor ) {
         return true;
     }


     private
     boolean expand_goal_p ( Functor functor ) {
         return true;
     }

     private
     boolean hilog_p ( Functor functor ) {

         return true;
     }

     private
     boolean create_object_p ( Functor functor ) {


         handleEntityRelations((HtFunctor) functor, true);
         return false;
     }

     private
     boolean encoding_p ( Functor functor ) {

         return true;
     }

     private
     boolean multifile_p ( Functor functor ) {
         functor.setProperty(MULTIFILE, functor.getArgument(0));
         return true;
     }

     private
     boolean dynamic_p ( Functor functor ) {
         functor.setProperty(DYNAMIC, functor.getArgument(0));
         return true;
     }

     private
     boolean static_p ( Functor functor ) {
         functor.setProperty(STATIC, functor.getArgument(0));
         return true;
     }

     private
     boolean include_p ( Functor functor ) {
         functor.setProperty(INCLUDE, functor.getArgument(0));
         return true;
     }

     private
     boolean extends_p ( Functor functor ) {
         createRelation(functor, HtRelationKind.EXTENDS, false);
         return true;
     }

     private
     boolean implements_p ( Functor functor ) {
         createRelation(functor, HtRelationKind.IMPLEMENTS, false);
         return true;
     }

     private
     boolean imports_p ( Functor functor ) {
         createRelation(functor, HtRelationKind.IMPORTS, false);
         return true;
     }

     private
     boolean complements_p ( Functor functor ) {
         createRelation(functor, HtRelationKind.COMPLEMENTS, false);
         return true;
     }

     private
     boolean instantiates_p ( Functor functor ) {
         createRelation(functor, HtRelationKind.INSTANTIATES, false);
         return true;
     }

     private
     boolean specializes_p ( Functor functor ) {
         createRelation(functor, HtRelationKind.SPECIALIZES, false);
         return true;
     }

     private
     boolean public_p ( Functor functor ) {

         return true;
     }

     private
     boolean protected_p ( Functor functor ) {

         return true;
     }

     private
     boolean private_p ( Functor functor ) {

         return true;
     }

     private
     boolean true_p ( Functor term ) {
         return true;
     }

     private
     boolean fail_p ( Functor term ) {
         return false;
     }

     private
     boolean cut_p ( Functor term ) {
         return true;
     }

     private
     boolean unifies_p ( Functor term ) {
         boolean result = false;

         return result;
     }

     private
     boolean nonUnifies_p ( Functor term ) {
         boolean result = false;

         return result;
     }

     private
     boolean assign_p ( Functor term ) {
         return true;
     }

     private
     boolean conjunction_p ( Functor term ) {
         boolean result = false;

         return result;
     }

     private
     boolean disjunction_p ( Functor term ) {
         boolean result = false;

         return result;
     }

     private
     boolean call_p ( Functor term ) {
         boolean result = false;

         return result;
     }

     private
     boolean apply_p ( Functor functor ) {
         return true;
     }

     private
     boolean object_p ( Functor functor ) {
         if (isEntityCompiling()) {
             throw new ExecutionError(PERMISSION_ERROR, "");
         }
         entityCompiling = new HtEntityIdentifier(functor, HtEntityKind.OBJECT);
         handleEntityRelations((HtFunctor) functor, false);

         return true;
     }

     @SuppressWarnings("SuspiciousMethodCalls")
     private
     void handleEntityRelations ( HtFunctor entityFunctor, boolean dynamic ) {
         int arityMax = entityFunctor.getArityMax();
         HtEntityKind entityKind = entityCompiling.getKind();
         EnumSet <HtRelationKind> kinds = EnumSet.noneOf(HtRelationKind.class);
         List <Set <IRelation>> relations = new ArrayList <>();
         for (int i = entityFunctor.getArityMin(); i < arityMax; i++) {
             Functor relationFunctor = (Functor) entityFunctor.getArgument(i);
             Functor subEntityFunctor = (Functor) relationFunctor.getArgument(0);
             if (isList(subEntityFunctor)) {
                 ListTerm listTerm = (ListTerm) subEntityFunctor;
                 for (; listTerm.isNil(); listTerm.newTail()) {
                     handleNormalizedRelations((Functor) listTerm.getHead(), kinds, relations, dynamic);
                 }
             }
             else {
                 if (entityFunctor.getArityMin() != entityFunctor.getArityMax()) {
                     FunctorName name = interner.getDeinternedFunctorName(entityFunctor.getName());
                     relationFunctor = (Functor) entityFunctor.getArgument(1);
                     createRelation(relationFunctor, relationKindMap.get(name), dynamic);
                 }
             }
         }
     }

     private
     void handleNormalizedRelations ( Functor functor,
                                      EnumSet <HtRelationKind> kinds,
                                      List <Set <IRelation>> relations,
                                      boolean dynamic ) {
         Set <IRelation> arr = new HashSet <>();
         String name = interner.getFunctorName(functor);
         HtRelationKind relationKind = relationKindMap.get(name);
         if (kinds.add(relationKind)) {
             relations.add(arr);
             arr.add(createRelation(functor, relationKind, dynamic));
         }
     }

     /**
      * todo SAVE RELATIONS
      *
      * @param relationFunctor
      * @param relationKind
      * @param dynamic
      * @return
      */
     private
     IRelation createRelation ( Functor relationFunctor, HtRelationKind relationKind, boolean dynamic ) {
         HtScope scope = new HtScope(HtConstants.PUBLIC);//default scope
         Functor subEntityFunctor = (Functor) relationFunctor.getArgument(0);
         String name = interner.getFunctorName(subEntityFunctor);
         Functor subEntName;
         if (COLON_COLON.equals(name)) {
             scope = new HtScope(interner.getFunctorName((Functor) subEntityFunctor.getArgument(0)));
             subEntName = (Functor) subEntityFunctor.getArgument(1);//todo must it be already loaded??
         }
         else {
             subEntName = (Functor) subEntityFunctor.getArgument(0);
         }
         HtEntityKind subEntityKind = detectSubEntityKind(relationKind, entityCompiling.getKind());
         return new HtRelation(entityCompiling, scope, new HtEntityIdentifier(subEntName, subEntityKind), relationKind);
     }

     private
     boolean isList ( Term relationTerm ) {
         return relationTerm instanceof ListTerm;
     }

     private
     HtEntityKind detectSubEntityKind ( HtRelationKind relationKind, HtEntityKind superKind ) {
         HtEntityKind result = HtEntityKind.OBJECT;
         switch (relationKind) {
             case EXTENDS:
                 result = superKind;
                 break;
             case IMPLEMENTS:
                 if (superKind == HtEntityKind.OBJECT || superKind == HtEntityKind.CATEGORY) {
                     result = HtEntityKind.PROTOCOL;
                     break;
                 }
                 else {
                     throw new ExecutionError(PERMISSION_ERROR, "");//protocol implememnts protocp;!!!!!!!!!
                 }
             case IMPORTS:
                 if (superKind == HtEntityKind.OBJECT) {
                     result = HtEntityKind.CATEGORY;
                     break;
                 }
             case COMPLEMENTS:
                 if (superKind == HtEntityKind.CATEGORY) {
                     break;
                 }
             case SPECIALIZES:
             case INSTANTIATES:
                 break;
             default:
                 throw new ExecutionError(PERMISSION_ERROR, "" + superKind + "/" + relationKind);
         }
         return result;
     }

     private
     boolean isEntityCompiling () {
         return entityCompiling != null;
     }

     private
     boolean protocol_p ( Functor functor ) {
         if (isEntityCompiling()) {
             throw new ExecutionError(PERMISSION_ERROR, "");
         }
         entityCompiling = new HtEntityIdentifier(functor, HtEntityKind.PROTOCOL);
         handleEntityRelations((HtFunctor) functor, false);

         return true;
     }

     private
     boolean category_p ( Functor functor ) {
         if (isEntityCompiling()) {
             throw new ExecutionError(PERMISSION_ERROR, "");
         }
         entityCompiling = new HtEntityIdentifier(functor, HtEntityKind.CATEGORY);
         handleEntityRelations((HtFunctor) functor, false);

         return true;
     }

     private
     boolean endObject_p ( Functor functor ) {
         return endEntity(HtEntityKind.OBJECT);
     }

     private
     boolean endProtocol_p ( Functor functor ) {
         return endEntity(HtEntityKind.PROTOCOL);
     }

     private
     boolean endCategory_p ( Functor functor ) {
         return endEntity(HtEntityKind.CATEGORY);
     }

     boolean endEntity ( HtEntityKind entityKind ) {
         try {
             if (entityCompiling == null || entityCompiling.getKind() != entityKind) {
                 throw new ExecutionError(PERMISSION_ERROR, "");
             }
         } finally {
//             entityCompiling;
             //ending object
             entityCompiling = null;
             lastDirective = null;//fixme
             lastRelation = null;
         }
         return true;
     }

     private
     boolean op_p ( Functor functor ) {
         return true;
     }

     private
     boolean current_op_p ( Functor functor ) {
         return true;
     }

     private
     boolean initialization_p ( Functor functor ) {
         return true;
     }

     public
     VariableAndFunctorInterner getInterner () {
         return interner;
     }

     public
     Clause <?> getLastDirective () {
         return lastDirective;
     }
 }
