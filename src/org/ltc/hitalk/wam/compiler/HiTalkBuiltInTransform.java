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

 import com.thesett.aima.logic.fol.Functor;
 import com.thesett.aima.logic.fol.FunctorName;
 import com.thesett.aima.logic.fol.Term;
 import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
 import com.thesett.aima.logic.fol.wam.builtins.BuiltInFunctor;
 import com.thesett.common.util.Function;
 import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
 import org.ltc.hitalk.entities.*;
 import org.ltc.hitalk.term.ListTerm;

 import java.util.*;
 import java.util.function.Predicate;

 import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;
 import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.TYPE_ERROR;
 import static org.ltc.hitalk.core.HtConstants.INCLUDE;
 import static org.ltc.hitalk.entities.HtScope.Kind;
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
     private final Map <FunctorName, Predicate <Functor>> builtIns = new HashMap <>();
     /**
      * Holds the default built in, for standard compilation and interners and symbol tables.
      */
     private final HiTalkDefaultBuiltIn defaultBuiltIn;
     private final VariableAndFunctorInterner interner;
     private HtEntityIdentifier entityCompiling;

     ///////////////////////////
     private IRelation lastRelation;

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

         builtIns.put(new FunctorName("true", 0), this::true_p);
         builtIns.put(new FunctorName("fail", 0), this::fail_p);
         builtIns.put(new FunctorName("!", 0), this::cut_p);

         builtIns.put(new FunctorName("=", 2), this::unifies_p);
         builtIns.put(new FunctorName(":=", 2), this::assign_p);
         builtIns.put(new FunctorName("\\=", 2), this::nonUnifies_p);
         builtIns.put(new FunctorName(";", 2), this::disjunction_p);
         builtIns.put(new FunctorName(",", 2), this::conjunction_p);
         builtIns.put(new FunctorName("call", 1), this::call_p);
         builtIns.put(new FunctorName("object", 1), this::object_p);//todo 1-5
         builtIns.put(new FunctorName("protocol", 1), this::protocol_p);//todo 1-2
         builtIns.put(new FunctorName("category", 1), this::category_p);//todo 1-4
         builtIns.put(new FunctorName("end_object", 0), this::endObject_p);
         builtIns.put(new FunctorName("end_protocol", 0), this::endProtocol_p);
         builtIns.put(new FunctorName("end_category", 0), this::endCategory_p);

         builtIns.put(new FunctorName("op", 3), this::op_p);
         builtIns.put(new FunctorName("current_op", 3), this::current_op_p);

         builtIns.put(new FunctorName("initialization", 1), this::initialization_p);
         builtIns.put(new FunctorName("public", 1), this::public_p);
         builtIns.put(new FunctorName("protected", 1), this::protected_p);
         builtIns.put(new FunctorName("private", 1), this::private_p);

         builtIns.put(new FunctorName(EXTENDS, 1), this::extends_p);
         builtIns.put(new FunctorName(IMPLEMENTS, 1), this::implements_p);
         builtIns.put(new FunctorName(IMPORTS, 1), this::imports_p);
         builtIns.put(new FunctorName(COMPLEMENTS, 1), this::complements_p);
         builtIns.put(new FunctorName(INSTANTIATES, 1), this::instantiates_p);
         builtIns.put(new FunctorName(SPECIALIZES, 1), this::specializes_p);

         builtIns.put(new FunctorName(INCLUDE, 1), this::include_p);


     }

     private
     boolean include_p ( Functor functor ) {
         return false;
     }

     private
     boolean extends_p ( Functor functor ) {
         Term term = functor.getArgument(0);
         if (isList(term)) {
             ListTerm listTerm = (ListTerm) term;
             for (ListTerm currentHead = listTerm; currentHead.isNil(); currentHead = currentHead.getTail()) {
                 term = currentHead.getHead();
                 extends_p();
             }
         }
         else {
             createRelation(entityCompiling, HtRelationKind.EXTENDS, functor);//fixme functor
         }

         return false;
     }

     private
     boolean implements_p ( Functor functor ) {
         return false;
     }

     private
     boolean imports_p ( Functor functor ) {
         return false;
     }

     private
     boolean complements_p ( Functor functor ) {
         return false;
     }

     private
     boolean instantiates_p ( Functor functor ) {
         return false;
     }

     private
     boolean specializes_p ( Functor functor ) {
         return false;
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
         return false;
     }

     private
     boolean nonUnifies_p ( Functor term ) {
         return false;
     }

     private
     boolean assign_p ( Functor term ) {
         return false;
     }

     private
     boolean conjunction_p ( Functor term ) {
         return false;
     }

     private
     boolean disjunction_p ( Functor term ) {
         return false;
     }

     private
     boolean call_p ( Functor term ) {
         return false;
     }

     private
     boolean apply_p ( Functor term ) {
         return false;
     }

     private
     boolean object_p ( Functor functor ) {
         if (isEntityCompiling()) {
             throw new ExecutionError(PERMISSION_ERROR, "");
         }
         entityCompiling = new HtEntityIdentifier(functor, HtEntityKind.OBJECT);
         handleEntityRelations(functor);

         return true;
     }

     private
     void handleEntityRelations ( Functor functor ) {
         int arity = functor.getArity();
         EnumSet <HtRelationKind> kinds = EnumSet.noneOf(HtRelationKind.class);
         List <Set <IRelation>> relations = new ArrayList <>();
         for (int i = 1; i < arity; i++) {
             Term term = functor.getArgument(i);
             if (term.isFunctor()) {
                 Functor f = (Functor) term;
                 String name = interner.getFunctorName(f);
                 HtRelationKind relationKind = relationKindMap.get(name);
                 if (kinds.add(relationKind)) {
                     Set <IRelation> arr = new AbstractSet <IRelation>() {
                         HashSet <IRelation> baked = new HashSet <>();

                         @Override
                         public
                         Iterator <IRelation> iterator () {
                             return baked.iterator();
                         }

                         /**
                          * @return
                          */
                         @Override
                         public
                         int size () {
                             return baked.size();
                         }

                         /**
                          *
                          */
                         @Override
                         public
                         boolean add ( IRelation relation ) {
                             if (!baked.add(relation)) {
//!       Permission error: repeat entity_relation implements/1
//!       in directive object/3
//!       in file /Users/pmoura/Desktop/ad.lgt between lines 1-4
                                 throw new ExecutionError(PERMISSION_ERROR, "");
                             }
                             return true;
                         }
                     };
                     relations.add(arr);
                     arr.add(createRelation(entityCompiling, relationKind, f));
                 }
             }
             else {
                 throw new ExecutionError(TYPE_ERROR, "");
             }
         }
     }

     private
     IRelation createRelation ( HtEntityIdentifier superEntity,
                                HtRelationKind relationKind,
                                Functor functor ) {

         HtEntityIdentifier subEntity;
         HtScope scope;
//         if (name.equals(HtConstants.COLON_COLON)) {
//             scope = new HtScope(decodeScope((Functor) functor.getArgument(0)));
//         }
//         else {
//             scope = new HtScope(PUBLIC);
//         }
         Term relationTerm = functor.getArgument(0);
         if (isList(relationTerm)) {
             subEntity = new HtEntityIdentifier((Functor) functor.getArgument(0),
                     detectSubEntityKind(relationKind, superEntity.getKind()));
         }

         builtIns.get(entityCompiling.getKind()).test(functor);

         return lastRelation;
     }

     private
     boolean isList ( Term relationTerm ) {
         return relationTerm instanceof ListTerm;
     }

     private
     HtEntityKind detectSubEntityKind ( HtRelationKind relationKind, HtEntityKind superKind ) {
         switch (relationKind) {
             case EXTENDS:
                 return superKind;
             case IMPLEMENTS:
                 if (superKind == HtEntityKind.OBJECT || superKind == HtEntityKind.CATEGORY) {
                     return HtEntityKind.PROTOCOL;
                 }
                 else {
                     throw new ExecutionError(PERMISSION_ERROR, "");//protocol implememnts protocp;!!!!!!!!!
                 }
             case IMPORTS:
                 if (superKind == HtEntityKind.OBJECT) {
                     return HtEntityKind.CATEGORY;
                 }
             case COMPLEMENTS:
                 if (superKind == HtEntityKind.CATEGORY) {
                     return HtEntityKind.OBJECT;
                 }
             case SPECIALIZES:
             case INSTANTIATES:
                 return HtEntityKind.OBJECT;
             default:
                 throw new ExecutionError(PERMISSION_ERROR, "" + superKind + "/" + relationKind);
         }
     }

     private
     Kind decodeScope ( Functor functor ) {
         String name = interner.getFunctorName(functor);
         return null;
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
         handleEntityRelations(functor, HtEntityKind.PROTOCOL);

         return true;
     }

     private
     boolean category_p ( Functor functor ) {
         if (isEntityCompiling()) {
             throw new ExecutionError(PERMISSION_ERROR, "");
         }
         entityCompiling = new HtEntityIdentifier(functor, HtEntityKind.CATEGORY);
         handleEntityRelations(functor, HtEntityKind.CATEGORY);

         return true;
     }

     private
     boolean endObject_p ( Functor term ) {
         if (entityCompiling != null || entityCompiling.getKind() != HtEntityKind.OBJECT) {
             throw new ExecutionError(PERMISSION_ERROR, "");
         }
         //ending object

         entityCompiling = null;
         return true;
     }

     private
     boolean endProtocol_p ( Functor term ) {
         if (isEntityCompiling()) {
             throw new ExecutionError(PERMISSION_ERROR, "");
         }
         Functor functor = term;
         entityCompiling = new HtEntityIdentifier(functor, HtEntityKind.PROTOCOL);
         return true;
     }

     private
     boolean endCategory_p ( Functor term ) {
         if (isEntityCompiling()) {
             throw new ExecutionError(PERMISSION_ERROR, "");
         }
         Functor functor = term;
         entityCompiling = new HtEntityIdentifier(functor, HtEntityKind.CATEGORY);
         return true;
     }

     private
     boolean op_p ( Functor functor ) {
         return true;
     }

     private
     boolean current_op_p ( Functor functor ) {
         return false;
     }

     private
     boolean initialization_p ( Functor functor ) {
         return false;
     }

     public
     VariableAndFunctorInterner getInterner () {
         return interner;
     }
 }
