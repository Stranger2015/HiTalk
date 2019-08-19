%
% True when SrcDest can be opened in Mode and Stream is an I/O stream to/from the object.
%SrcDest is normally the name of a file, represented as an atom or string.
%Mode is one of read, write, append or update.
%Mode append opens the file for writing, positioning the file pointer at the end.
%Mode update opens the file for writing, positioning the file pointer at the beginning of the file without truncating the file.
%Stream is either a variable, in which case it is bound to an integer identifying the stream, or an atom,
%in which case this atom will be the stream identifier.
  %
  %SWI-Prolog also allows SrcDest to be a term pipe(Command). In this form, Command is started as a child process
%   and if Mode is write, output written to Stream is sent to the standard input of Command. Viso versa, if Mode is read, data written by Command to the standard output may be read from Stream. On Unix systems, Command is handed to popen() which hands it to the Unix shell. On Windows, Command is executed directly. See also process_create/3 from library(process).
  %
  %If SrcDest is an IRI, i.e., starts with <scheme>://, where <scheme> is a non-empty sequence of lowercase ASCII letters open/3,4 calls hooks registered by register_iri_scheme/3. Currently the only predefined IRI scheme is res, providing access to the resource database. See section 13.4.
  %
  %The following Options are recognised by open/4:
  %
  %alias(Atom)
  %    Gives the stream a name. Below is an example. Be careful with this option as stream names are global. See also set_stream/2.
  %
  %    ?- open(data, read, Fd, [alias(input)]).
  %
  %            ...,
  %            read(input, Term),
  %            ...
  %
  %bom(Bool)
  %    Check for a BOM (Byte Order Marker) or write one. If omitted, the default is true for mode read and false for mode write. See also stream_property/2 and especially section 2.20.1.1 for a discussion of this feature.
  %buffer(Buffering)
  %    Defines output buffering. The atom full (default) defines full buffering, line buffering by line, and false implies the stream is fully unbuffered. Smaller buffering is useful if another process or the user is waiting for the output as it is being produced. See also flush_output/[0,1]. This option is not an ISO option.
  %close_on_abort(Bool)
  %    If true (default), the stream is closed on an abort (see abort/0). If false, the stream is not closed. If it is an output stream, however, it will be flushed. Useful for logfiles and if the stream is associated to a process (using the pipe/1 construct).
  %create(+List)
  %    Specifies how a new file is created when opening in write, append or update mode. Currently, List is a list of atoms that describe the permissions of the created file.89 Defined values are below. Not recognised values are silently ignored, allowing for adding platform specific extensions to this set.
  %
  %    read
  %        Allow read access to the file.
  %    write
  %        Allow write access to the file.
  %    execute
  %        Allow execution access to the file.
  %    default
  %        Allow read and write access to the file.
  %    all
  %        Allow any access provided by the OS.
  %
  %    Note that if List is empty, the created file has no associated access permissions. The create options map to the POSIX mode option of open(), where read map to 0444, write to 0222 and execute to 0111. On POSIX systems, the final permission is defined as (mode & ~umask).
  %encoding(Encoding)
  %    Define the encoding used for reading and writing text to this stream. The default encoding for type text is derived from the Prolog flag encoding. For binary streams the default encoding is octet. For details on encoding issues, see section 2.20.1.
  %eof_action(Action)
  %    Defines what happens if the end of the input stream is reached. The default value for Action is eof_code, which makes get0/1 and friends return -1, and read/1 and friends return the atom end_of_file. Repetitive reading keeps yielding the same result. Action error is like eof_code, but repetitive reading will raise an error. With action reset, Prolog will examine the file again and return more data if the file has grown.
  %locale(+Locale)
  %    Set the locale that is used by notably format/2 for output on this stream. See section 4.23.
  %lock(LockingMode)
  %    Try to obtain a lock on the open file. Default is none, which does not lock the file. The value read or shared means other processes may read the file, but not write it. The value write or exclusive means no other process may read or write the file.
  %
  %    Locks are acquired through the POSIX function fcntl() using the command F_SETLKW, which makes a blocked call wait for the lock to be released. Please note that fcntl() locks are advisory and therefore only other applications using the same advisory locks honour your lock. As there are many issues around locking in Unix, especially related to NFS (network file system), please study the fcntl() manual page before trusting your locks!
  %
  %    The lock option is a SWI-Prolog extension.
  %type(Type)
  %    Using type text (default), Prolog will write a text file in an operating system compatible way. Using type binary the bytes will be read or written without any translation. See also the option encoding.
  %wait(Bool)
  %    This option can be combined with the lock option. If false (default true), the open call returns immediately with an exception if the file is locked. The exception has the format permission_error(lock, source_sink, SrcDest).
  %
  %The option reposition is not supported in SWI-Prolog. All streams connected to a file may be repositioned.

