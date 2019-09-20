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
 package org.ltc.hitalk.compiler;

 import com.thesett.aima.logic.fol.*;
 import com.thesett.aima.logic.fol.isoprologparser.Token;
 import com.thesett.aima.logic.fol.isoprologparser.TokenSource;
 import com.thesett.aima.logic.fol.wam.builtins.BuiltInFunctor;
 import com.thesett.common.parsing.SourceCodeException;
 import com.thesett.common.util.Function;
 import org.ltc.hitalk.compiler.bktables.IApplication;
 import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
 import org.ltc.hitalk.entities.*;
 import org.ltc.hitalk.parser.HtClause;
 import org.ltc.hitalk.term.ListTerm;
 import org.ltc.hitalk.term.io.HiTalkStream;
 import org.ltc.hitalk.wam.compiler.DirectiveClause;
 import org.ltc.hitalk.wam.compiler.HiTalkDefaultBuiltIn;
 import org.ltc.hitalk.wam.compiler.HtFunctor;
 import org.ltc.hitalk.wam.compiler.HtTokenSource;

 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.nio.file.Path;
 import java.nio.file.Paths;
 import java.util.*;
 import java.util.concurrent.atomic.AtomicInteger;

 import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.*;
 import static org.ltc.hitalk.core.BuiltIns.ENCODING;
 import static org.ltc.hitalk.entities.IRelation.*;
 import static org.ltc.hitalk.parser.HtPrologParser.BEGIN_OF_FILE;
 import static org.ltc.hitalk.parser.HtPrologParser.END_OF_FILE;

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
 class HiTalkBuiltInTransform<A extends IApplication, T, Q> implements Function <Functor, Functor> {

//     public static final Pair EXTENDS_OBJECT = Pair.of(EXTENDS, OBJECT);
//     public static final Pair EXTENDS_CATEGORY = Pair.of(EXTENDS, CATEGORY);
//     public static final Pair EXTENDS_PROTOCOL = Pair.of(EXTENDS, PROTOCOL);
//     public static final Pair IMPLEMENTS_PROTOCOL = Pair.of(IMPLEMENTS, PROTOCOL);
//     public static final Pair IMPORTS_CATEGORY = Pair.of(IMPORTS, CATEGORY);
//     public static final Pair SPECIALIZES_CLASS = Pair.of(SPECIALIZES, OBJECT);
//     public static final Pair INSTANTIATES_CLASS = Pair.of(INSTANTIATES, OBJECT);

//     static {
//         Map <String, HtRelationKind> tmp = new HashMap <>();
//
//         tmp.put(EXTENDS, HtRelationKind.EXTENDS);
//         tmp.put(IMPLEMENTS, HtRelationKind.IMPLEMENTS);
//         tmp.put(IMPORTS, HtRelationKind.);
//         tmp.put(COMPLEMENTS, HtRelationKind.COMPLEMENTS);
//         tmp.put(INSTANTIATES, HtRelationKind.INSTANTIATES);
//         tmp.put(SPECIALIZES, HtRelationKind.SPECIALIZES);
//
//         relationKindMap = Collections.unmodifiableMap(tmp);
//     }

     /**
      * Holds a mapping from functor names to built-in implementations.
      */
//     private final Map <HtFunctorName, Predicate <Functor>> builtIns = new HashMap <>();
     private final PredicateTable builtIns = new PredicateTable();

     /**
      * Holds the default built in, for standard compilation and interners and symbol tables.
      */
     protected final HiTalkDefaultBuiltIn defaultBuiltIn;
     protected final VariableAndFunctorInterner interner;
     protected final A app;

     ///////////////////////////
     private HtEntityIdentifier entityCompiling;
     private IRelation lastRelation;
     private DirectiveClause lastDirective;
     private Term lastTerm;

     protected final AtomicInteger objectCounter = new AtomicInteger(0);
     protected final AtomicInteger categoryCounter = new AtomicInteger(0);
     protected final AtomicInteger protocolCounter = new AtomicInteger(0);
     protected final Resolver <T, Q> resolver;
//IMPLEMENT BUILTINS AS THE CONSUMER

     /**
      * Initializes the built-in transformation by population the the table of mappings of functors onto their built-in
      * implementations.
      *
      * @param defaultBuiltIn The default built in, for standard compilation and interners and symbol tables.
      * @param app
      * @param resolver
      */
     public
     HiTalkBuiltInTransform ( HiTalkDefaultBuiltIn defaultBuiltIn, A app,
                              Resolver <T, Q> resolver ) {
         this.defaultBuiltIn = defaultBuiltIn;
         interner = defaultBuiltIn.getInterner();
         this.resolver = resolver;
         this.app = app;
         defineBuiltIns();


//***************************************************************************************************************

//         Primitive character I/O
//         nl/0
//         nl/1
//         put/1
//         put/2
//         put_byte/1
//         put_byte/2
//         put_char/1
//         put_char/2
//         put_code/1
//         put_code/2
//         tab/1
//         tab/2
//         flush_output/0
//         flush_output/1
//         ttyflush/0
//         get_byte/1
//         get_byte/2
//         get_code/1
//         get_code/2
//         get_char/1
//         get_char/2
//         get0/1
//         get0/2
//         get/1
//         get/2
//         peek_byte/1
//         peek_byte/2
//         peek_code/1
//         peek_code/2
//         peek_char/1
//         peek_char/2
//         peek_string/3
//         skip/1
//         skip/2
//         get_single_char/1
//         with_tty_raw/1
//         at_end_of_stream/0
//         at_end_of_stream/1
//         set_end_of_stream/1
//         copy_stream_data/3
//         copy_stream_data/2
//         fill_buffer/1
//         read_pending_codes/3
//         read_pending_chars/3
//
//
//***************************************************************************************************************
//        name/2
//        term_to_atom/2
//        atom_to_term/3
//        C/3
//        atomic_concat/3
//        atomic_list_concat/2
////        atomic_list_concat/3
////        atom_length/2
////        atom_prefix/2
////        sub_atom/5
////        sub_atom_icasechk/3
     }

     private
     void defineBuiltIns () {
//         builtIns.put(new HtFunctorName(TRUE, 0), this::true_p);
//         builtIns.put(new HtFunctorName(FAIL, 0), this::fail_p);
//         builtIns.put(new HtFunctorName(FALSE, 0), this::fail_p);
//         builtIns.put(new HtFunctorName(CUT, 0), this::cut_p);
//         builtIns.put(new HtFunctorName(NOT, 0), this::not_p);
//
//         builtIns.put(new HtFunctorName(UNIFIES, 2), this::unifies_p);
//         builtIns.put(new HtFunctorName(ASSIGN, 2), this::assign_p);
//         builtIns.put(new HtFunctorName(NON_UNIFIES, 2), this::nonUnifies_p);
//         builtIns.put(new HtFunctorName(SEMICOLON, 2), this::disjunction_p);
//         builtIns.put(new HtFunctorName(COMMA, 2), this::conjunction_p);
//         builtIns.put(new HtFunctorName(CALL, 1), this::call_p);
//         builtIns.put(new HtFunctorName(OBJECT, 1, 5), this::object_p);//todo 1-5
//         builtIns.put(new HtFunctorName(PROTOCOL, 1, 2), this::protocol_p);//todo 1-2
//         builtIns.put(new HtFunctorName(CATEGORY, 1, 4), this::category_p);//todo 1-4
//         builtIns.put(new HtFunctorName(END_OBJECT, 0), this::end_object_p);
//         builtIns.put(new HtFunctorName(END_PROTOCOL, 0), this::end_protocol_p);
//         builtIns.put(new HtFunctorName(END_CATEGORY, 0), this::end_category_p);
//
//         builtIns.put(new HtFunctorName(OP, 3), this::op_p);
//         builtIns.put(new HtFunctorName(CURRENT_OP, 3), this::current_op_p);
//
//         builtIns.put(new HtFunctorName(INITIALIZATION, 1), this::initialization_p);
//         builtIns.put(new HtFunctorName(PUBLIC, 1), this::public_p);
//         builtIns.put(new HtFunctorName(PROTECTED, 1), this::protected_p);
//         builtIns.put(new HtFunctorName(PRIVATE, 1), this::private_p);
////source_file DIRECTIVES
//         builtIns.put(new HtFunctorName(INCLUDE, 1), this::include_p);
//         builtIns.put(new HtFunctorName(ENCODING, 1), this::encoding_p);
//         builtIns.put(new HtFunctorName(CURRENT_LOGTALK_FLAG, 2), this::current_logtalk_flag_p);
//         builtIns.put(new HtFunctorName(SET_LOGTALK_FLAG, 2), this::set_logtalk_flag_p);
//         builtIns.put(new HtFunctorName(CREATE_LOGTALK_FLAG, 3), this::create_logtalk_flag_p);
//
//         builtIns.put(new HtFunctorName(MULTIFILE, 1), this::multifile_p);
//         builtIns.put(new HtFunctorName(DISCONTIGUOUS, 1), this::discontiguous_p);
//         builtIns.put(new HtFunctorName(DYNAMIC, 1), this::dynamic_p);
//         builtIns.put(new HtFunctorName(STATIC, 1), this::static_p);
//
//         builtIns.put(new HtFunctorName(HILOG, 1), this::hilog_p);
//
//         builtIns.put(new HtFunctorName(EXPAND_GOAL, 1), this::expand_goal_p);
//         builtIns.put(new HtFunctorName(EXPAND_TERM, 1), this::expand_term_p);
//
//         builtIns.put(new HtFunctorName(LOGTALK_LIBRARY_PATH, 1), this::logtalk_library_path_p);
//
////          entity properties
//         builtIns.put(new HtFunctorName(OBJECT_PROPERTY, 2), this::object_property_p);
//         builtIns.put(new HtFunctorName(PROTOCOL_PROPERTY, 2), this::protocol_property_p);
//         builtIns.put(new HtFunctorName(CATEGORY_PROPERTY, 2), this::category_property_p);
////          entity enumeration
//         builtIns.put(new HtFunctorName(CURRENT_PROTOCOL, 1), this::current_protocol_p);
//         builtIns.put(new HtFunctorName(CURRENT_CATEGORY, 1), this::current_category_p);
//         builtIns.put(new HtFunctorName(CURRENT_OBJECT, 1), this::current_object_p);
//// entity creation predicates
//         builtIns.put(new HtFunctorName(CREATE_OBJECT, 1), this::create_object_p);
//         builtIns.put(new HtFunctorName(CREATE_CATEGORY, 1), this::create_category_p);
//         builtIns.put(new HtFunctorName(CREATE_PROTOCOL, 1), this::create_protocol_p);
//// entity abolishing predicates
//         builtIns.put(new HtFunctorName(ABOLISH_OBJECT, 1), this::abolish_object_p);
//         builtIns.put(new HtFunctorName(ABOLISH_CATEGORY, 1), this::abolish_category_p);
//         builtIns.put(new HtFunctorName(ABOLISH_PROTOCOL, 1), this::abolish_protocol_p);
//// entity relations
//         builtIns.put(new HtFunctorName(HtConstants.IMPLEMENTS_PROTOCOL, 2, 3), this::implements_protocol_p);
//         builtIns.put(new HtFunctorName(HtConstants.IMPORTS_CATEGORY, 2, 3), this::imports_category_p);
//         builtIns.put(new HtFunctorName(HtConstants.INSTANTIATES_CLASS, 2, 3), this::instantiates_class_p);
//         builtIns.put(new HtFunctorName(HtConstants.SPECIALIZES_CLASS, 2, 3), this::specializes_class_p);
//         builtIns.put(new HtFunctorName(HtConstants.EXTENDS_PROTOCOL, 2, 3), this::extends_protocol_p);
//         builtIns.put(new HtFunctorName(HtConstants.EXTENDS_OBJECT, 2, 3), this::extends_object_p);
//         builtIns.put(new HtFunctorName(HtConstants.EXTENDS_CATEGORY, 2, 3), this::extends_category_p);
//         builtIns.put(new HtFunctorName(COMPLEMENTS_OBJECT, 2), this::complements_object_p);
////          protocol conformance
//         builtIns.put(new HtFunctorName(CONFORMS_TO_PROTOCOL, 2, 3), this::conforms_to_protocol_p);
////          events
//         builtIns.put(new HtFunctorName(ABOLISH_EVENTS, 1), this::abolish_events_p);
//         builtIns.put(new HtFunctorName(DEFINE_EVENTS, 1), this::define_events_p);
//         builtIns.put(new HtFunctorName(CURRENT_EVENT, 1), this::current_event_p);
////      termio   read
//         builtIns.put(new HtFunctorName(READ, 1, 2), this::read_p);
//         builtIns.put(new HtFunctorName(CURRENT_INPUT, 1, 2), this::current_input_p);
//         builtIns.put(new HtFunctorName(CURRENT_OUTPUT, 1, 2), this::current_output_p);
//
////vintage edinburg LIB
//         builtIns.put(new HtFunctorName(TELL, 1), this::tell_p);
//         builtIns.put(new HtFunctorName(TELLING, 1), this::telling_p);
//         builtIns.put(new HtFunctorName(TOLD, 0), this::told_p);
//         builtIns.put(new HtFunctorName(APPEND, 1), this::append1_p);
//
//         builtIns.put(new HtFunctorName(SEE, 2), this::see_p);
//         builtIns.put(new HtFunctorName(SEEING, 2), this::seeing_p);
//         builtIns.put(new HtFunctorName(SEEN, 2), this::seen_p);
//
//         builtIns.put(new HtFunctorName(TTYFLUSH, 0), this::ttyflush_p);
//         builtIns.put(new HtFunctorName(NL, 0, 1), this::nl_p);
//         builtIns.put(new HtFunctorName(FUNCTOR, 1), this::functor_p);
//
//
//         builtIns.put(new HtFunctorName(CURRENT_OUTPUT, 2), this::atom_chars_p);
//         builtIns.put(new HtFunctorName(CURRENT_OUTPUT, 2), this::char_code_p);
//         builtIns.put(new HtFunctorName(CURRENT_OUTPUT, 2), this::number_chars_p);
//         builtIns.put(new HtFunctorName(ATOM_NUMBER, 2), this::atom_number_p);
//         builtIns.put(new HtFunctorName(NAME, 2), this::name_p);
//         builtIns.put(new HtFunctorName(TERM_TO_ATOM, 2), this::term_to_atom_p);
//         builtIns.put(new HtFunctorName(ATOM_TO_TERM, 3), this::atom_to_term_p);
//         builtIns.put(new HtFunctorName(ATOM_CONCAT, 3), this::atom_concat_p);
//         builtIns.put(new HtFunctorName(ATOMIC_CONCAT, 3), this::atomic_concat_p);
//         builtIns.put(new HtFunctorName(ATOMIC_LIST_CONCAT, 2, 3), this::atomic_list_concat_p);
//         builtIns.put(new HtFunctorName(ATOM_LENGTH, 2), this::atom_length_p);
//         builtIns.put(new HtFunctorName(ATOM_PREFIX, 2), this::atom_prefix_p);
//         builtIns.put(new HtFunctorName(SUB_ATOM, 2), this::sub_atom_p);
//         builtIns.put(new HtFunctorName(SUB_ATOM_ICASECHK, 2), this::sub_atom_icasechk_p);
     }

     private
     boolean nl_p ( Functor functor ) {
         return false;
     }

     private
     boolean ttyflush_p ( Functor functor ) {
         return false;
     }

     private
     boolean seen_p ( Functor functor ) {
         return false;
     }

     private
     boolean functor_p ( Functor functor ) {
         return false;
     }

     private
     boolean char_code_p ( Functor functor ) {
         return false;
     }

     private
     boolean seeing_p ( Functor functor ) {
         return false;
     }

     private
     boolean append1_p ( Functor functor ) {
         return false;
     }

     private
     boolean told_p ( Functor functor ) {
         return false;
     }

     private
     boolean atom_chars_p ( Functor functor ) {
         return false;
     }

     private
     boolean telling_p ( Functor functor ) {
         return false;
     }

     private
     boolean tell_p ( Functor functor ) {
         return false;
     }

     private
     boolean see_p ( Functor functor ) {
         return false;
     }

     private
     boolean number_chars_p ( Functor functor ) {
         return true;
     }

     private
     boolean atom_number_p ( Functor functor ) {
         return false;
     }

     private
     boolean name_p ( Functor functor ) {
         return false;
     }

     private
     boolean term_to_atom_p ( Functor functor ) {
         return false;
     }

     private
     boolean atom_to_term_p ( Functor functor ) {
         return false;
     }

     private
     boolean atom_concat_p ( Functor functor ) {
         return false;
     }

     private
     boolean atomic_concat_p ( Functor functor ) {
         return false;
     }

     private
     boolean atomic_list_concat_p ( Functor functor ) {
         return false;
     }

     private
     boolean atom_length_p ( Functor functor ) {
         return false;
     }

     private
     boolean atom_prefix_p ( Functor functor ) {
         return false;
     }

     private
     boolean sub_atom_p ( Functor functor ) {
         return false;
     }

     private
     boolean sub_atom_icasechk_p ( Functor functor ) {
         return false;
     }

     private
     boolean current_input_p ( Functor functor ) {
         try {//(/*InputStream input = in*/) {
             HiTalkStream input;
             throw new IOException();
         } catch (IOException e) {
             throw new ExecutionError(RESOURCE_ERROR, null);
         }

//         return false;
     }

     private
     boolean current_output_p ( Functor functor ) {

         return false;
     }

     private
     boolean read_p ( Functor functor ) {
//         parser = (HtPrologParser) getParser();
         try {
             Term term = app.getParser().termSentence();
             String name = interner.getFunctorName((Functor) term);
             while (!name.equals(END_OF_FILE)) {
                 if (BEGIN_OF_FILE.equals(name)) {
                     term = app.getParser().term();
                     if (isEncodingDirective((Functor) term)) {//run directive
                         Token token = app.getParser().lastToken();
                         app.getParser().getTokenSource().setOffset(token.endLine, token.endColumn);
                         lastTerm = term;
                     }
                 }
             }
             lastTerm = term;
         } catch (SourceCodeException e) {
             throw new ExecutionError(PERMISSION_ERROR, null);
         }
         return false;
     }
//    List <T> clauses = preprocess(sentence.getT());
//    for (Clause clause : clauses) {
//        substituteBuiltIns(clause);
//        initializeSymbolTable(clause);
//        topLevelCheck(clause);
//
//        if (observer != null) {
//            final Clause finalClause = clause;
//            if (Objects.requireNonNull(clause).isQuery()) {
//                observer.onQueryCompilation(() -> finalClause);
//            }
//            else {
//                observer.onCompilation(() -> finalClause);
//            }
//        }
//    }
//} catch (SourceCodeException e) {
//    throw new ExecutionError();
//}
//         return false;
//     }

     private
     boolean create_logtalk_flag_p ( Functor functor ) {
         return false;
     }

     private
     boolean set_logtalk_flag_p ( Functor functor ) {
         return false;
     }

     private
     boolean current_logtalk_flag_p ( Functor functor ) {
         return false;
     }

     private
     boolean current_event_p ( Functor functor ) {
         return false;
     }

     private
     boolean define_events_p ( Functor functor ) {
         return false;
     }

     private
     boolean abolish_events_p ( Functor functor ) {
         return true;
     }

     private
     boolean conforms_to_protocol_p ( Functor functor ) {
         return false;
     }

     private
     boolean complements_object_p ( Functor functor ) {
         return false;
     }

     private
     boolean extends_category_p ( Functor functor ) {
         return false;
     }

     private
     boolean extends_object_p ( Functor functor ) {
         return false;
     }

     private
     boolean extends_protocol_p ( Functor functor ) {
         return false;
     }

     private
     boolean specializes_class_p ( Functor functor ) {
         return false;
     }

     private
     boolean instantiates_class_p ( Functor functor ) {
         return false;
     }

     private
     boolean imports_category_p ( Functor functor ) {
         return false;
     }

     private
     boolean implements_protocol_p ( Functor functor ) {
         return false;
     }

     private
     boolean abolish_object_p ( Functor functor ) {
         return false;
     }

     private
     boolean abolish_protocol_p ( Functor functor ) {
         return false;
     }

     private
     boolean create_protocol_p ( Functor functor ) {
         return false;
     }

     private
     boolean create_category_p ( Functor functor ) {
         return false;
     }

     private
     boolean current_object_p ( Functor functor ) {

         return true;
     }

     private
     boolean current_category_p ( Functor functor ) {
         return false;
     }

     private
     boolean current_protocol_p ( Functor functor ) {
         return false;
     }

     private
     boolean category_property_p ( Functor functor ) {
         return false;
     }

     private
     boolean abolish_category_p ( Functor functor ) {
         return false;
     }

     private
     boolean protocol_property_p ( Functor functor ) {
         return false;
     }

     private
     boolean object_property_p ( Functor functor ) {
         return false;
     }

     private
     boolean logtalk_library_path_p ( Functor functor ) {
         return false;
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
         return createObject(functor);
     }

     private
     boolean createObject ( Functor functor ) {
         Functor identifier = (Functor) functor.getArgument(0);
         ListTerm relations = (ListTerm) functor.getArgument(1);
         ListTerm directives = (ListTerm) functor.getArgument(2);
         ListTerm clauses = (ListTerm) functor.getArgument(3);

         return false;
     }

     private
     boolean encoding_p ( Functor functor ) {
         Token token = app.getParser().lastToken();
         app.getParser().getTokenSource().setOffset(token.endLine, token.endColumn);
//         app.getParser().getTokenSource().setFileBeginPos(app.getParser().);
         lastTerm = new Functor(interner.internFunctorName(BEGIN_OF_FILE, 0), null);
         String encoding = String.valueOf(functor.getArgument(0));
         HiTalkStream in = currentinput();
//         Charset cs = Charset.forName(encoding);
         return true;
     }

     private
     HiTalkStream currentinput () {
         return null;
     }
//
//     private
//     boolean multifile_p ( Functor functor ) {
//         functor.setProperty(MULTIFILE, functor.getArgument(0));
//         return true;
//     }
//
//     private
//     boolean dynamic_p ( Functor functor ) {
//         functor.setProperty(DYNAMIC, functor.getArgument(0));
//         return true;
//     }
//
//     private
//     boolean static_p ( Functor functor ) {
//         functor.setProperty(STATIC, functor.getArgument(0));
//         return true;
//     }

     private
     boolean include_p ( Functor functor ) {
         try {
             Path path = expandSourceFileName((Functor) functor.getArgument(0));
             TokenSource tokenSource = HtTokenSource.getTokenSourceForFile(path.toFile());//bof/eof
             app.setTokenSource(tokenSource);
         } catch (FileNotFoundException fnfe) {
             throw new ExecutionError(EXISTENCE_ERROR, null);
         }

         return true;
     }

     public
     Path expandSourceFileName ( Functor functor ) {
         String filename = interner.getFunctorName(functor);

         if (functor.isAtom()) {
             return null;//fixme aliases + \-->/
         }
         else {
             if (functor.getArity() == 1) {
//                 libPath = interner.getFunctorName(functor)
             }
         }
         return null;
     }

     /**
      * @param functor
      * @return
      */
     @SuppressWarnings("ToArrayCallWithZeroLengthArrayArgument")
     public
     Path convertsToOsFileName ( Functor functor ) {
         List <String> names = new ArrayList <>();
         for (; ; functor = (Functor) functor.getArgument(0)) {
             String name = interner.getFunctorName(functor);
             if (functor.isAtom()) {
                 return Paths.get(name, names.toArray(new String[names.size()])).toAbsolutePath();//fixme
             }
             if (functor.getArity() == 1) {
                 names.add(name);///fixme
             }
             else {
                 if (!functor.isGround()) {
                     throw new ExecutionError(INSTANTIATION_ERROR, null);
                 }
                 else {
                     throw new ExecutionError(TYPE_ERROR, null);
                 }
             }
         }
     }

     String[] getSourceFilenameExtensions () {
         return new String[]{
                 ".hlgt",
                 ".hl",
                 ".lgt",
                 ".pl",
                 ".hitalk",
                 ".logtalk",
                 ".prolog",
                 ".hilog",
                 "."
         };
     }

     private
     Path expandLibraryAlias ( Functor library ) {
         Functor location = logtalkLibraryPath(library);
         return Paths.get("");
     }

     private
     Functor logtalkLibraryPath ( Functor library ) {

         return null;
     }

     private
     Path expandPath ( Functor sourceFileName ) {
         List <String> names = new ArrayList <>();
         for (Functor functor = sourceFileName; ; functor = (Functor) functor.getArgument(0)) {
             String name = interner.getFunctorName(functor);
             if (functor.isAtom()) {
                 return Paths.get(name, names.toArray(new String[names.size()])).toAbsolutePath();
             }
             if (functor.getArity() == 1) {
                 names.add(name);
             }
             else {
                 if (!functor.isGround()) {
                     throw new ExecutionError(INSTANTIATION_ERROR, null);
                 }
                 else {
                     throw new ExecutionError(TYPE_ERROR, null);
                 }
             }
         }
     }

//=====================================================
//
     /*
'$lgt_check_and_expand_source_file'(File, Path) :-
	(	atom(File) ->
		'$lgt_prolog_os_file_name'(NormalizedFile, File),
		(	sub_atom(NormalizedFile, 0, 1, _, '$') ->
			'$lgt_expand_path'(NormalizedFile, Path)
		;	Path = NormalizedFile
		)
	;	compound(File),
		File =.. [Library, Basename],
		atom(Basename) ->
		% library notation
		'$lgt_prolog_os_file_name'(NormalizedBasename, Basename),
		(	'$lgt_expand_library_alias'(Library, Directory) ->
			atom_concat(Directory, NormalizedBasename, Path)
		;	throw(error(existence_error(library, Library), _))
		)
	;	% invalid source file specification
		ground(File) ->
		throw(error(type_error(source_file_name, File), _))
	;	throw(error(instantiation_error, _))
	).

 */
//     private
//     boolean extends_p ( Functor functor ) {
//         createRelation(functor, HtRelationKind.EXTENDS, false);
//         return true;
//     }
//
//     private
//     boolean implements_p ( Functor functor ) {
//         createRelation(functor, HtRelationKind.IMPLEMENTS, false);
//         return true;
//     }
//
//     private
//     boolean imports_p ( Functor functor ) {
//         createRelation(functor, HtRelationKind.IMPORTS, false);
//         return true;
//     }
//
//     private
//     boolean complements_p ( Functor functor ) {
//         createRelation(functor, HtRelationKind.COMPLEMENTS, false);
//         return true;
//     }
//
//     private
//     boolean instantiates_p ( Functor functor ) {
//         createRelation(functor, HtRelationKind.INSTANTIATES, false);
//         return true;
//     }
//
//     private
//     boolean specializes_p ( Functor functor ) {
//         createRelation(functor, HtRelationKind.SPECIALIZES, false);
//         return true;
//     }

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
     boolean true_p ( Functor functor ) {
         return true;
     }

     private
     boolean fail_p ( Functor functor ) {
         return false;
     }

     private
     boolean cut_p ( Functor functor ) {
         return true;
     }

     private
     boolean unifies_p ( Functor functor ) {
         boolean result = false;

         return result;
     }

     private
     boolean nonUnifies_p ( Functor functor ) {
         boolean result = false;

         return result;
     }

     private
     boolean assign_p ( Functor functor ) {
         return true;
     }

     private
     boolean conjunction_p ( Functor functor ) {
         boolean result = false;

         return result;
     }

     private
     boolean disjunction_p ( Functor functor ) {
         boolean result = false;

         return result;
     }

     private
     boolean call_p ( Functor functor ) {
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
             throw new ExecutionError(PERMISSION_ERROR, null);
         }
         entityCompiling = new HtEntityIdentifier(functor, HtEntityKind.OBJECT);
         handleEntityRelations((HtFunctor) functor, false);

         return true;
     }

     @SuppressWarnings("SuspiciousMethodCalls")
     private
     void handleEntityRelations ( HtFunctor entityFunctor, boolean dynamic ) {
         int arityMax = entityFunctor.getArityMin() + entityFunctor.getArityDelta();
//         HtEntityKind entityKind = entityCompiling.getKind();
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
                 if (entityFunctor.getArityDelta() != 0) {
                     FunctorName name = interner.getDeinternedFunctorName(entityFunctor.getName());
                     relationFunctor = (Functor) entityFunctor.getArgument(1);
                     createRelation(relationFunctor, null, dynamic);
                 }
             }
         }
     }

     private
     void handleNormalizedRelations ( Functor functor,
                                      EnumSet <HtRelationKind> kinds,
                                      List <Set <IRelation>> relations,
                                      boolean dynamic ) {

         String name = interner.getFunctorName(functor);
//         HtRelationKind relationKind = relationKindMap.get(name);

         HtRelationKind relationKind = HtRelationKind.EXTENDS_OBJECT;///default
         if (kinds.add(relationKind)) {
             Set <IRelation> arr = new HashSet <>();
//             List<List<IRelation>> newRelData = new ArrayList <>();
             relations.add(arr);

//             newRelData.add(arr);
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
         HtScope scope = new HtScope(PUBLIC);//default scope
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
     HtEntityKind detectSubEntityKind ( HtRelationKind relationKind, HtEntityKind kind ) {
         return null;
     }

     private
     boolean isList ( Term relationTerm ) {
         return relationTerm instanceof ListTerm;
     }

//     private
//     HtEntityKind detectSubEntityKind ( HtRelationKind relationKind, HtEntityKind superKind ) {
//         HtEntityKind result = HtEntityKind.OBJECT;
//         switch (relationKind) {
//             case EXTENDS:
//                 result = superKind;
//                 break;
//             case IMPLEMENTS:
//                 if (superKind == HtEntityKind.OBJECT || superKind == HtEntityKind.CATEGORY) {
//                     result = HtEntityKind.PROTOCOL;
//                     break;
//                 }
//                 else {
//                     throw new ExecutionError(PERMISSION_ERROR, null);//protocol implememnts protocp;!!!!!!!!!
//                 }
//             case IMPORTS:
//                 if (superKind == HtEntityKind.OBJECT) {
//                     result = HtEntityKind.CATEGORY;
//                     break;
//                 }
//             case COMPLEMENTS:
//                 if (superKind == HtEntityKind.CATEGORY) {
//                     break;
//                 }
//             case SPECIALIZES:
//             case INSTANTIATES:
//                 break;
//             default:
//                 throw new ExecutionError(PERMISSION_ERROR, null/* + superKind + "/" + relationKind*/);
//         }
//
//         return result;
//     }

     private
     boolean isEntityCompiling () {
         return entityCompiling != null;
     }

     private
     boolean protocol_p ( Functor functor ) {
         if (isEntityCompiling()) {
             throw new ExecutionError(PERMISSION_ERROR, null);
         }
         entityCompiling = new HtEntityIdentifier(functor, HtEntityKind.PROTOCOL);
         handleEntityRelations((HtFunctor) functor, false);

         return true;
     }

     /**
      * @param functor
      * @return
      */
     protected
     boolean isEncodingDirective ( Functor functor ) {
         boolean result = false;
         if (isDirective(functor)) {
             functor = (Functor) functor.getArgument(0);
             String name = interner.getFunctorName(functor);
             result = ENCODING.equals(name) && functor.getArity() == 1;
         }
         return result;
     }

     /**
      * @param functor
      * @return
      */
     private
     boolean isDirective ( Functor functor ) {
         return interner.getFunctorName(functor).equals(IMPLIES) && functor.getArity() == 1;
     }

     private
     boolean category_p ( Functor functor ) {
         if (isEntityCompiling()) {
             throw new ExecutionError(PERMISSION_ERROR, null);
         }
         entityCompiling = new HtEntityIdentifier(functor, HtEntityKind.CATEGORY);
         handleEntityRelations((HtFunctor) functor, false);

         return true;
     }

     public
     AtomicInteger getObjectCounter () {
         return objectCounter;
     }

     public
     AtomicInteger getCategoryCounter () {
         return categoryCounter;
     }

     public
     AtomicInteger getProtocolCounter () {
         return protocolCounter;
     }

     public
     Resolver <T, Q> getResolver () {
         return resolver;
     }

     private
     boolean end_object_p ( Functor functor ) {
         if (getObjectCounter().get() == 0) {
             throw new ExecutionError(PERMISSION_ERROR, null);
         }
         ;
         return endEntity(HtEntityKind.OBJECT);
     }

     private
     boolean end_protocol_p ( Functor functor ) {
         if (getProtocolCounter().get() == 0) {
             throw new ExecutionError(PERMISSION_ERROR, null);
         }
         return endEntity(HtEntityKind.PROTOCOL);
     }

     private
     boolean end_category_p ( Functor functor ) {
         if (getCategoryCounter().get() == 0) {
             throw new ExecutionError(PERMISSION_ERROR, null);
         }
         return endEntity(HtEntityKind.CATEGORY);
     }

     boolean endEntity ( HtEntityKind entityKind ) {
         try {
             if (entityCompiling == null || entityCompiling.getKind() != entityKind) {
                 throw new ExecutionError(PERMISSION_ERROR, null);
             }
         } finally {
//             entityCompiling;
             //ending object
             entityCompiling = null;
             lastDirective = null;
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
//======================================================
//          atom_codes/2
////        atom_chars/2
////        char_code/2
////        number_chars/2
////        number_codes/2
////        atom_number/2
////        name/2
////        term_to_atom/2
////        atom_to_term/3
////        atom_concat/3
////        atomic_concat/3
////        atomic_list_concat/2
////        atomic_list_concat/3
////        atom_length/2
////        atom_prefix/2
////        sub_atom/5
////        sub_atom_icasechk/3
//======================================================

     //
     public
     VariableAndFunctorInterner getInterner () {
         return interner;
     }

     public
     HtClause getLastDirective () {
         return lastDirective;
     }

     public
     IRelation getLastRelation () {
         return lastRelation;
     }

     public
     Term getLastTerm () {
         return lastTerm;
     }
 }
