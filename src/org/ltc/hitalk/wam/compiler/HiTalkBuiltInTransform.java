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

import java.util.HashMap;
import java.util.Map;
 import java.util.function.Predicate;

 import static org.ltc.hitalk.wam.compiler.HiTalkBuiltInTransform.BuiltInKind.*;


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
     private final Map <FunctorName, BuiltInKind> builtIns = new HashMap <>();

     /**
      * Holds the default built in, for standard compilation and interners and symbol tables.
      */
     private final HiTalkDefaultBuiltIn defaultBuiltIn;

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
     enum BuiltInKind {
         TRUE(BuiltInKind::True),
         FAIL(BuiltInKind::Fail),
         CUT(BuiltInKind::Cut),
         UNIFIES(BuiltInKind::Unifies),
         NON_UNIFIES(BuiltInKind::NonUnifies),
         ASSIGN(BuiltInKind::Assign),
         CONJUNCTION(BuiltInKind::Conjunction),
         DISJUNCTION(BuiltInKind::Disjunction),
         CALL(BuiltInKind::Call),
         APPLY(BuiltInKind::Apply),
         OBJECT(BuiltInKind::Object),
         PROTOCOL(BuiltInKind::Protocol),
         CATEGORY(BuiltInKind::Category),
         END_OBJECT(BuiltInKind::EndObject),
         END_PROTOCOL(BuiltInKind::EndProtocol),
         END_CATEGORY(BuiltInKind::EndCategory),
         ;

         private static
         boolean True ( Term term ) {
             return false;
         }


         private static
         boolean Fail ( Term term ) {
             return false;
         }


         private static
         boolean Cut ( Term term ) {
             return false;
         }

         private static
         boolean Unifies ( Term term ) {
             return false;
         }

         private static
         boolean NonUnifies ( Term term ) {
             return false;
         }

         private static
         boolean Assign ( Term term ) {
             return false;
         }

         private static
         boolean Conjunction ( Term term ) {
             return false;
         }

         private static
         boolean Disjunction ( Term term ) {
             return false;
         }

         private static
         boolean Call ( Term term ) {
             return false;
         }

         private static
         boolean Apply ( Term term ) {
             return false;
         }

         private static
         boolean Object ( Term term ) {
             return false;
         }

         private static
         boolean Protocol ( Term term ) {
             return true;
         }


         private static
         boolean Category ( Term term ) {
             return true;
         }

         private static
         boolean EndObject ( Term term ) {
             return false;
         }

         private static
         boolean EndProtocol ( Term term ) {
             return true;
         }

         private static
         boolean EndCategory ( Term term ) {
             return false;
         }

         private final Predicate <Term> impl;

         BuiltInKind ( Predicate <Term> impl ) {
             this.impl = impl;
         }
     }

     /**
      * Initializes the built-in transformation by population the the table of mappings of functors onto their built-in
      * implementations.
      *
      * @param defaultBuiltIn The default built in, for standard compilation and interners and symbol tables.
      */
     public
     HiTalkBuiltInTransform ( HiTalkDefaultBuiltIn defaultBuiltIn ) {
         this.defaultBuiltIn = defaultBuiltIn;

         builtIns.put(new FunctorName("true", 0), TRUE);
         builtIns.put(new FunctorName("fail", 0), FAIL);
         builtIns.put(new FunctorName("!", 0), CUT);

         builtIns.put(new FunctorName("=", 2), UNIFIES);
         builtIns.put(new FunctorName(":=", 2), ASSIGN);
         builtIns.put(new FunctorName("\\=", 2), NON_UNIFIES);
         builtIns.put(new FunctorName(";", 2), DISJUNCTION);
         builtIns.put(new FunctorName(",", 2), CONJUNCTION);
         builtIns.put(new FunctorName("call", 1), CALL);
//         builtIns.put(new FunctorName("{}", 1), Bypass.class);
         builtIns.put(new FunctorName("object", 1), OBJECT);
         builtIns.put(new FunctorName("protocol", 1), PROTOCOL);
         builtIns.put(new FunctorName("category", 1), CATEGORY);
         builtIns.put(new FunctorName("end_object", 0), END_OBJECT);
         builtIns.put(new FunctorName("end_protocol", 0), END_PROTOCOL);
         builtIns.put(new FunctorName("end_category", 0), END_CATEGORY);

        /*builtIns.put(new FunctorName("is", 2), Is.class);
        builtIns.put(new FunctorName(">", 2), GreaterThan.class);
        builtIns.put(new FunctorName(">=", 2), GreaterThanOrEqual.class);
        builtIns.put(new FunctorName("<", 2), LessThan.class);
        builtIns.put(new FunctorName("=<", 2), LessThanOrEqual.class);
        builtIns.put(new FunctorName("integer", 1), IntegerCheck.class);
        builtIns.put(new FunctorName("float", 1), FloatCheck.class);*/
     }

     /**
      * Applies a built-in replacement transformation to functors. If the functor matches built-in, a
      * {@link BuiltInFunctor} is created with a mapping to the functors built-in implementation, and the functors
      * arguments are copied into this new functor. If the functor does not match a built-in, it is returned unmodified.
      *
      * @param functor The functor to attempt to map onto a built-in.
      * @return The functor unmodified, or a {@link BuiltInFunctor} replacement for it.
      */
//     public
//     Functor apply ( Functor functor ) {
//         FunctorName functorName = defaultBuiltIn.getInterner().getFunctorFunctorName(functor);
//
//         Class <? extends HiTalkBuiltInFunctor> builtInClass;
//
//         builtInClass = builtIns.get(functorName);
//
//         if (builtInClass != null) {
//             return newInstance(getConstructor(
//                     builtInClass,
//                     new Class[]{Functor.class, HiTalkDefaultBuiltIn.class}),
//                     new Object[]{functor, defaultBuiltIn});
//         }
//         else {
//             return functor;
//         }
//     }
 }