:- mode(open(+SrcDest, +Mode, --Stream, +Options), one). %iso builtin
:- mode(open(+SrcDest, +Mode, --Stream), one). %iso builtin open(+SrcDest, +Mode, --Stream)



%%%  Open an output stream that produces no output.

% All counting functions are enabled on such a stream.
% It can be used to discard output (like Unix /dev/null) or exploit the counting properties.
% The initial encoding of Stream is utf8, enabling arbitrary Unicode output.
% The encoding can be changed to determine byte counts of the output in a particular encoding
% or validate if output is possible in a particular encoding.
% For example, the code below determines the number of characters emitted when writing Term.

%            write_length(Term, Len) :-
%                    open_null_stream(Out),
%                    write(Out, Term),
%                    character_count(Out, Len0),
%                    close(Out),
%                    Len = Len0.
%
:- mode(open_null_stream(--Stream), one).% builtin

%%%
open(SrcDest, Mode, Stream, []) :-
    open(SrcDest, Mode, Stream).

open(SrcDest, Mode, Stream, Options):-
    handle_options(Options).

:- mode(close(+Stream, +Options), one).% ISO builtin

%%%
close(Stream, []):-
    close(Stream).% builtin

close(Stream, Options):-
    handle_options(Options, Stream).

%%%
handle_options([], _ ).
handle_options([Option | Options], Stream):-
    handle_option(Option, Stream),
    handle_options(Options, Stream).

handle_option(alias(Atom), Stream).
handle_option(bom(Bool), Stream).
handle_option(buffer(Buffering), Stream).
handle_option(close_on_abort(Bool), Stream).
handle_option(create(List), Stream) :-

handle_option(encoding(Encoding), Stream).




    /**

alias(Atom)
    Gives the stream a name. Below is an example. Be careful with this option as stream names are global.
    See also set_stream/2.

    ?- open(data, read, Fd, [alias(input)]).

            ...,
            read(input, Term),
            ...

bom(Bool)
    Check for a BOM (Byte Order Marker) or write one.
    If omitted, the default is true for mode read and false for mode write.
    See also stream_property/2 and especially section 2.20.1.1 for a discussion of this feature.
buffer(Buffering)
    Defines output buffering. The atom full (default) defines full buffering, line buffering by line,
    and false implies the stream is fully unbuffered. Smaller buffering is useful if another process
    or the user is waiting for the output as it is being produced. See also flush_output/[0,1]. This option is not an ISO option.
close_on_abort(Bool)
    If true (default), the stream is closed on an abort (see abort/0). If false, the stream is not closed. If it is an output stream, however, it will be flushed. Useful for logfiles and if the stream is associated to a process (using the pipe/1 construct).
create(+List)
    Specifies how a new file is created when opening in write, append or update mode.
    Currently, List is a list of atoms that describe the permissions of the created file.
    Defined values are below. Not recognised values are silently ignored, allowing for adding platform
    specific extensions to this set.

    read
        Allow read access to the file.
    write
        Allow write access to the file.
    execute
        Allow execution access to the file.
    default
        Allow read and write access to the file.
    all
        Allow any access provided by the OS.

    Note that if List is empty, the created file has no associated access permissions. The create options map to the POSIX mode option of open(), where read map to 0444, write to 0222 and execute to 0111. On POSIX systems, the final permission is defined as (mode & ~umask).
encoding(Encoding)
    Define the encoding used for reading and writing text to this stream. The default encoding for type text is derived from the Prolog flag encoding. For binary streams the default encoding is octet. For details on encoding issues, see section 2.20.1.
eof_action(Action)
    Defines what happens if the end of the input stream is reached. The default value for Action is eof_code, which makes get0/1 and friends return -1, and read/1 and friends return the atom end_of_file. Repetitive reading keeps yielding the same result. Action error is like eof_code, but repetitive reading will raise an error. With action reset, Prolog will examine the file again and return more data if the file has grown.
locale(+Locale)
    Set the locale that is used by notably format/2 for output on this stream. See section 4.23.
lock(LockingMode)
    Try to obtain a lock on the open file. Default is none, which does not lock the file.
     The value read or shared means other processes may read the file, but not write it.
     The value write or exclusive means no other process may read or write the file.

    Locks are acquired through the POSIX function fcntl() using the command F_SETLKW,
    which makes a blocked call wait for the lock to be released. Please note that fcntl() locks are advisory and
    therefore only other applications using the same advisory locks honour your lock.
    As there are many issues around locking in Unix, especially related to NFS (network file system),

    please study the fcntl() manual page before trusting your locks!

    The lock option is a SWI-Prolog extension.
type(Type)
    Using type text (default), Prolog will write a text file in an operating system compatible way. Using type binary the bytes will be read or written without any translation. See also the option encoding.
wait(Bool)
    This option can be combined with the lock option. If false (default true), the open call returns immediately with an exception if the file is locked. The exception has the format permission_error(lock, source_sink, SrcDest).

The option reposition is not supported in SWI-Prolog. All streams connected to a file may be repositioned.
    */

