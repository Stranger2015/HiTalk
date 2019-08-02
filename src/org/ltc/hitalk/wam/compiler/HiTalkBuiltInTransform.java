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
 import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
 import com.thesett.aima.logic.fol.wam.builtins.BuiltInFunctor;
 import com.thesett.common.util.Function;
 import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
 import org.ltc.hitalk.entities.HtEntityIdentifier;

 import java.util.HashMap;
 import java.util.Map;
 import java.util.function.Predicate;

 import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;
 import static org.ltc.hitalk.entities.HtEntityKind.*;


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
         builtIns.put(new FunctorName("object", 1), this::object_p);
         builtIns.put(new FunctorName("protocol", 1), this::protocol_p);
         builtIns.put(new FunctorName("category", 1), this::category_p);
         builtIns.put(new FunctorName("end_object", 0), this::endObject_p);
         builtIns.put(new FunctorName("end_protocol", 0), this::endProtocol_p);
         builtIns.put(new FunctorName("end_category", 0), this::endCategory_p);

         builtIns.put(new FunctorName("op", 3), this::op_p);
         builtIns.put(new FunctorName("current_op", 3), this::current_op_p);

         builtIns.put(new FunctorName("initialization", 1), this::initialization_p);
         builtIns.put(new FunctorName("initialization", 1), this::initialization_p);
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
     boolean object_p ( Functor term ) {
         if (isEntityCompiling()) {
             throw new ExecutionError(PERMISSION_ERROR);
         }

         entityCompiling = new HtEntityIdentifier(term, OBJECT);

         handleObjectRelations();

         return true;
     }

     private
     void handleObjectRelations () {

     }

     private
     boolean isEntityCompiling () {
         return entityCompiling != null;
     }

     private
     boolean protocol_p ( Functor term ) {
         return true;
     }

     private
     boolean category_p ( Functor term ) {
         return true;
     }

     private
     boolean endObject_p ( Functor term ) {
         if (entityCompiling != null || entityCompiling.getKind() != OBJECT) {
             throw new ExecutionError(PERMISSION_ERROR);
         }
         //ending object

         entityCompiling = null;
         return true;
     }

     private
     boolean endProtocol_p ( Functor term ) {
         if (isEntityCompiling()) {
             throw new ExecutionError(PERMISSION_ERROR);
         }
         ;
         Functor functor = (Functor) term;
         entityCompiling = new HtEntityIdentifier(functor, PROTOCOL);
         return true;
     }

     private
     boolean endCategory_p ( Functor term ) {
         if (isEntityCompiling()) {
             throw new ExecutionError(PERMISSION_ERROR);
         }
         Functor functor = (Functor) term;
         entityCompiling = new HtEntityIdentifier(functor, CATEGORY);
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