%True when StreamProperty is a property of Stream. If enumeration of streams or properties is demanded because either Stream or StreamProperty are unbound, the implementation enumerates all candidate streams and properties while locking the stream database. Properties are fetched without locking the stream and may be outdated before this predicate returns due to asynchronous activity.
%alias(Atom)
%    If Atom is bound, test if the stream has the specified alias. Otherwise unify Atom with the first alias of the stream.bug
%buffer(Buffering)
%    SWI-Prolog extension to query the buffering mode of this stream. Buffering is one of full, line or false. See also open/4.
%buffer_size(Integer)
%    SWI-Prolog extension to query the size of the I/O buffer associated to a stream in bytes. Fails if the stream is not buffered.
%bom(Bool)
%    If present and true, a BOM (Byte Order Mark) was detected while opening the file for reading, or a BOM was written while opening the stream. See section 2.20.1.1 for details.
%close_on_abort(Bool)
%    Determine whether or not abort/0 closes the stream. By default streams are closed.
%close_on_exec(Bool)
%    Determine whether or not the stream is closed when executing a new process (exec() in Unix, CreateProcess() in Windows).
%Default is to close streams. This maps to fcntl() F_SETFD using the flag FD_CLOEXEC on Unix and (negated) HANDLE_FLAG_INHERIT on Windows.
%encoding(Encoding)
%    Query the encoding used for text. See section 2.20.1 for an overview of wide character and encoding issues in SWI-Prolog.
%end_of_stream(E)
%    If Stream is an input stream, unify E with one of the atoms not, at or past. See also at_end_of_stream/[0,1].
%eof_action(A)
%    Unify A with one of eof_code, reset or error. See open/4 for details.
%file_name(Atom)
%    If Stream is associated to a file, unify Atom to the name of this file.
%file_no(Integer)
%    If the stream is associated with a POSIX file descriptor, unify Integer with the descriptor number.
%SWI-Prolog extension used primarily for integration with foreign code. See also Sfileno() from SWI-Stream.h.
%input
%    True if Stream has mode read.
%locale(Locale)
%    True when Locale is the current locale associated with the stream. See section 4.23.
%mode(IOMode)
%    Unify IOMode to the mode given to open/4 for opening the stream. Values are: read, write, append and the SWI-Prolog extension update.
%newline(NewlineMode)
%    One of posix or dos. If dos, text streams will emit \r\n for \n and discard \r from input streams. Default depends on the operating system.
%nlink(-Count)
%    Number of hard links to the file. This expresses the number of `names' the file has. Not supported on all operating systems and the value might be bogus. See the documentation of fstat() for your OS and the value st_nlink.
%output
%    True if Stream has mode write, append or update.
%position(Pos)
%    Unify Pos with the current stream position. A stream position is an opaque term whose fields can be extracted using stream_position_data/3. See also set_stream_position/2.
%reposition(Bool)
%    Unify Bool with true if the position of the stream can be set (see seek/4). It is assumed the position can be set if the stream has a seek-function and is not based on a POSIX file descriptor that is not associated to a regular file.
%representation_errors(Mode)
%    Determines behaviour of character output if the stream cannot represent a character. For example, an ISO Latin-1 stream cannot represent Cyrillic characters. The behaviour is one of error (throw an I/O error exception), prolog (write \...\ escape code) or xml (write &#...; XML character entity). The initial mode is prolog for the user streams and error for all other streams. See also section 2.20.1 and set_stream/2.
%    This property is reported with Bool equal to true if the stream is associated with a terminal. See also set_stream/2.
%    write_errors(Atom)
%    Atom is one of error (default) or ignore. The latter is intended to deal with service processes for which the standard output handles are not connected to valid streams. In these cases write errors may be ignored on user_error.
%:- mode(stream_property(?Stream, ?StreamProperty), one_or_more).%ISO builtin
%
%===============================================
%
%Availability:
%:- use_module(library(quintus)).(can be autoloaded)
%   The predicate current_stream/3 is used to access the status of a stream as well as to generate all open streams.
%   Object is the name of the file opened if the stream refers to an open file, an integer file descriptor
%   if the stream encapsulates an operating system stream, or the atom [] if the stream refers to some other object.
%   Mode is one of read or write.
%   SICStus/Quintus and backward compatible predicate. New code should be using the ISO compatible stream_property/2.

  current_stream(Object, Mode, Stream) :
      stream_property(Stream, mode(FullMode)),
      stream_mode(FullMode, Mode),
      (   stream_property(Stream, file_name(Object0))
      ->  true
      ;   stream_property(Stream, file_no(Object0))
      ->  true
      ;   Object0 = []
      ),
      Object = Object0.

  stream_mode(read,   read).
  stream_mode(write,  write).
  stream_mode(append, write).
  stream_mode(update, write).

:- mode( set_stream(+Stream, +Attribute), one).
%  set_stream(+Stream, +Attribute)
   %    Modify an attribute of an existing stream. Attribute specifies the stream property to set.
%   If stream is a pair (see stream_pair/3) both streams are modified, unless the property is only
%meaningful on one of the streams or setting both is not meaningful.
% In particular, eof_action only applies to the read stream, representation_errors only applies to
%the write stream and trying to set alias or line_position on a pair results in a permission_error exception.
%See also stream_property/2 and open/4.
   %
   %    alias(AliasName)
   %        Set the alias of an already created stream. If AliasName is the name of one of the standard streams,
%   this stream is rebound. Thus, set_stream(S, current_input) is the same as set_input/1, and by setting the alias of a stream to user_input, etc., all user terminal input is read from this stream. See also interactor/0.
   %    buffer(Buffering)
   %        Set the buffering mode of an already created stream. Buffering is one of full, line or false.
   %    buffer_size(+Size)
   %        Set the size of the I/O buffer of the underlying stream to Size bytes.
   %    close_on_abort(Bool)
   %        Determine whether or not the stream is closed by abort/0. By default, streams are closed.
   %    close_on_exec(Bool)
   %        Set the close_on_exec property. See stream_property/2.
   %    encoding(Atom)
   %        Defines the mapping between bytes and character codes used for the stream. See section 2.20.1 for supported encodings. The value bom causes the stream to check whether the current character is a Unicode BOM marker. If a BOM marker is found, the encoding is set accordingly and the call succeeds. Otherwise the call fails.
   %    eof_action(Action)
   %        Set end-of-file handling to one of eof_code, reset or error.
   %    file_name(FileName)
   %        Set the filename associated to this stream. This call can be used to set the file for error locations if Stream corresponds to FileName and is not obtained by opening the file directly but, for example, through a network service.
   %    line_position(LinePos)
   %        Set the line position attribute of the stream. This feature is intended to correct position management of the stream after sending a terminal escape sequence (e.g., setting ANSI character attributes). Setting this attribute raises a permission error if the stream does not record positions. See line_position/2 and stream_property/2 (property position).
   %    locale(+Locale)
   %        Change the locale of the stream. See section 4.23.
   %    newline(NewlineMode)
   %        Set input or output translation for newlines. See corresponding stream_property/2 for details. In addition to the detected modes, an input stream can be set in mode detect. It will be set to dos if a \r character was removed.
   %    timeout(Seconds)
   %        This option can be used to make streams generate an exception if it takes longer than Seconds before any new data arrives at the stream. The value infinite (default) makes the stream block indefinitely. Like wait_for_input/3, this call only applies to streams that support the select() system call. For further information about timeout handling, see wait_for_input/3. The exception is of the form
   %
   %            error(timeout_error(read, Stream), _)
   %
   %    type(Type)
   %        Set the type of the stream to one of text or binary. See also open/4 and the encoding property of streams. Switching to binary sets the encoding to octet. Switching to text sets the encoding to the default text encoding.
   %    record_position(Bool)
   %        Do/do not record the line count and line position (see line_count/2 and line_position/2). Calling set_stream(S, record_position(true)) resets the position the start of line 1.
   %    representation_errors(Mode)
   %        Change the behaviour when writing characters to the stream that cannot be represented by the encoding. See also stream_property/2 and section 2.20.1.
   %    tty(Bool)
   %        Modify whether Prolog thinks there is a terminal (i.e. human interaction) connected to this stream. On Unix systems the initial value comes from isatty(). On Windows, the initial user streams are supposed to be associated to a terminal. See also stream_property/2.
%
%===============================================
:- mode(stream_property(?Stream, ?StreamProperty), one_or_more).

  %    True when StreamProperty is a property of Stream.
%  If enumeration of streams or properties is demanded because either Stream or StreamProperty are unbound,
%the implementation enumerates all candidate streams and properties while locking the stream database.
%Properties are fetched without locking the stream and may be outdated before this predicate returns due
%to asynchronous activity.
  %
  %    alias(Atom)
  %        If Atom is bound, test if the stream has the specified alias. Otherwise unify Atom with the first alias of
%  the stream.bug
  %    buffer(Buffering)
  %        SWI-Prolog extension to query the buffering mode of this stream. Buffering is one of full, line or false.
%  See also open/4.
  %    buffer_size(Integer)
  %        SWI-Prolog extension to query the size of the I/O buffer associated to a stream in bytes.
%  Fails if the stream is not buffered.
  %    bom(Bool)
  %        If present and true, a BOM (Byte Order Mark) was detected while opening the file for reading,
%  or a BOM was written while opening the stream. See section 2.20.1.1 for details.
  %    close_on_abort(Bool)
  %        Determine whether or not abort/0 closes the stream. By default streams are closed.
  %    close_on_exec(Bool)
  %        Determine whether or not the stream is closed when executing a new process (exec() in Unix,
%  CreateProcess() in Windows). Default is to close streams. This maps to fcntl() F_SETFD using the flag FD_CLOEXEC
%on Unix and (negated) HANDLE_FLAG_INHERIT on Windows.
  %    encoding(Encoding)
  %        Query the encoding used for text. See section 2.20.1 for an overview of wide character and encoding issues
%  in SWI-Prolog.
  %    end_of_stream(E)
  %        If Stream is an input stream, unify E with one of the atoms not, at or past. See also at_end_of_stream/[0,1].
  %    eof_action(A)
  %        Unify A with one of eof_code, reset or error. See open/4 for details.
  %    file_name(Atom)
  %        If Stream is associated to a file, unify Atom to the name of this file.
  %    file_no(Integer)
  %        If the stream is associated with a POSIX file descriptor, unify Integer with the descriptor number.
%  SWI-Prolog extension used primarily for integration with foreign code. See also Sfileno() from SWI-Stream.h.
  %    input
  %        True if Stream has mode read.
  %    locale(Locale)
  %        True when Locale is the current locale associated with the stream. See section 4.23.
  %    mode(IOMode)
  %        Unify IOMode to the mode given to open/4 for opening the stream. Values are: read, write, append and
%  the SWI-Prolog extension update.
  %    newline(NewlineMode)
  %        One of posix or dos. If dos, text streams will emit \r\n for \n and discard \r from input streams.
%  Default depends on the operating system.
  %    nlink(-Count)
  %        Number of hard links to the file. This expresses the number of `names' the file has. Not supported on all operating systems and the value might be bogus. See the documentation of fstat() for your OS and the value st_nlink.
  %    output
  %        True if Stream has mode write, append or update.
  %    position(Pos)
  %        Unify Pos with the current stream position. A stream position is an opaque term whose fields can be extracted using stream_position_data/3. See also set_stream_position/2.
  %    reposition(Bool)

  %        Unify Bool with true if the position of the stream can be set (see seek/4). It is assumed
%  the position can be set if the stream has a seek-function and is not based on a POSIX file descriptor that is not associated to a regular file.
  %    representation_errors(Mode)
  %        Determines behaviour of character output if the stream cannot represent a character.
%  For example, an ISO Latin-1 stream cannot represent Cyrillic characters. The behaviour is one of error (throw an I/O error exception), prolog (write \...\ escape code) or xml (write &#...; XML character entity). The initial mode is prolog for the user streams and error for all other streams. See also section 2.20.1 and set_stream/2.
  %    timeout(-Time)
  %        Time is the timeout currently associated with the stream. See set_stream/2 with the same option.
%   If no timeout is specified, Time is unified to the atom infinite.
  %    type(Type)
  %        Unify Type with text or binary.
  %    tty(Bool)
  %        This property is reported with Bool equal to true if the stream is associated with a terminal.
%  See also set_stream/2.
  %    write_errors(Atom)
  %        Atom is one of error (default) or ignore. The latter is intended to deal with service processes for which
%  the standard output handles are not connected to valid streams.
% In these cases write errors may be ignored on user_error.
%==============================================

%Availability:built-in
seek(+Stream, +Offset, +Method, -NewLocation).
%    Reposition the current point of the given Stream. Method is one of bof, current or eof, indicating positioning relative to the start, current point or end of the underlying object.
%NewLocation is unified with the new offset, relative to the start of the stream.
%    Positions are counted in `units'. A unit is 1 byte, except for text files using 2-byte Unicode encoding (2 bytes)
%or wchar encoding (sizeof(wchar_t)). The latter guarantees comfortable interaction with wide-character text objects.
%Otherwise, the use of seek/4 on non-binary files (see open/4) is of limited use, especially when using multi-byte text
%encodings (e.g. UTF-8) or multi-byte newline files (e.g. DOS/Windows). On text files, SWI-Prolog offers reliable backup
%to an old position using stream_property/2 and set_stream_position/2. Skipping N character codes is achieved calling
% get_code/2 N times or using copy_stream_data/3, directing the output to a null stream (see open_null_stream/1).
%If the seek modifies the current location, the line number and character position in the line are set to 0.
%    If the stream cannot be repositioned, a permission_error is raised. If applying the offset would result in
%a file position less than zero, a domain_error is raised. Behaviour when seeking to positions beyond the size of the
%underlying object depend on the object and possibly the operating system. The predicate seek/4 is compatible with
%Quintus Prolog, though the error conditions and signalling is ISO compliant.
%See also stream_property/2 and set_stream_position/2.
%***************************************************
%Availability:built-in
%[ISO]

:- mode(read(-Term), one).

%    Read the next Prolog term from the current input stream and unify it with Term.
% On a syntax error read/1 displays an error message, attempts to skip the erroneous term and fails.
%On reaching end-of-file Term is unified with the atom end_of_file.

%Availability:built-in
%[ISO]

:- mode(read_term(-Term, +Options), one).
:- mode(read_term(+Stream, -Term, +Options), one).

%    Describes the detailed layout of the term. The formats for the various types of terms are given below. All positions are character positions. If the input is related to a normal stream, these positions are relative to the start of the input; when reading from the terminal, they are relative to the start of the term.
%
%    From-To
%        Used for primitive types (atoms, numbers, variables).
%    string_position(From, To)
%        Used to indicate the position of a string enclosed in double quotes (").
%    brace_term_position(From, To, Arg)
%        Term of the form {...}, as used in DCG rules. Arg describes the argument.
%    list_position(From, To, Elms, Tail)
%        A list. Elms describes the positions of the elements. If the list specifies the tail as |<TailTerm>, Tail is unified with the term position of the tail, otherwise with the atom none.
%    term_position(From, To, FFrom, FTo, SubPos)
%        Used for a compound term not matching one of the above. FFrom and FTo describe the position of the functor. SubPos is a list, each element of which describes the term position of the corresponding subterm.
%    dict_position(From, To, TagFrom, TagTo, KeyValuePosList)
%        Used for a dict (see section 5.4). The position of the key-value pairs is described by KeyValuePosList, which is a list of key_value_position/7 terms. The key_value_position/7 terms appear in the order of the input. Because maps to not preserve ordering, the key is provided in the position description.
%    key_value_position(From, To, SepFrom, SepTo, Key, KeyPos, ValuePos)
%        Used for key-value pairs in a map (see section 5.4). It is similar to the term_position/5 that would be created, except that the key and value positions do not need an intermediate list and the key is provided in Key to enable synchronisation of the file position data with the data structure.
%    parentheses_term_position(From, To, ContentPos)
%        Used for terms between parentheses. This is an extension compared to the original Quintus specification that was considered necessary for secure refactoring of terms.
%    quasi_quotation_position(From, To, SyntaxFrom, SyntaxTo, ContentPos)
%        Used for quasi quotations.
%

%!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

%Availability:built-in
%[ISO]
:- mode(read(+Stream, -Term), one).

:- mode(read(-Term), one).
%    Read Term from Stream.

read(Term) :-
    current_input(Stream),
    read(Stream, Term).

:- mode(read_term(+Stream, -Term, +Options), one).

%%%
read_term(Term, Options):-
    current_input(Stream),
    read_term(Stream, Term, Options).

%%%
read_term(Stream, Term, []):-
    read(Stream, Term).

read_term(Stream, Term, Options):-
    handle_options(Options, Term).

%%%
handle_options([], Term):- !.
handle_options([Option | Options], Term):-
    handle_option(Option, Term),
    handle_options(Options, Term),!.


%handle_option(Option, Term).
handle_option(backquoted_string(Bool), Term).
handle_option(character_escapes(Bool), Term).
handle_option(comments(Comments), Term).
handle_option(cycles(Bool), Term).
handle_option(dotlists(Bool), Term).
handle_option(double_quotes(Atom), Term).
handle_option(module(Module), Term).
handle_option(quasi_quotations(List), Term).
handle_option(singletons(Vars), Term).
handle_option(subterm_positions(TermPos), Term):-
    handle_suboption(SubOption, subterm_positions(TermPos), Term).

handle_suboption( From-To, subterm_positions(TermPos), Term).
handle_suboption( string_position(From, To), subterm_positions(TermPos), Term).
handle_suboption( brace_term_position(From, To, Arg), subterm_positions(TermPos), Term).
handle_suboption( list_position(From, To, Elms, Tail), subterm_positions(TermPos), Term).
handle_suboption( term_position(From, To, FFrom, FTo, SubPos), subterm_positions(TermPos), Term).
handle_suboption( dict_position(From, To, TagFrom, TagTo, KeyValuePosList), subterm_positions(TermPos), Term).
handle_suboption( key_value_position(From, To, SepFrom, SepTo, Key, KeyPos, ValuePos), subterm_positions(TermPos), Term).
handle_suboption( parentheses_term_position(From, To, ContentPos), subterm_positions(TermPos), Term).
handle_suboption( quasi_quotation_position(From, To, SyntaxFrom, SyntaxTo, ContentPos), subterm_positions(TermPos), Term).



