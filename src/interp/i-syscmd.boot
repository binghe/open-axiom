-- Copyright (c) 1991-2002, The Numerical Algorithms Group Ltd.
-- All rights reserved.
-- Copyright (C) 2007-2011, Gabriel Dos Reis.
-- All rights reserved.
--
-- Redistribution and use in source and binary forms, with or without
-- modification, are permitted provided that the following conditions are
-- met:
--
--     - Redistributions of source code must retain the above copyright
--       notice, this list of conditions and the following disclaimer.
--
--     - Redistributions in binary form must reproduce the above copyright
--       notice, this list of conditions and the following disclaimer in
--       the documentation and/or other materials provided with the
--       distribution.
--
--     - Neither the name of The Numerical Algorithms Group Ltd. nor the
--       names of its contributors may be used to endorse or promote products
--       derived from this software without specific prior written permission.
--
-- THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
-- IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
-- TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
-- PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
-- OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
-- EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
-- PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
-- PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
-- LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
-- NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
-- SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


import i_-object
namespace BOOT

--% Utility Variable Initializations

$cacheAlist := nil
$compileRecurrence := true
$errorReportLevel := 'warning
$sourceFileTypes := '(INPUT SPAD BOOT LISP LISP370 META)

$existingFiles := hashTable "EQUAL"

$SYSCOMMANDS := [first x for x in $systemCommands]

$NonNullStream ==
  '"NonNullStream"

$NullStream ==
  '"NullStream"

$whatOptions := '( _
    operations _
    categories _
    domains _
    packages _
    commands _
    synonyms _
    things _
    )

$clearOptions := '( _
  modes _
  operations _
  properties _
  types _
  values  _
  )

$displayOptions := '( _
  abbreviations _
  all _
  macros _
  modes _
  names _
  operations  _
  properties _
  types _
  values _
  )

$countAssoc := '( (cache countCache) )

$localExposureDataDefault :=
  VECTOR(["basic", "categories"], [], [])

$localExposureData := 
  COPY_-SEQ $localExposureDataDefault


--% Top level system command

$options := nil

initializeSystemCommands() ==
  l := $systemCommands
  $SYSCOMMANDS := NIL
  while l repeat
    $SYSCOMMANDS := [CAAR l,:$SYSCOMMANDS]
    l := rest l
  $SYSCOMMANDS := nreverse $SYSCOMMANDS

systemCommand [[op,:argl],:options] ==
  $options: local:= options
  $e:local := $CategoryFrame
  fun := selectOptionLC(op,$SYSCOMMANDS,'commandError)
  argl and (argl.0 = '_?) and fun ~= 'synonym =>
    helpSpad2Cmd [fun]
  fun := selectOption(fun,commandsForUserLevel $systemCommands,
    'commandUserLevelError)
  FUNCALL(fun, argl)

commandsForUserLevel l == --[a for [a,:b] in l | satisfiesUserLevel(a)]
  c := nil
  for [a,:b] in l repeat
    satisfiesUserLevel b => c := [a,:c]
  reverse c

synonymsForUserLevel l ==
  -- l is a list of synonyms, and this returns a sublist of applicable
  -- synonyms at the current user level.
  $UserLevel = 'development => l
  nl := NIL
  for syn in reverse l repeat
    cmd := STRING2ID_-N(rest syn,1)
    null selectOptionLC(cmd,commandsForUserLevel
      $systemCommands,NIL) => nil
    nl := [syn,:nl]
  nl

satisfiesUserLevel x ==
  x          = 'interpreter => true
  $UserLevel = 'interpreter => false
  x          = 'compiler    => true
  $UserLevel = 'compiler    => false
  true

unAbbreviateKeyword x ==
  x' :=selectOptionLC(x,$SYSCOMMANDS,'commandErrorIfAmbiguous)
  if not x' then
    x' := 'system
    SETQ(LINE, strconc('")system ", subString(LINE, 1, #LINE-1)))
    $currentLine := LINE
  selectOption(x',commandsForUserLevel $systemCommands,
    'commandUserLevelError)

hasOption(al,opt) ==
  optPname:= PNAME opt
  found := NIL
  for pair in al while not found repeat
    stringPrefix?(PNAME first pair,optPname) => found := pair
  found

selectOptionLC(x,l,errorFunction) ==
  selectOption(DOWNCASE object2Identifier x,l,errorFunction)

selectOption(x,l,errorFunction) ==
  member(x,l) => x                   --exact spellings are always OK
  not IDENTP x =>
    errorFunction => FUNCALL(errorFunction,x,u)
    nil
  u := [y for y in l | stringPrefix?(PNAME x,PNAME y)]
  u is [y] => y
  errorFunction => FUNCALL(errorFunction,x,u)
  nil

terminateSystemCommand() == TERSYSCOMMAND()

commandUserLevelError(x,u) == userLevelErrorMessage("command",x,u)

optionUserLevelError(x,u) == userLevelErrorMessage("option",x,u)

userLevelErrorMessage(kind,x,u) ==
  null u =>
    sayKeyedMsg("S2IZ0007",[$UserLevel,kind])
    terminateSystemCommand()
  commandAmbiguityError(kind,x,u)

commandError(x,u) == commandErrorMessage("command",x,u)

optionError(x,u) == commandErrorMessage("option",x,u)

commandErrorIfAmbiguous(x, u) ==
  null u => nil
  SETQ($OLDLINE, LINE)
  commandAmbiguityError("command", x, u)

commandErrorMessage(kind,x,u) ==
  SETQ ($OLDLINE,LINE)
  null u =>
    sayKeyedMsg("S2IZ0008",[kind,x])
    terminateSystemCommand()
  commandAmbiguityError(kind,x,u)

commandAmbiguityError(kind,x,u) ==
  sayKeyedMsg("S2IZ0009",[kind,x])
  for a in u repeat sayMSG ['"     ",:bright a]
  terminateSystemCommand()

--% Utility for access to original command line

getSystemCommandLine() ==
  p := STRPOS('")",$currentLine,0,NIL)
  line := if p then subString($currentLine,p) else $currentLine
  maxIndex:= MAXINDEX line
  for i in 0..maxIndex while (line.i ~= " ") repeat index:= i
  if index=maxIndex then line := '""
  else line := subString(line,index+2)
  line

------------ start of commands ------------------------------------------

--% )abbreviations

abbreviations l == abbreviationsSpad2Cmd l

abbreviationsSpad2Cmd l ==
  null l => helpSpad2Cmd '(abbreviations)
  abopts := '(query domain category package remove)

  quiet := nil
  for [opt] in $options repeat
    opt := selectOptionLC(opt,'(quiet),'optionError)
    opt = 'quiet => quiet := true

  l is [opt,:al] =>
    key := opOf first al
    type := selectOptionLC(opt,abopts,'optionError)
    type is 'query =>
      null al => listConstructorAbbreviations()
      constructor := abbreviation?(key) => abbQuery(constructor)
      abbQuery(key)
    type is 'remove =>
      DELDATABASE(key,'ABBREVIATION)
    ODDP # al => sayKeyedMsg("S2IZ0002",[type])
    repeat
      null al => return 'fromLoop
      [a,b,:al] := al
      mkUserConstructorAbbreviation(b,a,type)
      SETDATABASE(b,'ABBREVIATION,a)
      SETDATABASE(b,'CONSTRUCTORKIND,type)
    null quiet =>
      sayKeyedMsg("S2IZ0001",[a,type,opOf b])
      nil
  nil

listConstructorAbbreviations() ==
  x := UPCASE queryUserKeyedMsg("S2IZ0056",NIL)
  STRING2ID_-N(x,1) in '(Y YES) =>
    whatSpad2Cmd '(categories)
    whatSpad2Cmd '(domains)
    whatSpad2Cmd '(packages)
  sayKeyedMsg("S2IZ0057",NIL)

--% )cd

cd args ==
  dir := TRUENAME STRING(first args or '"")
  changeDirectory NAMESTRING dir
  SETF(_*DEFAULT_-PATHNAME_-DEFAULTS_*, 
    PATHNAME ensureTrailingSlash NAMESTRING dir)
  sayKeyedMsg("S2IZ0070", [NAMESTRING _*DEFAULT_-PATHNAME_-DEFAULTS_*]) 


--% )clear

clear l == clearSpad2Cmd l

clearSpad2Cmd l ==
  -- new version which changes the environment and updates history
  $clearExcept: local := nil
  if $options then $clearExcept :=
    "and"/[selectOptionLC(opt,'(except),'optionError) =
             'except for [opt,:.] in $options]
  null l =>
    optList:= "append"/[['"%l",'"       ",x] for x in $clearOptions]
    sayKeyedMsg("S2IZ0010",[optList])
  arg := selectOptionLC(first l,'(all completely scaches),NIL)
  arg = 'all          => clearCmdAll()
  arg = 'completely   => clearCmdCompletely()
  arg = 'scaches      => clearCmdSortedCaches()
  $clearExcept => clearCmdExcept(l)
  clearCmdParts(l)
  updateCurrentInterpreterFrame()

clearCmdSortedCaches() ==
  $lookupDefaults: local := false
  for [.,.,:domain] in HGET($ConstructorCache,'SortedCache) repeat
    pair := compiledLookupCheck('clearCache,[$Void],domain)
    SPADCALL pair

clearCmdCompletely() ==
  clearCmdAll()
  $localExposureData := COPY_-SEQ $localExposureDataDefault
  -- $functionTable := NIL
  sayKeyedMsg("S2IZ0013",NIL)
  clearClams()
  clearConstructorCaches()
  $existingFiles := hashTable 'EQUAL
  sayKeyedMsg("S2IZ0014",NIL)
  RECLAIM()
  sayKeyedMsg("S2IZ0015",NIL)
  NIL

clearCmdAll() ==
  clearCmdSortedCaches()
  ------undo special variables------
  $frameRecord := nil
  $previousBindings := nil
  $variableNumberAlist := nil
  untraceMapSubNames _/TRACENAMES
  $InteractiveFrame := [[NIL]]
  resetInCoreHist()
  if $useInternalHistoryTable
    then $internalHistoryTable := NIL
    else removeFile histFileName()
  $IOindex := 1
  updateCurrentInterpreterFrame()
  $currentLine := '")clear all"    --restored 3/94; needed for undo (RDJ)
  clearMacroTable()
  if $frameMessages then sayKeyedMsg("S2IZ0011",[$interpreterFrameName])
  else sayKeyedMsg("S2IZ0012",NIL)

clearCmdExcept(l is [opt,:vl]) ==
  --clears elements of vl of all options EXCEPT opt
  for option in $clearOptions |
    not stringPrefix?(object2String opt,object2String option)
      repeat clearCmdParts [option,:vl]

clearCmdParts(l is [opt,:vl]) ==
  -- clears the bindings indicated by opt of all variables in vl

  option:= selectOptionLC(opt,$clearOptions,'optionError)
  option:= makeSymbol PNAME option

  -- the option can be plural but the key in the alist is sometimes
  -- singular

  option :=
    option = 'types =>  'mode
    option = 'modes =>  'mode
    option = 'values => 'value
    option

  null vl => sayKeyedMsg("S2IZ0055",NIL)
  pmacs := getParserMacroNames()
  imacs := getInterpMacroNames()
  if vl='(all) then
    vl := ASSOCLEFT CAAR $InteractiveFrame
    vl := removeDuplicates(append(vl, pmacs))
  $e : local := $InteractiveFrame
  for x in vl repeat
    clearDependencies(x,true)
    if option='properties and x in pmacs then clearParserMacro(x)
    if option='properties and x in imacs and not (x in pmacs) then
        sayMessage ['"   You cannot clear the definition of the system-defined macro ",
            fixObjectForPrinting x,"."]
    p1 := assoc(x,CAAR $InteractiveFrame) =>
      option='properties =>
        if isMap x then
          (lm := get(x,'localModemap,$InteractiveFrame)) =>
            cons? lm => untraceMapSubNames [CADAR lm]
          NIL
        for p2 in rest p1 repeat
          prop:= first p2
          recordOldValue(x,prop,rest p2)
          recordNewValue(x,prop,NIL)
        CAAR($InteractiveFrame) := deleteAssoc(x,CAAR $InteractiveFrame)
      p2:= assoc(option,rest p1) =>
        recordOldValue(x,option,rest p2)
        recordNewValue(x,option,NIL)
        p2.rest := NIL
  nil

--% )close

queryClients () ==
  -- Returns the number of active scratchpad clients
  sockSendInt($SessionManager, $QueryClients)
  sockGetInt $SessionManager


close args ==
  quiet:local:= false
  null $SpadServer =>
    throwKeyedMsg('"S2IZ0071", [])
  numClients := queryClients()
  numClients > 1 =>
    sockSendInt($SessionManager, $CloseClient)
    sockSendInt($SessionManager, $currentFrameNum)
    closeInterpreterFrame(NIL)
  for [opt,:.] in $options repeat
    fullopt := selectOptionLC(opt, '(quiet), 'optionError)
    fullopt = 'quiet   =>
           quiet:=true
  quiet =>
    sockSendInt($SessionManager, $CloseClient)
    sockSendInt($SessionManager, $currentFrameNum)
    closeInterpreterFrame(NIL)
  x := UPCASE queryUserKeyedMsg('"S2IZ0072", nil)
  STRING2ID_-N(x,1) in '(YES Y) =>
    coreQuit()  -- ??? should be coreQuit errorCount()
  nil

--% )constructor

constructor args ==
  sayMessage '"   Not implemented yet."
  NIL

--% )compiler

compiler args ==
    $newConlist: local := nil    --reset by compDefineLisplib and astran
    null args and null $options and null _/EDITFILE => helpSpad2Cmd '(compiler)
    if null args then args := [_/EDITFILE]

    -- first see if the user has explicitly specified the compiler
    -- to use.

    optlist := '(new old translate constructor)
    haveNew := nil
    haveOld := nil
    for opt in $options while not (haveNew and haveOld) repeat
        [optname,:optargs] := opt
        fullopt := selectOptionLC(optname,optlist,nil)
        fullopt = 'new => haveNew := true
        fullopt = 'translate => haveOld := true
        fullopt = 'constructor => haveOld := true
        fullopt = 'old => haveOld := true

    haveNew and haveOld => throwKeyedMsg("S2IZ0081", nil)

    af  := pathname args
    aft := pathnameType af
-- Whats this for? MCD/PAB 21-9-95
--    if haveNew and (null(aft) or (aft = '"")) then
--        af := pathname [af, '"as"]
--        aft = '"as"
--    if haveOld and (null(aft) or (aft = '"")) then
--        af := pathname [af, '"spad"]
--        aft = '"spad"

    haveNew or (aft = '"as")   =>
        not (af1 := $FINDFILE (af, '(as))) =>
            throwKeyedMsg("S2IL0003",[NAMESTRING af])
        compileAsharpCmd [af1]
    haveOld or (aft = '"spad") =>
        not (af1 := $FINDFILE (af, '(spad))) =>
            throwKeyedMsg("S2IL0003",[NAMESTRING af])
        compileSpad2Cmd  [af1]
    aft = '"lsp"   =>
        not (af1 := $FINDFILE (af, '(lsp))) =>
            throwKeyedMsg("S2IL0003",[NAMESTRING af])
        compileAsharpLispCmd [af1]
    aft = '"NRLIB"  =>
        not (af1 := $FINDFILE (af, '(NRLIB))) =>
            throwKeyedMsg("S2IL0003",[NAMESTRING af])
        compileSpadLispCmd [af1]
    aft = '"ao"   =>
        not (af1 := $FINDFILE (af, '(ao))) =>
            throwKeyedMsg("S2IL0003",[NAMESTRING af])
        compileAsharpCmd [af1]
    aft = '"al"   =>    -- archive library of .ao files
        not (af1 := $FINDFILE (af, '(al))) =>
            throwKeyedMsg("S2IL0003",[NAMESTRING af])
        compileAsharpArchiveCmd [af1]

    -- see if we something with the appropriate file extension
    -- lying around

    af1 := $FINDFILE (af, '(as spad ao asy))

    af1 and pathnameType(af1) = '"as"   => compileAsharpCmd [af1]
    af1 and pathnameType(af1) = '"ao"  => compileAsharpCmd [af1]
    af1 and pathnameType(af1) = '"spad" => compileSpad2Cmd  [af1]
    af1 and pathnameType(af1) = '"asy"  => compileAsharpArchiveCmd [af1]

    -- maybe /EDITFILE has some stuff that can help us
    ef := pathname _/EDITFILE
    ef := mergePathnames(af,ef)

    ef = af => throwKeyedMsg("S2IZ0039", nil)
    af := ef

    pathnameType(af) = '"as"   => compileAsharpCmd args
    pathnameType(af) = '"ao"  => compileAsharpCmd args
    pathnameType(af) = '"spad" => compileSpad2Cmd  args

    -- see if we something with the appropriate file extension
    -- lying around
    af1 := $FINDFILE (af, '(as spad ao asy))

    af1 and pathnameType(af1) = '"as"   => compileAsharpCmd [af1]
    af1 and pathnameType(af1) = '"ao"  => compileAsharpCmd [af1]
    af1 and pathnameType(af1) = '"spad" => compileSpad2Cmd  [af1]
    af1 and pathnameType(af1) = '"asy"  => compileAsharpArchiveCmd [af1]

    throwKeyedMsg("S2IZ0039", nil)

compileAsharpCmd args ==
    compileAsharpCmd1 args
    terminateSystemCommand()

compileAsharpCmd1 args ==
    -- Assume we entered from the "compiler" function, so args ~= nil
    -- and is a file with file extension .as or .ao

    path := pathname args
    pathType := pathnameType path
    (pathType ~= '"as") and (pathType ~= '"ao") => throwKeyedMsg("S2IZ0083", nil)
    null PROBE_-FILE path => throwKeyedMsg("S2IL0003",[namestring args])

    SETQ(_/EDITFILE, path)
    updateSourceFiles path

    optList :=  '( _
      new _
      old _
      translate _
      onlyargs _
      moreargs _
      quiet _
      nolispcompile _
      noquiet _
      library _
      nolibrary _
        )

    beQuiet := false         -- be verbose here
    doLibrary  := true       -- so a )library after compilation
    doCompileLisp := true    -- do compile generated lisp code

    moreArgs := NIL
    onlyArgs := NIL

    for opt in $options repeat
        [optname,:optargs] := opt
        fullopt := selectOptionLC(optname,optList,nil)

        fullopt = 'new       => nil
        fullopt = 'old       => error "Internal error: compileAsharpCmd got )old"
        fullopt = 'translate => error "Internal error: compileAsharpCmd got )translate"

        fullopt = 'quiet     => beQuiet := true
        fullopt = 'noquiet   => beQuiet := false

        fullopt = 'nolispcompile   => doCompileLisp := false

        fullopt = 'moreargs  => moreArgs := optargs
        fullopt = 'onlyargs  => onlyArgs := optargs

        fullopt = 'library   => doLibrary  := true
        fullopt = 'nolibrary => doLibrary  := false

        throwKeyedMsg("S2IZ0036",[strconc('")",object2String optname)])

    tempArgs :=
        pathType = '"ao" =>
            -- want to strip out -Fao
            (p := STRPOS('"-Fao", $asharpCmdlineFlags, 0, NIL)) =>
                p = 0 => subString($asharpCmdlineFlags, 5)
                strconc(subString($asharpCmdlineFlags, 0, p), '" ",
                    subString($asharpCmdlineFlags, p+5))
            $asharpCmdlineFlags
        $asharpCmdlineFlags

    asharpArgs :=
        onlyArgs =>
            s := ""
            for a in onlyArgs repeat
                s := strconc(s, '" ", object2String a)
            s
        moreArgs =>
            s := tempArgs
            for a in moreArgs repeat
                s := strconc(s, '" ", object2String a)
            s
        tempArgs

    if not beQuiet then sayKeyedMsg("S2IZ0038A",[namestring args, asharpArgs])

    command :=
     strconc(strconc(getEnv('"ALDORROOT"),'"/bin/"),_
               "aldor ", asharpArgs, '" ", namestring args)
    rc := runCommand command

    if (rc = 0) and doCompileLisp then
        lsp := fnameMake('".", pathnameName args, '"lsp")
        if fnameReadable?(lsp) then
            if not beQuiet then sayKeyedMsg("S2IZ0089", [namestring lsp])
            compileFileQuietly(lsp)
        else
            sayKeyedMsg("S2IL0003", [namestring lsp])

    if rc = 0 and doLibrary then
        -- do we need to worry about where the compilation output went?
        if not beQuiet then sayKeyedMsg("S2IZ0090", [ pathnameName path ])
        withAsharpCmd [ pathnameName path ]
    else if not beQuiet then
        sayKeyedMsg("S2IZ0084", nil)

    if not $buildingSystemAlgebra then
      extendLocalLibdb $newConlist

compileAsharpArchiveCmd args ==
    -- Assume we entered from the "compiler" function, so args ~= nil
    -- and is a file with file extension .al. We also assume that
    -- the name is fully qualified.

    path := pathname args
    null PROBE_-FILE path => throwKeyedMsg("S2IL0003",[namestring args])

    -- here is the plan:
    --   1. extract the file name and try to make a directory based
    --      on that name.
    --   2. cd to that directory and ar x the .al file
    --   3. for each .ao file that shows up, compile it
    --   4. delete the generated .ao files

    -- First try to make the directory in the current directory

    dir  := fnameMake('".", pathnameName path, '"axldir")
    exists := PROBE_-FILE dir
    isDir := directoryp namestring dir
    exists and isDir ~= 1=>
        throwKeyedMsg("S2IL0027",[namestring dir, namestring args])

    if isDir ~= 1 then
        rc   := mkdir namestring dir
        rc ~= 0 => throwKeyedMsg("S2IL0027",[namestring dir, namestring args])

    curDir := GET_-CURRENT_-DIRECTORY()

    -- cd to that directory and try to unarchive the .al file

    cd [ object2Identifier namestring dir ]

    cmd := strconc( '"ar x ", namestring path )
    rc := runCommand cmd
    rc ~= 0 =>
        cd [ object2Identifier namestring curDir ]
        throwKeyedMsg("S2IL0028",[namestring dir, namestring args])

    -- Look for .ao files

    asos := DIRECTORY '"*.ao"
    null asos =>
        cd [ object2Identifier namestring curDir ]
        throwKeyedMsg("S2IL0029",[namestring dir, namestring args])

    -- Compile the .ao files

    for aso in asos repeat
        compileAsharpCmd1 [ namestring aso ]

    -- Reset the current directory

    cd [ object2Identifier namestring curDir ]

    terminateSystemCommand()

compileAsharpLispCmd args ==
    -- Assume we entered from the "compiler" function, so args ~= nil
    -- and is a file with file extension .lsp

    path := pathname args
    null PROBE_-FILE path => throwKeyedMsg("S2IL0003",[namestring args])

    optList :=  '( _
      quiet _
      noquiet _
      library _
      nolibrary _
        )

    beQuiet := false         -- be verbose here
    doLibrary  := true       -- so a )library after compilation

    for opt in $options repeat
        [optname,:optargs] := opt
        fullopt := selectOptionLC(optname,optList,nil)

        fullopt = 'quiet     => beQuiet := true
        fullopt = 'noquiet   => beQuiet := false

        fullopt = 'library   => doLibrary  := true
        fullopt = 'nolibrary => doLibrary  := false

        throwKeyedMsg("S2IZ0036",[strconc('")",object2String optname)])

    lsp := fnameMake(pathnameDirectory path, pathnameName path, pathnameType path)
    if fnameReadable?(lsp) then
        if not beQuiet then sayKeyedMsg("S2IZ0089", [namestring lsp])
        compileFileQuietly(lsp)
    else
        sayKeyedMsg("S2IL0003", [namestring lsp])

    if doLibrary then
        -- do we need to worry about where the compilation output went?
        if not beQuiet then sayKeyedMsg("S2IZ0090", [ pathnameName path ])
        withAsharpCmd [ pathnameName path ]
    else if not beQuiet then
        sayKeyedMsg("S2IZ0084", nil)
    terminateSystemCommand()

compileSpadLispCmd args ==
    -- Assume we entered from the "compiler" function, so args ~= nil
    -- and is a file with file extension .NRLIB

    path := pathname fnameMake(first args, '"code", '"lsp")
    null PROBE_-FILE path => throwKeyedMsg("S2IL0003",[namestring args])

    optList :=  '( _
      quiet _
      noquiet _
      library _
      nolibrary _
        )

    beQuiet := false         -- be verbose here
    doLibrary  := true       -- so a )library after compilation

    for opt in $options repeat
        [optname,:optargs] := opt
        fullopt := selectOptionLC(optname,optList,nil)

        fullopt = 'quiet     => beQuiet := true
        fullopt = 'noquiet   => beQuiet := false

        fullopt = 'library   => doLibrary  := true
        fullopt = 'nolibrary => doLibrary  := false

        throwKeyedMsg("S2IZ0036",[strconc('")",object2String optname)])

    lsp := fnameMake(pathnameDirectory path, pathnameName path, pathnameType path)
    if fnameReadable?(lsp) then
        if not beQuiet then sayKeyedMsg("S2IZ0089", [namestring lsp])
        --compileFileQuietly(lsp)
        RECOMPILE_-LIB_-FILE_-IF_-NECESSARY lsp
    else
        sayKeyedMsg("S2IL0003", [namestring lsp])

    if doLibrary then
        -- do we need to worry about where the compilation output went?
        if not beQuiet then sayKeyedMsg("S2IZ0090", [ pathnameName path ])
        LOCALDATABASE([ pathnameName first args ],[])
    else if not beQuiet then
        sayKeyedMsg("S2IZ0084", nil)
    terminateSystemCommand()

compileSpad2Cmd args ==
    -- This is the old compiler
    -- Assume we entered from the "compiler" function, so args ~= nil
    -- and is a file with file extension .spad.

    path := pathname args
    pathnameType path ~= '"spad" => throwKeyedMsg("S2IZ0082", nil)
    null PROBE_-FILE path => throwKeyedMsg("S2IL0003",[namestring args])

    SETQ(_/EDITFILE, path)
    updateSourceFiles path
    sayKeyedMsg("S2IZ0038",[namestring args])

    optList :=  '( _
      break _
      constructor _
      functions _
      library _
      lisp _
      new _
      old _
      nobreak _
      nolibrary _
      noquiet _
      vartrace _
      quiet _
      translate _
      optimize _
      report
        )

    translateOldToNew        := nil

    $scanIfTrue              : local := false
    $compileOnlyCertainItems : local := nil
    $f                       : local := nil  -- compiler
    $m                       : local := nil  --   variables

    -- following are for )quick option for code generation
    $QuickLet   : local := true

    fun         := ['rq, 'lib]
    constructor := nil
    $sourceFileTypes : local := '("SPAD")

    for opt in $options repeat
        [optname,:optargs] := opt
        fullopt := selectOptionLC(optname,optList,nil)

        fullopt = 'new         => error "Internal error: compileSpad2Cmd got )new"
        fullopt = 'old         => NIL     -- no opt
        fullopt = 'translate   => translateOldToNew := true

        fullopt = 'library     => fun.1 := 'lib
        fullopt = 'nolibrary   => fun.1 := 'nolib

        -- Ignore quiet/nonquiet if "constructor" is given.
        fullopt = 'quiet       => if fun.0 ~= 'c then fun.0 := 'rq
        fullopt = 'noquiet     => if fun.0 ~= 'c then fun.0 := 'rf
        fullopt = 'nobreak     => $scanIfTrue := true
        fullopt = 'break       => $scanIfTrue := nil
        fullopt = 'vartrace      =>
          $QuickLet  := false
        fullopt = 'lisp        =>
          throwKeyedMsg("S2IZ0036",['")lisp"])
        fullopt = 'functions   =>
            null optargs =>
              throwKeyedMsg("S2IZ0037",['")functions"])
            $compileOnlyCertainItems := optargs
        fullopt = 'constructor =>
            null optargs =>
              throwKeyedMsg("S2IZ0037",['")constructor"])
            fun.0       := 'c
            constructor := [unabbrev o for o in optargs]
        fullopt = "optimize" => setCompilerOptimizations first optargs
        fullopt = "report" =>
           null optargs => throwKeyedMsg("S2IZ0037",['")report"])
           if "insn" in optargs then
             $reportOptimization := true
        throwKeyedMsg("S2IZ0036",[strconc('")",object2String optname)])

    $InteractiveMode : local := nil
    -- avoid Boolean semantics transformations based on syntax only
    $normalizeTree: local := false
    if translateOldToNew then
        spad2AsTranslatorAutoloadOnceTrigger()
        sayKeyedMsg("S2IZ0085", nil)
        convertSpadToAsFile path
    else if $compileOnlyCertainItems then
        null constructor => sayKeyedMsg("S2IZ0040",NIL)
        compilerDoitWithScreenedLisplib(constructor, fun)
    else
        compilerDoit(constructor, fun)
    if not $buildingSystemAlgebra then 
      extendLocalLibdb $newConlist
    terminateSystemCommand()
    -- reset compiler optimization options
    setCompilerOptimizations 0

convertSpadToAsFile path ==
    -- can assume path has type = .spad
    $globalMacroStack : local := nil       -- for spad -> as translator
    $abbreviationStack: local := nil       -- for spad -> as translator
    $macrosAlreadyPrinted: local := nil    -- for spad -> as translator
    $abbreviationsAlreadyPrinted: local := nil    -- for spad -> as translator
    $convertingSpadFile : local := true
    $options: local := '((nolib))      -- translator shouldn't create nrlibs
    SETQ(HT,hashTable 'EQUAL)

    newName := fnameMake(pathnameDirectory path, pathnameName path, '"as")
    canDoIt := true
    if not fnameWritable? newName then
        sayKeyedMsg("S2IZ0086", [NAMESTRING newName])
        newName := fnameMake('".", pathnameName path, '"as")
        if not fnameWritable? newName then
            sayKeyedMsg("S2IZ0087", [NAMESTRING newName])
            canDoIt := false
    not canDoIt => 'failure

    sayKeyedMsg("S2IZ0088", [NAMESTRING newName])

    $outStream :local := MAKE_-OUTSTREAM newName
    markSay('"#include _"axiom.as_"")
    markTerpri()
    CATCH($SpadReaderTag,compiler [path])
    SHUT $outStream
    mkCheck()
    'done

compilerDoit(constructor, fun) ==
    $byConstructors : local := []
    $constructorsSeen : local := []
    fun = ['rf, 'lib]   => _/RQ_,LIB()    -- Ignore "noquiet".
    fun = ['rf, 'nolib] => _/RF()
    fun = ['rq, 'lib]   => _/RQ_,LIB()
    fun = ['rq, 'nolib] => _/RQ()
    fun = ['c,  'lib]   =>
      $byConstructors := [opOf x for x in constructor]
      _/RQ_,LIB()
      for ii in $byConstructors repeat
        null member(ii,$constructorsSeen) =>
          sayBrightly ['">>> Warning ",'"%b",ii,'"%d",'" was not found"]

compilerDoitWithScreenedLisplib(constructor, fun) ==
    EMBED('RWRITE,
          '(LAMBDA (KEY VALUE STREAM)
                   (COND ((AND (EQ STREAM $libFile)
                               (NOT (MEMBER KEY $saveableItems)))
                          VALUE)
                         ((NOT NIL)
                          (RWRITE KEY VALUE STREAM)))) )
    (try compilerDoit(constructor,fun); finally SEQ(UNEMBED 'RWRITE))


withAsharpCmd args ==
    $options: local := nil
    LOCALDATABASE(args, $options)

--% )copyright -- display copyright notice

summary l ==
 runCommand strconc('"cat _"", systemRootDirectory(),'"/lib/summary_"")

copyright () ==
 runCommand strconc('"cat _"", systemRootDirectory(),'"/lib/copyright_"")

--% )credits -- display credit list

CREDITS := '(
  "An alphabetical listing of contributors to AXIOM (to October, 2006):"
  "Cyril Alberga          Roy Adler              Christian Aistleitner"
  "Richard Anderson       George Andrews"
  "Henry Baker            Stephen Balzac         Yurij Baransky"
  "David R. Barton        Gerald Baumgartner     Gilbert Baumslag"
  "Michael Becker         Fred Blair             Vladimir Bondarenko"
  "Mark Botch             Alexandre Bouyer       Peter A. Broadbery"
  "Martin Brock           Manuel Bronstein       Florian Bundschuh"
  "Luanne Burns           William Burge"
  "Quentin Carpent        Robert Caviness        Bruce Char"
  "Cheekai Chin           David V. Chudnovsky    Gregory V. Chudnovsky"
  "Josh Cohen             Christophe Conil       Don Coppersmith"
  "George Corliss         Robert Corless         Gary Cornell"
  "Meino Cramer           Claire Di Crescenzo"
  "Timothy Daly Sr.       Timothy Daly Jr.       James H. Davenport"
  "Jean Della Dora        Gabriel Dos Reis       Michael Dewar"
  "Claire DiCrescendo     Sam Dooley             Lionel Ducos"
  "Martin Dunstan         Brian Dupee            Dominique Duval"
  "Robert Edwards         Heow Eide-Goodman      Lars Erickson"
  "Richard Fateman        Bertfried Fauser       Stuart Feldman"
  "Brian Ford             Albrecht Fortenbacher  George Frances"
  "Constantine Frangos    Timothy Freeman        Korrinn Fu"
  "Marc Gaetano           Rudiger Gebauer        Kathy Gerber"
  "Patricia Gianni        Holger Gollan          Teresa Gomez-Diaz"
  "Laureano Gonzalez-Vega Stephen Gortler        Johannes Grabmeier"
  "Matt Grayson           James Griesmer         Vladimir Grinberg"
  "Oswald Gschnitzer      Jocelyn Guidry"
  "Steve Hague            Vilya Harvey           Satoshi Hamaguchi"
  "Martin Hassner         Waldek Hebisch         Ralf Hemmecke"
  "Henderson              Antoine Hersen"
  "Pietro Iglio"
  "Richard Jenks"
  "Kai Kaminski           Grant Keady            Tony Kennedy"
  "Paul Kosinski          Klaus Kusche           Bernhard Kutzler"
  "Larry Lambe            Frederic Lehobey       Michel Levaud"
  "Howard Levy            Rudiger Loos           Michael Lucks"
  "Richard Luczak"
  "Camm Maguire           Stefan Mai             Bob McElrath"
  "Michael McGettrick     Ian Meikle             David Mentre"
  "Victor S. Miller       Gerard Milmeister      Mohammed Mobarak"
  "H. Michael Moeller     Michael Monagan        Stephen Montgomery-Smith"
  "Marc Moreno-Maza       Scott Morrison         Mark Murray"
  "William Naylor         C. Andrew Neff         John Nelder"
  "Godfrey Nolan          Arthur Norman          Jinzhong Niu"
  "Michael O'Connor       Kostas Oikonomou"
  "Julian A. Padget       Bill Page              Susan Pelzel"
  "Michel Petitot         Didier Pinchon         Jose Alfredo Portes"
  "Claude Quitte"
  "Norman Ramsey          Anatoly Raportirenko   Michael Richardson"
  "Renaud Rioboo          Jean Rivlin            Nicolas Robidoux"
  "Simon Robinson         Michael Rothstein      Martin Rubey"
  "Aleksej Saushev        Philip Santas          Alfred Scheerhorn"
  "William Schelter       Gerhard Schneider      Martin Schoenert"
  "Marshall Schor         Frithjof Schulze       Fritz Schwarz"
  "Nick Simicich          William Sit            Elena Smirnova"
  "Jonathan Steinbach     Christine Sundaresan   Robert Sutor"
  "Moss E. Sweedler       Eugene Surowitz"
  "James Thatcher         Balbir Thomas          Mike Thomas"
  "Dylan Thurston         Barry Trager           Themos T. Tsikas"
  "Gregory Vanuxem"
  "Bernhard Wall          Stephen Watt           Jaap Weel"
  "Juergen Weiss          M. Weller              Mark Wegman"
  "James Wen              Thorsten Werther       Michael Wester"
  "John M. Wiley          Berhard Will           Clifton J. Williamson"
  "Stephen Wilson         Shmuel Winograd        Robert Wisbauer"
  "Sandra Wityak          Waldemar Wiwianka      Knut Wolf"
  "Clifford Yapp          David Yun"
  "Richard Zippel         Evelyn Zoernack        Bruno Zuercher"
  "Dan Zwillinger"
 )

credits() ==
 for i in CREDITS repeat
  PRINC(i)
  TERPRI()

--% )display

display l == displaySpad2Cmd l

displaySpad2Cmd l ==
  $e: local := $EmptyEnvironment
  l is [opt,:vl] and opt ~= "?" =>
    option := selectOptionLC(opt,$displayOptions,'optionError) =>

      -- the option may be given in the plural but the property in
      -- the alist is sometimes singular

      option :=
        option = 'all   =>
            l := ['properties]
            'properties
        (option = 'modes) or (option = 'types) =>
            l := ['type, :vl]
            'type
        option = 'values =>
            l := ['value, :vl]
            'value
        option

      option = 'abbreviations =>
        null vl => listConstructorAbbreviations()
        for v in vl repeat
          abbQuery
            -- unbbeviate if necessary
            v := opOf v
            ctor:= abbreviation? v => ctor
            v

      option = 'operations =>     displayOperations vl
      option = "macros" =>         displayMacros vl
      option = 'names =>          displayWorkspaceNames()
      displayProperties(option,l)
  optList:= [:['"%l",'"        ",x] for x in $displayOptions]
  msg := [:bright '"  )display",'"keyword arguments are",
    :bright optList,'"%l",'"   or abbreviations thereof."]
  sayMessage msg

displayMacros names ==
  imacs := getInterpMacroNames()
  pmacs := getParserMacroNames()
  macros :=
     null names => append (imacs, pmacs)
     names
  macros := removeDuplicates macros

  null macros => sayBrightly '"   There are no OpenAxiom macros."

  -- first do user defined ones

  first := true
  for macro in macros repeat
    macro in pmacs =>
        if first then
            sayBrightly ['"%l",'"User-defined macros:"]
            first := NIL
        displayParserMacro macro
    macro in imacs => 'iterate
    sayBrightly (["   ",'"%b", macro, '"%d", " is not a known OpenAxiom macro."])

  -- now system ones

  first := true
  for macro in macros repeat
    macro in imacs =>
        macro in pmacs => 'iterate
        if first then
            sayBrightly ['"%l",'"System-defined macros:"]
            first := NIL
        displayMacro macro
    macro in pmacs => 'iterate
  NIL

getParserMacroNames() ==
  removeDuplicates [first mac for mac in getParserMacros()]

clearParserMacro(macro) ==
  -- first see if it is one
  not IFCDR assoc(macro, $pfMacros) => NIL
  $pfMacros := REMALIST($pfMacros, macro)

displayMacro name ==
  m := isInterpMacro name
  null m =>
    sayBrightly ['"  ",:bright name,'"is not an interpreter macro."]
  -- $op is needed in the output routines.
  $op : local := strconc('"macro ",object2String name)
  [args,:body] := m
  args :=
    null args => nil
    null rest args => first args
    ["tuple",:args]
  mathprint ["%Map",[args,:body]]

displayWorkspaceNames() ==
  imacs := getInterpMacroNames()
  pmacs := getParserMacroNames()
  sayMessage '"Names of User-Defined Objects in the Workspace:"
  names := MSORT append(getWorkspaceNames(),pmacs)
  if null names
    then sayBrightly "   * None *"
    else sayAsManyPerLineAsPossible [object2String x for x in names]
  imacs := SETDIFFERENCE(imacs,pmacs)
  if imacs then
    sayMessage '"Names of System-Defined Objects in the Workspace:"
    sayAsManyPerLineAsPossible [object2String x for x in imacs]


getWorkspaceNames() ==
  NMSORT [n for [n,:.] in CAAR $InteractiveFrame |
    (n ~= "--macros--" and n ~= "--flags--")]

displayOperations l ==
  null l =>
    x := UPCASE queryUserKeyedMsg("S2IZ0058",NIL)
    if STRING2ID_-N(x,1) in '(Y YES)
      then for op in allOperations() repeat reportOpSymbol op
      else sayKeyedMsg("S2IZ0059",NIL)
    nil
  for op in l repeat reportOpSymbol op

interpFunctionDepAlists() ==
  $e : local := $InteractiveFrame
  deps := getFlag "$dependencies"
  $dependentAlist := [[NIL,:NIL]]
  $dependeeAlist := [[NIL,:NIL]]
  for [dependee,dependent] in deps repeat
    $dependentAlist := PUTALIST($dependentAlist,dependee,
      [dependent,:GETALIST($dependentAlist,dependee)])
    $dependeeAlist  := PUTALIST($dependeeAlist,dependent,
      [dependee,:GETALIST($dependeeAlist,dependent)])

fixObjectForPrinting(v) ==
    v' := object2Identifier v
    v' = "%" => '"\%"
    member(v',$msgdbPrims) => strconc('"\",PNAME v')
    v

displayProperties(option,l) ==
  $dependentAlist : local := nil
  $dependeeAlist  : local := nil
  [opt,:vl]:= (l or ['properties])
  imacs := getInterpMacroNames()
  pmacs := getParserMacroNames()
  macros := removeDuplicates append(imacs, pmacs)
  if vl is ['all] or null vl then
    vl := MSORT append(getWorkspaceNames(),macros)
  if $frameMessages then sayKeyedMsg("S2IZ0065",[$interpreterFrameName])
  null vl =>
    null $frameMessages => sayKeyedMsg("S2IZ0066",NIL)
    sayKeyedMsg("S2IZ0067",[$interpreterFrameName])
  interpFunctionDepAlists()
  for v in vl repeat
    isInternalMapName(v) => 'iterate
    pl := getIProplist(v)
    option = 'flags =>     getAndSay(v,"flags")
    option = 'value =>     displayValue(v,getI(v,'value),nil)
    option = 'condition => displayCondition(v,getI(v,"condition"),nil)
    option = 'mode =>      displayMode(v,getI(v,'mode),nil)
    option = 'type =>      displayType(v,getI(v,'value),nil)
    option = 'properties =>
      v = "--flags--" => nil
      pl is [['cacheInfo,:.],:.] => nil
      v1 := fixObjectForPrinting(v)
      sayMSG ['"Properties of",:bright prefix2String v1,'":"]
      null pl =>
        v in pmacs =>
            sayMSG '"   This is a user-defined macro."
            displayParserMacro v
        isInterpMacro v =>
            sayMSG '"   This is a system-defined macro."
            displayMacro v
        sayMSG '"   none"
      propsSeen:= nil
      for [prop,:val] in pl | not MEMQ(prop,propsSeen) and val repeat
        prop in '(alias generatedCode IS_-GENSYM mapBody localVars) =>
          nil
        prop = 'condition =>
          displayCondition(prop,val,true)
        prop = 'recursive =>
          sayMSG '"   This is recursive."
        prop = 'isInterpreterFunction =>
          sayMSG '"   This is an interpreter function."
          sayFunctionDeps v where
            sayFunctionDeps x ==
              if dependents := GETALIST($dependentAlist,x) then
                null rest dependents =>
                  sayMSG ['"   The following function or rule ",
                    '"depends on this:",:bright first dependents]
                sayMSG
                  '"   The following functions or rules depend on this:"
                msg := ["%b",'"     "]
                for y in dependents repeat msg := ['" ",y,:msg]
                sayMSG [:nreverse msg,"%d"]
              if dependees := GETALIST($dependeeAlist,x) then
                null rest dependees =>
                  sayMSG ['"   This depends on the following function ",
                    '"or rule:",:bright first dependees]
                sayMSG
                  '"   This depends on the following functions or rules:"
                msg := ["%b",'"     "]
                for y in dependees repeat msg := ['" ",y,:msg]
                sayMSG [:nreverse msg,"%d"]
        prop = 'isInterpreterRule =>
          sayMSG '"   This is an interpreter rule."
          sayFunctionDeps v
        prop = 'localModemap =>
          displayModemap(v,val,true)
        prop = 'mode =>
          displayMode(prop,val,true)
        prop = 'value =>
          val => displayValue(v,val,true)
        sayMSG ['"   ",prop,'":  ",val]
        propsSeen:= [prop,:propsSeen]
    sayKeyedMsg("S2IZ0068",[option])
  terminateSystemCommand()

displayModemap(v,val,giveVariableIfNil) ==
  for mm in val repeat g(v,mm,giveVariableIfNil) where
    g(v,mm,giveVariableIfNil) ==
      [[local,:signature],fn,:.]:= mm
      local='interpOnly => nil
      varPart:= (giveVariableIfNil => nil; ['" of",:bright v])
      prefix:= ["   Compiled function type",:varPart,": "]
      sayBrightly concat(prefix,formatSignature signature)

displayMode(v,mode,giveVariableIfNil) ==
  null mode => nil
  varPart:= (giveVariableIfNil => nil; [" of",:bright fixObjectForPrinting v])
  sayBrightly concat("   Declared type or mode",
    varPart,":   ",prefix2String mode)

displayCondition(v,condition,giveVariableIfNil) ==
  varPart:= (giveVariableIfNil => nil; [" of",:bright v])
  condPart:= condition or 'true
  sayBrightly concat("   condition",varPart,":  ",pred2English condPart)

getAndSay(v,prop) ==
  val:= getI(v,prop) => sayMSG ["    ",val,'"%l"]
  sayMSG ["    none",'"%l"]

displayType($op,u,omitVariableNameIfTrue) ==
  null u =>
    sayMSG ['"   Type of value of ",
        fixObjectForPrinting PNAME $op,'":  (none)"]
  type := prefix2String objMode(u)
  if atom type then type := [type]
  sayMSG concat ['"   Type of value of ",fixObjectForPrinting PNAME $op,'": ",:type]
  NIL

displayValue($op,u,omitVariableNameIfTrue) ==
  null u => sayMSG ["   Value of ",fixObjectForPrinting PNAME $op,'":  (none)"]
  expr := objValUnwrap(u)
  expr is [op,:.] and (op = "%Map") or objMode(u) = $EmptyMode =>
    displayRule($op,expr)
  label:=
    omitVariableNameIfTrue =>
        rhs := '"):  "
        '"Value (has type "
    rhs := '":  "
    strconc('"Value of ", PNAME $op,'": ")
  labmode := prefix2String objMode(u)
  if atom labmode then labmode := [labmode]
  IDENTP expr and getConstructorKindFromDB expr = "domain" =>
    sayMSG concat('"   ",label,labmode,rhs,form2String expr)
  mathprint ['CONCAT,label,:labmode,rhs,
    outputFormat(expr,objMode(u))]
  NIL

--% )edit

edit l == editSpad2Cmd l

editSpad2Cmd l ==
  l:= 
    null l => _/EDITFILE
    first l
  l := pathname STRING l
  oldDir := pathnameDirectory l
  fileTypes :=
    pathnameType l => [pathnameType l]
    $UserLevel = 'interpreter => '("input" "INPUT" "spad" "SPAD")
    $UserLevel = 'compiler    => '("input" "INPUT" "spad" "SPAD")
    '("input" "INPUT" "spad" "SPAD" "boot" "BOOT" "lisp" "LISP" "meta" "META")
  ll := 
       oldDir = '"" => pathname $FINDFILE (pathnameName l, fileTypes)
       l
  l := pathname ll
  SETQ(_/EDITFILE,l)
  rc := editFile l
  updateSourceFiles l
  rc

--% )help

help l == helpSpad2Cmd l

helpSpad2Cmd args ==
  -- try to use new stuff first
  if newHelpSpad2Cmd(args) then return nil

  sayKeyedMsg("S2IZ0025",[args])
  nil

newHelpSpad2Cmd args ==
  if null args then args := ["?"]
  # args > 1 =>
    sayKeyedMsg("S2IZ0026",NIL)
    true
  sarg := PNAME first args
  if sarg = '"?" then args := ['help]
  else if sarg = '"%" then args := ['history]
       else if sarg = '"%%" then args := ['history]
  arg := selectOptionLC(first args,$SYSCOMMANDS,nil)
  if null arg then arg := first args
  if arg = 'compiler then arg := 'compile

  -- see if new help file exists

  narg := PNAME arg
  null (helpFile := MAKE_-INPUT_-FILENAME [narg,'HELPSPAD,'_*]) => NIL

  $useFullScreenHelp =>
    editFile helpFile
    true

  filestream := MAKE_-INSTREAM(helpFile)
  repeat
    line := read_-line(filestream,false)
    null line =>
      SHUT filestream
      return true
    SAY line
  true

--%
--% )frame
--%

$frameRecord  := nil  --Initial setting for frame record
$previousBindings := nil

frame l == frameSpad2Cmd l

frameName(frame) == first frame

frameNames() == [frameName f for f in $interpreterFrameRing]

frameEnvironment fname ==
  -- extracts the environment portion of a frame
  -- if fname is not a valid frame name then the empty environment
  -- is returned
  fname = frameName first $interpreterFrameRing => $InteractiveFrame
  ifr := rest $interpreterFrameRing
  e := [[NIL]]
  while ifr repeat
    [f,:ifr] := ifr
    if fname = frameName f   then
      e := second f
      ifr := NIL
  e

frameSpad2Cmd args ==
  frameArgs := '(drop import last names new next)
  $options => throwKeyedMsg("S2IZ0016",['")frame"])
  null(args) => helpSpad2Cmd ['frame]
  arg  := selectOptionLC(first args,frameArgs,'optionError)
  args := rest args
  if args is [a] then args := a
  if atom args then args := object2Identifier args
  arg = 'drop  =>
    args and cons?(args) => throwKeyedMsg("S2IZ0017",[args])
    closeInterpreterFrame(args)
  arg = "import" =>  importFromFrame args
  arg = "last"  =>   previousInterpreterFrame()
  arg = "names" =>   displayFrameNames()
  arg = "new"   =>
    args and cons?(args) => throwKeyedMsg("S2IZ0017",[args])
    addNewInterpreterFrame(args)
  arg = "next"  =>   nextInterpreterFrame()

  NIL

addNewInterpreterFrame(name) ==
  null name => throwKeyedMsg("S2IZ0018",NIL)
  updateCurrentInterpreterFrame()
  -- see if we already have one by that name
  for f in $interpreterFrameRing repeat
    name = frameName(f) => throwKeyedMsg("S2IZ0019",[name])
  initHistList()
  $interpreterFrameRing := [emptyInterpreterFrame(name),
    :$interpreterFrameRing]
  updateFromCurrentInterpreterFrame()
  _$ERASE histFileName()

emptyInterpreterFrame(name) ==
  [name,                                -- frame name
       [[NIL]],                         -- environment
       1,                               -- $IOindex
       $HiFiAccess,                     -- $HiFiAccess
       $HistList,                       -- $HistList
       $HistListLen,                    -- $HistListLen
       $HistListAct,                    -- $HistListAct
       $HistRecord,                     -- $HistRecord
       NIL,                             -- $internalHistoryTable
       COPY_-SEQ $localExposureDataDefault        -- $localExposureData
      ]

closeInterpreterFrame(name) ==
  -- if name = NIL then it means the current frame
  null rest $interpreterFrameRing =>
    name and (name ~= $interpreterFrameName) =>
      throwKeyedMsg("S2IZ0020",[$interpreterFrameName])
    throwKeyedMsg("S2IZ0021",NIL)
  if null name then $interpreterFrameRing := rest $interpreterFrameRing
  else   -- find the frame
    found := nil
    ifr := NIL
    for f in $interpreterFrameRing repeat
      found or (name ~= frameName(f)) => ifr := [f,:ifr]
      found := true
    not found => throwKeyedMsg("S2IZ0022",[name])
    _$ERASE makeHistFileName(name)
    $interpreterFrameRing := nreverse ifr
  updateFromCurrentInterpreterFrame()

previousInterpreterFrame() ==
  updateCurrentInterpreterFrame()
  null rest $interpreterFrameRing => NIL  -- nothing to do
  [:b,l] := $interpreterFrameRing
  $interpreterFrameRing := NCONC2([l],b)
  updateFromCurrentInterpreterFrame()

nextInterpreterFrame() ==
  updateCurrentInterpreterFrame()
  null rest $interpreterFrameRing => NIL  -- nothing to do
  $interpreterFrameRing :=
    NCONC2(rest $interpreterFrameRing,[first $interpreterFrameRing])
  updateFromCurrentInterpreterFrame()


createCurrentInterpreterFrame() ==
  [$interpreterFrameName,           -- frame name
       $InteractiveFrame,               -- environment
       $IOindex,                        -- $IOindex
       $HiFiAccess,                     -- $HiFiAccess
       $HistList,                       -- $HistList
       $HistListLen,                    -- $HistListLen
       $HistListAct,                    -- $HistListAct
       $HistRecord,                     -- $HistRecord
       $internalHistoryTable,           -- $internalHistoryTable
       $localExposureData               -- $localExposureData
      ]


updateFromCurrentInterpreterFrame() ==
  [$interpreterFrameName,          _
   $InteractiveFrame,              _
   $IOindex,                       _
   $HiFiAccess,                    _
   $HistList,                      _
   $HistListLen,                   _
   $HistListAct,                   _
   $HistRecord,                    _
   $internalHistoryTable,          _
   $localExposureData              _
   ] := first $interpreterFrameRing
  if $frameMessages then
    sayMessage ['"   Current interpreter frame is called",:bright
      $interpreterFrameName]
  NIL


updateCurrentInterpreterFrame() ==
  $interpreterFrameRing.first := createCurrentInterpreterFrame()
  updateFromCurrentInterpreterFrame()
  NIL

initializeInterpreterFrameRing() ==
  $interpreterFrameName := 'initial
  $interpreterFrameRing := [emptyInterpreterFrame($interpreterFrameName)]
  updateFromCurrentInterpreterFrame()
  NIL


changeToNamedInterpreterFrame(name) ==
  updateCurrentInterpreterFrame()
  frame := findFrameInRing(name)
  null frame => NIL
  $interpreterFrameRing := [frame,:NREMOVE($interpreterFrameRing, frame)]
  updateFromCurrentInterpreterFrame()

findFrameInRing(name) ==
  val := NIL
  for frame in $interpreterFrameRing repeat
    first frame = name =>
      val := frame
      return frame
  val

displayFrameNames() ==
  fs := "append"/[ ['"%l",'"     ",:bright frameName f] for f in
    $interpreterFrameRing]
  sayKeyedMsg("S2IZ0024",[fs])

importFromFrame args ==
  -- args should have the form [frameName,:varNames]
  if args and atom args then args := [args]
  null args => throwKeyedMsg("S2IZ0073",NIL)
  [fname,:args] := args
  not member(fname,frameNames()) =>
    throwKeyedMsg("S2IZ0074",[fname])
  fname = frameName first $interpreterFrameRing =>
    throwKeyedMsg("S2IZ0075",NIL)
  fenv := frameEnvironment fname
  null args =>
    x := UPCASE queryUserKeyedMsg("S2IZ0076",[fname])
    STRING2ID_-N(x,1) in '(Y YES) =>
      vars := NIL
      for [v,:props] in CAAR fenv repeat
        v = "--macros" =>
          for [m,:.] in props repeat vars := [m,:vars]
        vars := [v,:vars]
      importFromFrame [fname,:vars]
    sayKeyedMsg("S2IZ0077",[fname])
  for v in args repeat
    plist := GETALIST(CAAR fenv,v)
    plist =>
      -- remove anything with the same name in the current frame
      clearCmdParts ['propert,v]
      for [prop,:val] in plist repeat
        putHist(v,prop,val,$InteractiveFrame)
    (m := get("--macros--",v,fenv)) =>
      putHist("--macros--",v,m,$InteractiveFrame)
    sayKeyedMsg("S2IZ0079",[v,fname])
  sayKeyedMsg("S2IZ0078",[fname])



--% )history

++ vm/370 filename type component
$historyFileType := 'axh

++ vm/370 filename name component
$oldHistoryFileName := 'last
$internalHistoryTable := NIL

++ t means keep history in core
$useInternalHistoryTable := true

++ vm/370 filename disk component
$historyDirectory := "A"

++ true means turn on history mechanism
$HiFiAccess := true

history l ==
  l or null $options => sayKeyedMsg("S2IH0006",NIL) 
  historySpad2Cmd()


makeHistFileName(fname) ==
  makePathname(fname,$historyFileType,$historyDirectory)

oldHistFileName() ==
  makeHistFileName($oldHistoryFileName)

histFileName() ==
  makeHistFileName($interpreterFrameName)


histInputFileName(fn) ==
  null fn =>
    makePathname($interpreterFrameName,'INPUT,$historyDirectory)
  makePathname(fn,'INPUT,$historyDirectory)


initHist() ==
  $useInternalHistoryTable => initHistList()
  oldFile := oldHistFileName()
  newFile := histFileName()
  -- see if history directory is writable
  histFileErase oldFile
  if MAKE_-INPUT_-FILENAME newFile then $REPLACE(oldFile,newFile)
  $HiFiAccess:= true
  initHistList()

initHistList() ==
  -- creates $HistList as a circular list of length $HistListLen
  -- and $HistRecord
  $HistListLen:= 20
  $HistList:= [NIL]
  li:= $HistList
  for i in 1..$HistListLen repeat li:= [NIL,:li]
  $HistList.rest := li
  $HistListAct:= 0
  $HistRecord:= NIL

historySpad2Cmd() ==
  -- history is a system command which can call resetInCoreHist
  -- and changeHistListLen, and restore last session
  histOptions:=
    '(on off yes no change reset restore write save show file memory)
  opts:= [ [selectOptionLC(opt,histOptions,'optionError),:optargs]
    for [opt,:optargs] in $options]
  for [opt,:optargs] in opts repeat
    opt in '(on yes) =>
      $HiFiAccess => sayKeyedMsg("S2IH0007",NIL) 
      $IOindex = 1 =>       -- haven't done anything yet
        $HiFiAccess:= true
        initHistList()
        sayKeyedMsg("S2IH0008",NIL) 
      x := UPCASE queryUserKeyedMsg("S2IH0009",NIL) 
      STRING2ID_-N(x,1) in '(Y YES) =>
        histFileErase histFileName()
        $HiFiAccess:= true
        $options := nil
        clearSpad2Cmd '(all)
        sayKeyedMsg("S2IH0008",NIL)
        initHistList()
      sayKeyedMsg("S2IH0010",NIL)
    opt in '(off no) =>
      null $HiFiAccess => sayKeyedMsg("S2IH0011",NIL)
      $HiFiAccess:= false
      disableHist()
      sayKeyedMsg("S2IH0012",NIL)
    opt = 'file    => setHistoryCore NIL
    opt = 'memory  => setHistoryCore true
    opt = 'reset   => resetInCoreHist()
    opt = 'save    => saveHistory optargs
    opt = 'show    => showHistory optargs
    opt = 'change  => changeHistListLen first optargs
    opt = 'restore => restoreHistory optargs
    opt = 'write   => writeInputLines(optargs,1)
  'done


setHistoryCore inCore ==
  inCore = $useInternalHistoryTable =>
    sayKeyedMsg((inCore => "S2IH0030"; "S2IH0029"),NIL) 
  not $HiFiAccess =>
    $useInternalHistoryTable := inCore
    inCore => sayKeyedMsg("S2IH0032",NIL)
    sayKeyedMsg("S2IH0031",NIL)
  inCore =>
    $internalHistoryTable := NIL
    if $IOindex ~= 0 then
      -- actually put something in there
      l := # RKEYIDS histFileName()
      for i in 1..l repeat
        vec:= (try readHiFi(i); finally disableHist())
        $internalHistoryTable := [[i,:vec],:$internalHistoryTable]
      histFileErase histFileName()
    $useInternalHistoryTable := true
    sayKeyedMsg("S2IH0032",NIL)
  $HiFiAccess:= false
  histFileErase histFileName()
  str := RDEFIOSTREAM ['(MODE . OUTPUT),['FILE,:histFileName()]]
  for [n,:rec] in reverse $internalHistoryTable repeat
    SPADRWRITE(object2Identifier n,rec,str)
  RSHUT str
  $HiFiAccess:= true
  $internalHistoryTable := NIL
  $useInternalHistoryTable := NIL
  sayKeyedMsg("S2IH0031",NIL)


writeInputLines(fn,initial) == 
  -- writes all input lines into file histInputFileName()
  not $HiFiAccess => sayKeyedMsg("S2IH0013",NIL) -- history not on
  null fn =>
    throwKeyedMsg("S2IH0038", nil)          -- missing file name
  maxn := 72
  breakChars := [" ","+"]
  for i in initial..$IOindex - 1 repeat
    vecl := first readHiFi i
    if string? vecl then vecl := [vecl]
    for vec in vecl repeat
      n := # vec
      while n > maxn repeat
        -- search backwards for a blank
        done := nil
        for j in 1..maxn while not done repeat
          k := 1 + maxn - j
          MEMQ(vec.k,breakChars) =>
            svec := strconc(subString(vec,0,k+1),UNDERBAR)
            lineList := [svec,:lineList]
            done := true
            vec := subString(vec,k+1)
            n := # vec
        -- in case we can't find a breaking point
        if not done then n := 0
      lineList := [vec,:lineList]
  file := histInputFileName(fn)
  histFileErase file
  inp:= DEFIOSTREAM(['(MODE . OUTPUT),['FILE,:file]],255,0)
  for x in removeUndoLines nreverse lineList repeat writeLine(x,inp)
  -- see file "undo" for definition of removeUndoLines
  if fn ~= 'redo then sayKeyedMsg("S2IH0014",[namestring file])
  SHUT inp
  NIL


resetInCoreHist() ==
  -- removes all pointers from $HistList
  $HistListAct:= 0
  for i in 1..$HistListLen repeat
    $HistList:= rest $HistList
    $HistList.first := NIL

changeHistListLen(n) ==
  -- changes the length of $HistList.  n must be nonnegative
  not integer? n => sayKeyedMsg("S2IH0015",[n]) 
  dif:= n-$HistListLen
  $HistListLen:= n
  l:= rest $HistList
  if dif > 0 then
    for i in 1..dif repeat l:= [NIL,:l]
  if dif < 0 then
    for i in 1..-dif repeat l:= rest l
    if $HistListAct > n then $HistListAct:= n
  $HistList.rest := l
  'done

updateHist() ==
  -- updates the history file and calls updateInCoreHist
  null $IOindex => nil
  startTimingProcess 'history
  updateInCoreHist()
  if $HiFiAccess then
    (try writeHiFi(); finally disableHist())
    $HistRecord:= NIL
  $IOindex:= $IOindex+1
  updateCurrentInterpreterFrame()
  $mkTestInputStack := nil
  $currentLine := nil
  stopTimingProcess 'history

updateInCoreHist() ==
  -- updates $HistList and $IOindex
  $HistList:= rest($HistList)
  $HistList.first := NIL
  if $HistListAct < $HistListLen then $HistListAct:= $HistListAct+1

putHist(x,prop,val,e) ==
  -- records new value to $HistRecord and old value to $HistList
  -- then put is called with e
  if not (x='%) then recordOldValue(x,prop,get(x,prop,e))
  if $HiFiAccess then recordNewValue(x,prop,val)
  putIntSymTab(x,prop,val,e)

histFileErase file ==
  removeFile file



recordNewValue(x,prop,val) ==
  startTimingProcess 'history
  recordNewValue0(x,prop,val)
  stopTimingProcess 'history

recordNewValue0(x,prop,val) ==
  -- writes (prop . val) into $HistRecord
  -- updateHist writes this stuff out into the history file
  p1:= ASSQ(x,$HistRecord) =>
    p2:= ASSQ(prop,rest p1) =>
      p2.rest := val
    p1.rest := [[prop,:val],:rest p1]
  p:= [x,:list [prop,:val]]
  $HistRecord:= [p,:$HistRecord]

recordOldValue(x,prop,val) ==
  startTimingProcess 'history
  recordOldValue0(x,prop,val)
  stopTimingProcess 'history

recordOldValue0(x,prop,val) ==
  -- writes (prop . val) into $HistList
  p1:= ASSQ(x,first $HistList) =>
    not ASSQ(prop,rest p1) =>
      p1.rest := [[prop,:val],:rest p1]
  p:= [x,:list [prop,:val]]
  $HistList.first := [p,:first $HistList]

undoInCore(n) ==
  -- undoes the last n>0 steps using $HistList
  -- resets $InteractiveFrame
  li:= $HistList
  for i in n..$HistListLen repeat li:= rest li
  undoChanges(li)
  n:= $IOindex-n-1
  n>0 and
    $HiFiAccess =>
      vec:= rest (try readHiFi(n); finally disableHist())
      val:= ( p:= ASSQ('%,vec) ) and ( p1:= ASSQ('value,rest p) ) and
        rest p1
    sayKeyedMsg("S2IH0019",[n])
  $InteractiveFrame:= putHist('%,'value,val,$InteractiveFrame)
  updateHist()

undoChanges(li) ==
  -- undoes all changes of list 'li'
  if not rest li = $HistList then undoChanges rest li
  for p1 in first li repeat
    x:= first p1
    for p2 in rest p1 repeat
      putHist(x,first p2,rest p2,$InteractiveFrame)

undoFromFile(n) ==
  -- makes a clear and redoes all the assignments until step n
  for [x,:varl] in CAAR $InteractiveFrame repeat
    for p in varl repeat
      [prop,:val]:= p
      val =>
        if not (x='%) then recordOldValue(x,prop,val)
        if $HiFiAccess then recordNewValue(x,prop,val)
        p.rest := NIL
  for i in 1..n repeat
    vec:= (try rest readHiFi(i); finally disableHist())
    for p1 in vec repeat
      x:= first p1
      for p2 in rest p1 repeat
        $InteractiveFrame:= putHist(x,first p2,rest p2,$InteractiveFrame)
  val:= ( p:= ASSQ('%,vec) ) and ( p1:= ASSQ('value,rest p) ) and rest p1
  $InteractiveFrame:= putHist('%,'value,val,$InteractiveFrame)
  updateHist()

saveHistory(fn) ==
  $seen: local := hashTable 'EQ
  not $HiFiAccess => sayKeyedMsg("S2IH0016",NIL)
  not $useInternalHistoryTable and
    null MAKE_-INPUT_-FILENAME histFileName() => sayKeyedMsg("S2IH0022",NIL)
  null fn => 
    throwKeyedMsg("S2IH0037", nil)
  savefile := makeHistFileName(fn)
  inputfile := histInputFileName(fn)
  writeInputLines(fn,1)
  histFileErase savefile
 
  if $useInternalHistoryTable
    then
      saveStr := RDEFIOSTREAM ['(MODE . OUTPUT),['FILE,:savefile]]
      for [n,:rec] in reverse $internalHistoryTable repeat
        val := SPADRWRITE0(object2Identifier n,rec,saveStr)
        val = 'writifyFailed =>
          sayKeyedMsg("S2IH0035", [n, inputfile]) -- unable to save step
      RSHUT saveStr
  sayKeyedMsg("S2IH0018",[namestring(savefile)])  -- saved hist file named
  nil

restoreHistory(fn) ==
  -- uses fn $historyFileType to recover an old session
  -- if fn = NIL, then use $oldHistoryFileName
  if null fn then fn' := $oldHistoryFileName
  else if fn is [fn'] and IDENTP(fn') then fn' := fn'
       else throwKeyedMsg("S2IH0023",[fn'])
  restfile := makeHistFileName(fn')
  null MAKE_-INPUT_-FILENAME restfile =>
    sayKeyedMsg("S2IH0024",[namestring(restfile)]) -- no history file
 
  -- if clear is changed to be undoable, this should be a reset-clear
  $options: local := nil
  clearSpad2Cmd '(all)
 
  curfile := histFileName()
  histFileErase curfile
  _$FCOPY(restfile,curfile)
 
  l:= # RKEYIDS curfile
  $HiFiAccess:= true
  oldInternal := $useInternalHistoryTable
  $useInternalHistoryTable := NIL
  if oldInternal then $internalHistoryTable := NIL
  for i in 1..l repeat
    vec:= (try readHiFi(i); finally disableHist())
    if oldInternal then $internalHistoryTable :=
      [[i,:vec],:$internalHistoryTable]
    LINE:= first vec
    for p1 in rest vec repeat
      x:= first p1
      for p2 in rest p1 repeat
        $InteractiveFrame:= putHist(x,first p2,rest p2,$InteractiveFrame)
    updateInCoreHist()
  $e := $InteractiveFrame
  for [a,:.] in CAAR $InteractiveFrame repeat
    get(a,'localModemap,$InteractiveFrame) =>
      rempropI(a,'localModemap)
      rempropI(a,'localVars)
      rempropI(a,'mapBody)
  $IOindex:= l+1
  $useInternalHistoryTable := oldInternal
  sayKeyedMsg("S2IH0025",[namestring(restfile)]) 
  clearCmdSortedCaches()
  nil


-- the following used to be the show command when that was used to
-- show history.
showHistory(arg) ==
  -- arg can be of form
  --    NIL          show at most last 20 input lines
  --    (n)          show at most last n input lines
  --    (lit)        where lit is an abbreviation for 'input or 'both
  --                 if 'input, same as NIL
  --                 if 'both, show last 5 input and outputs
  --    (n lit)      show last n input lines + last n output lines
  --                 if lit expands to 'both
  $evalTimePrint: local:= 0
  $printTimeSum: local:= 0
  -- ugh!!! these are needed for timedEvaluateStream
  -- displays the last n steps, default n=20
  not $HiFiAccess => sayKeyedMsg("S2IH0026",['show])
  showInputOrBoth := 'input
  n := 20
  nset := nil
  if arg then
    arg1 := first arg
    if integer? arg1 then
      n := arg1
      nset := true
      KDR arg => arg1 := second arg
      arg1 := NIL
    arg1 =>
      arg2 := selectOptionLC(arg1,'(input both),nil)
      if arg2
        then ((showInputOrBoth := arg2) = 'both) and (null nset) => n:= 5
        else sayMSG
          concat('"  ",bright arg1,'"is an invalid argument.")
  if n >= $IOindex then n:= $IOindex-1
  mini:= $IOindex-n
  maxi:= $IOindex-1
  showInputOrBoth = 'both =>
    (try showInOut(mini,maxi); finally setIOindex(maxi+1))
  showInput(mini,maxi)

setIOindex(n) ==
  -- set $IOindex to n
  $IOindex:= n

showInput(mini,maxi) ==
  -- displays all input lines from mini to maxi
  for ind in mini..maxi repeat
    vec:= (try readHiFi(ind); finally disableHist())
    if ind<10 then TAB 2 else if ind<100 then TAB 1
    l := first vec
    string? l =>
      sayMSG ['"   [",ind,'"] ",first vec]
    sayMSG ['"   [",ind,'"] " ]
    for ln in l repeat
      sayMSG ['"      ", ln]

showInOut(mini,maxi) ==
  -- displays all steps from mini to maxi
  for ind in mini..maxi repeat
    vec:= (try readHiFi(ind); finally disableHist())
    sayMSG [first vec]
    Alist:= ASSQ('%,rest vec) =>
      triple:= rest ASSQ('value,rest Alist)
      $IOindex:= ind
      spadPrint(objValUnwrap triple,objMode triple)

fetchOutput(n) ==
  -- result is the output of step n
  (n = -1) and (val := getI("%",'value)) => val
  $HiFiAccess =>
    n:=
      n < 0 => $IOindex+n
      n
    n >= $IOindex => throwKeyedMsg("S2IH0001",[n])
    n < 1        => throwKeyedMsg("S2IH0002",[n])
    vec:= (try readHiFi(n); finally disableHist())
    Alist:= ASSQ('%,rest vec) =>
      val:= rest ASSQ('value,rest Alist) => val
      throwKeyedMsg("S2IH0003",[n])
    throwKeyedMsg("S2IH0003",[n])
  throwKeyedMsg("S2IH0004",NIL)

readHiFi(n) ==
  -- reads the file using index n
  if $useInternalHistoryTable
  then
    pair := assoc(n,$internalHistoryTable)
    atom pair => keyedSystemError("S2IH0034",NIL)
    vec := rest pair
  else
    HiFi:= RDEFIOSTREAM ['(MODE . INPUT),['FILE,:histFileName()]]
    vec:= SPADRREAD(object2Identifier n,HiFi)
    RSHUT HiFi
  vec

writeHiFi() ==
  -- writes the information of the current step out to history file
  if $useInternalHistoryTable
  then
    $internalHistoryTable := [[$IOindex,$currentLine,:$HistRecord],
                                 :$internalHistoryTable]
  else
    HiFi:= RDEFIOSTREAM ['(MODE . OUTPUT),['FILE,:histFileName()]]
    SPADRWRITE(object2Identifier $IOindex, [$currentLine,:$HistRecord],HiFi)
    RSHUT HiFi

disableHist() ==
  -- disables the history mechanism if an error occurred in the protected
  -- piece of code
  not $HiFiAccess => histFileErase histFileName()
  NIL

writeHistModesAndValues() ==
  for [a,:.] in CAAR $InteractiveFrame repeat
    x := get(a,'value,$InteractiveFrame) =>
      putHist(a,'value,x,$InteractiveFrame)
    x := get(a,'mode,$InteractiveFrame) =>
      putHist(a,'mode,x,$InteractiveFrame)
  NIL

SPADRREAD(vec, stream) ==
    dewritify rread(vec, stream, nil)

--% Lisplib output transformations
--  Some types of objects cannot be saved by LISP/VM in lisplibs.
--  These functions transform an object to a writable form and back.
--  SMW
SPADRWRITE(vec, item, stream) ==
  val := SPADRWRITE0(vec, item, stream) 
  val = 'writifyFailed =>
    throwKeyedMsg("S2IH0036", nil) -- cannot save value to file
  item

SPADRWRITE0(vec, item, stream) ==
    val := safeWritify item
    val = 'writifyFailed => val
    rwrite(vec, val, stream)
    item

safeWritify ob ==
  CATCH('writifyTag,  writify ob)

writify ob ==
    not ScanOrPairVec(function(unwritable?), ob) => ob
    $seen:     local := hashTable 'EQ
    $writifyComplained: local := false
 
    writifyInner ob where
        writifyInner ob ==
            null ob                => nil
            (e := HGET($seen, ob)) => e
 
            cons? ob =>
                qcar := first ob
                qcdr := rest ob
                (name := spadClosure? ob) =>
                   d := writifyInner rest ob
                   nob := ['WRITIFIED_!_!, 'SPADCLOSURE, d, name]
                   HPUT($seen, ob, nob)
                   HPUT($seen, nob, nob)
                   nob
                (ob is ['LAMBDA_-CLOSURE, ., ., x, :.]) and x =>
                  THROW('writifyTag, 'writifyFailed)
                nob := [qcar,:qcdr]
                HPUT($seen, ob, nob)
                HPUT($seen, nob, nob)
                qcar := writifyInner qcar
                qcdr := writifyInner qcdr
                nob.first := qcar
                nob.rest := qcdr
                nob
            vector? ob =>
                isDomainOrPackage ob =>
                    d := mkEvalable devaluate ob
                    nob := ['WRITIFIED_!_!, 'DEVALUATED, writifyInner d]
                    HPUT($seen, ob, nob)
                    HPUT($seen, nob, nob)
                    nob
                n   := QVMAXINDEX ob
                nob := newVector(n+1)
                HPUT($seen, ob, nob)
                HPUT($seen, nob, nob)
                for i in 0..n repeat
                    vectorRef(nob, i) := writifyInner vectorRef(ob,i)
                nob
            ob = 'WRITIFIED_!_! =>
                ['WRITIFIED_!_!, 'SELF]
            -- In CCL constructors are also compiled functions, so we 
            -- need this line:
            constructor? ob => ob
            COMPILED_-FUNCTION_-P ob =>
                THROW('writifyTag, 'writifyFailed)
            HASHTABLEP ob =>
                nob := ['WRITIFIED_!_!]
                HPUT($seen, ob,  nob)
                HPUT($seen, nob, nob)
                keys := HKEYS ob
                nob.rest := 
                        ['HASHTABLE,
                          HASHTABLE_-CLASS ob,
                            writifyInner keys,
                              [writifyInner HGET(ob,k) for k in keys]]
                nob
            PLACEP ob =>
                nob := ['WRITIFIED_!_!, 'PLACE]
                HPUT($seen, ob,  nob)
                HPUT($seen, nob, nob)
                nob
            -- The next three types cause an error on de-writifying.
            -- Create an object of the right shape, nonetheless.
            READTABLEP ob =>
                THROW('writifyTag, 'writifyFailed)
            -- Default case: return the object itself.
            string? ob =>
                EQ(ob, $NullStream) => ['WRITIFIED_!_!, 'NULLSTREAM]
                EQ(ob, $NonNullStream) => ['WRITIFIED_!_!, 'NONNULLSTREAM]
                ob
            FLOATP ob =>
                ob = READ_-FROM_-STRING STRINGIMAGE ob => ob
                ['WRITIFIED_!_!, 'FLOAT, ob,:
                   MULTIPLE_-VALUE_-LIST INTEGER_-DECODE_-FLOAT ob]
            ob


unwritable? ob ==
    cons?  ob or vector? ob       => false   -- first for speed
    COMPILED_-FUNCTION_-P   ob or HASHTABLEP ob => true
    PLACEP ob or READTABLEP ob => true
    FLOATP ob => true
    false

-- Create a full isomorphic object which can be saved in a lisplib.
-- Note that  dewritify(writify(x))  preserves UEQUALity of hashtables.
-- HASHTABLEs go both ways.
-- READTABLEs cannot presently be transformed back.
 
writifyComplain s ==
   $writifyComplained  = true => nil
   $writifyComplained := true
   sayKeyedMsg("S2IH0027",[s]) 

spadClosure? ob ==
  fun := first ob
  not (name := BPINAME fun) => nil
  vec := rest ob
  not vector? vec => nil
  name

dewritify ob ==
    (not ScanOrPairVec(function is?, ob)
            where  is? a == a = 'WRITIFIED_!_!) => ob
 
    $seen:     local := hashTable 'EQ
 
    dewritifyInner ob where
        dewritifyInner ob ==
            null ob => nil
            e := HGET($seen, ob) => e
 
            cons? ob and first ob = 'WRITIFIED_!_! =>
                type := ob.1
                type = 'SELF =>
                    'WRITIFIED_!_!
                type = 'BPI =>
                    oname := ob.2
                    f :=
                        integer? oname => eval GENSYMMER oname
                        symbolFunction oname
                    not COMPILED_-FUNCTION_-P f =>
                        error '"A required BPI does not exist."
                    #ob > 3 and HASHEQ f ~= ob.3 =>
                        error '"A required BPI has been redefined."
                    HPUT($seen, ob, f)
                    f
                type = 'HASHTABLE =>
                    nob := hashTable ob.2
                    HPUT($seen, ob, nob)
                    HPUT($seen, nob, nob)
                    for k in ob.3 for e in ob.4 repeat
                        HPUT(nob, dewritifyInner k, dewritifyInner e)
                    nob
                type = 'DEVALUATED =>
                    nob := eval dewritifyInner ob.2
                    HPUT($seen, ob, nob)
                    HPUT($seen, nob, nob)
                    nob
                type = 'SPADCLOSURE =>
                    vec := dewritifyInner ob.2
                    name := ob.3
                    not FBOUNDP name => 
                       error strconc('"undefined function: ", symbolName name)
                    nob := [symbolFunction name,:vec]
                    HPUT($seen, ob, nob)
                    HPUT($seen, nob, nob)
                    nob
                type = 'PLACE =>
                    nob := VMREAD MAKE_-INSTREAM NIL
                    HPUT($seen, ob, nob)
                    HPUT($seen, nob, nob)
                    nob
                type = 'READTABLE =>
                    error '"Cannot de-writify a read table."
                type = 'NULLSTREAM => $NullStream
                type = 'NONNULLSTREAM => $NonNullStream
                type = 'FLOAT =>
                   [fval, signif, expon, sign] := CDDR ob
                   fval := SCALE_-FLOAT( FLOAT(signif, fval), expon)
                   sign<0 => -fval
                   fval
                error '"Unknown type to de-writify."
 
            cons? ob =>
                qcar := first ob
                qcdr := rest ob
                nob  := [qcar,:qcdr]
                HPUT($seen, ob, nob)
                HPUT($seen, nob, nob)
                nob.first := dewritifyInner qcar
                nob.rest := dewritifyInner qcdr
                nob
            vector? ob =>
                n   := QVMAXINDEX ob
                nob := newVector(n+1)
                HPUT($seen, ob, nob)
                HPUT($seen, nob, nob)
                for i in 0..n repeat
                    vectorRef(nob,i) := dewritifyInner vectorRef(ob,i)
                nob
            -- Default case: return the object itself.
            ob




--% )load

load args == loadSpad2Cmd args

loadSpad2Cmd args ==
    sayKeyedMsg("S2IU0003", nil)
    NIL
--  load1(args,$forceDatabaseUpdate)

--load1(args,$forceDatabaseUpdate) ==  -- $ var is now local
--  null args => helpSpad2Cmd '(load)
--  loadfun := 'loadLib
--  justWondering := nil
--  compiler := 'old
--  doExpose := true
--  $forceDatabaseUpdate := true  -- BMT request, 5/14/90
--  for [opt,:.] in $options repeat
--    fullopt := selectOptionLC(opt,
--      '(cond update query new noexpose noupdate),
--        'optionError)
--    fullopt = 'cond     => loadfun := 'loadLibIfNotLoaded
--    fullopt = 'query    => justWondering := true
--    fullopt = 'update   => $forceDatabaseUpdate := true
--    fullopt = 'noexpose => doExpose := false
--    fullopt = 'noupdate => $forceDatabaseUpdate := false
--  if $forceDatabaseUpdate then clearClams()
--  for lib in args repeat
--    lib := object2Identifier lib
--    justWondering =>
--      GETL(lib,'LOADED) => sayKeyedMsg("S2IZ0028",[lib])
--      sayKeyedMsg("S2IZ0029",[lib])
--    null getConstructorModuleFromDB lib and
--     null (lib := getConstructorFullNameFromDB lib) =>
--      sayKeyedMsg("S2IL0020", [namestring [lib,$spadLibFT,"*"]])
--    null FUNCALL(loadfun,lib) =>
--      sayKeyedMsg("S2IZ0029",[lib])
--    sayKeyedMsg("S2IZ0028",[lib])
--    if doExpose and
--       not isExposedConstructor(lib) then
--          setExposeAddConstr([lib])
--  'EndOfLoad

reportCount () ==
  centerAndHighlight(" Current Count Settings ",$LINELENGTH,specialChar 'hbar)
  SAY " "
  sayBrightly [:bright " cache",fillerSpaces(30,'".")," ",$cacheCount]
  if $cacheAlist then
    for [a,:b] in $cacheAlist repeat
      aPart:= linearFormatName a
      n:= sayBrightlyLength aPart
      sayBrightly concat("     ",aPart," ",fillerSpaces(32-n,'".")," ",b)
  SAY " "
  sayBrightly [:bright " stream",fillerSpaces(29,'".")," ",$streamCount]

--% )library
library args ==
  origDir := GET_-CURRENT_-DIRECTORY()
  $newConlist: local := nil
  -- Users typically specify abbreviations without quotes.  
  LOCALDATABASE([STRING a for a in args],$options)
  extendLocalLibdb $newConlist
  changeDirectory origDir
  TERSYSCOMMAND()

--% )quit

pquit() == pquitSpad2Cmd()

pquitSpad2Cmd() ==
  $quitCommandType :local := 'protected
  quitSpad2Cmd()

quit() == quitSpad2Cmd()

quitSpad2Cmd() ==
  $quitCommandType ~= 'protected => leaveScratchpad()
  x := UPCASE queryUserKeyedMsg("S2IZ0031",NIL)
  STRING2ID_-N(x,1) in '(Y YES) => leaveScratchpad()
  sayKeyedMsg("S2IZ0032",NIL)
  TERSYSCOMMAND ()

leaveScratchpad () ==
  coreQuit() -- ??? should be coreQuit errorCount()

--% )read

read l == readSpad2Cmd l

readSpad2Cmd l ==
  $InteractiveMode : local := true
  quiet := nil
  ifthere := nil
  for [opt,:.] in $options repeat
    fullopt := selectOptionLC(opt,'(quiet test ifthere),'optionError)
    fullopt = 'ifthere => ifthere  := true
    fullopt = 'quiet   => quiet := true

  ef := pathname _/EDITFILE
  if pathnameTypeId(ef) = 'SPAD then
    ef := makePathname(pathnameName ef,'"*",'"*")
  if l then
    l := mergePathnames(pathname l,ef)
  else
    l := ef
  devFTs := '("input" "INPUT" "boot" "BOOT" "lisp" "LISP")
  fileTypes :=
    $UserLevel = 'interpreter => '("input" "INPUT")
    $UserLevel = 'compiler    => '("input" "INPUT")
    devFTs
  ll := $FINDFILE (l, fileTypes)
  if null ll then
    ifthere => return nil    -- be quiet about it
    throwKeyedMsg("S2IL0003",[namestring l])
  ll := pathname ll
  ft := pathnameType ll
  upft := UPCASE ft
  null member(upft,fileTypes) =>
    fs := namestring l
    member(upft,devFTs) => throwKeyedMsg("S2IZ0033",[fs])
    throwKeyedMsg("S2IZ0034",[fs])
  SETQ(_/EDITFILE,ll)
  if upft = '"BOOT" then $InteractiveMode := nil
  _/READ(ll,quiet)

--% )savesystem
savesystem l ==
  #l ~= 1 or not(symbol? first l) => helpSpad2Cmd '(savesystem)
  SETQ($SpadServer,false)
  SETQ($openServerIfTrue,true)
)if not %hasFeature KEYWORD::ECL
  AxiomCore::saveCore symbolName first l
)else
  fatalError '"don't know how to save image"
)endif

--% )show

show l == showSpad2Cmd l

showSpad2Cmd l ==
  l = [NIL] => helpSpad2Cmd '(show)
  $showOptions : local := '(attributes operations)
  if null $options then $options := '((operations))
  $e : local := $InteractiveFrame
  $env : local := $InteractiveFrame
  l is [constr] =>
    constr in '(Union Record Mapping) =>
      constr = 'Record =>
        sayKeyedMsg("S2IZ0044R",[constr, '")show Record(a: Integer, b: String)"])
      constr = 'Mapping =>
        sayKeyedMsg("S2IZ0044M",NIL)
      sayKeyedMsg("S2IZ0045T",[constr, '")show Union(a: Integer, b: String)"])
      sayKeyedMsg("S2IZ0045U",[constr, '")show Union(Integer, String)"])
    constr is ['Mapping, :.] =>
      sayKeyedMsg("S2IZ0044M",NIL)
    reportOperations(constr,constr)
  reportOperations(l,l)

reportOperations(oldArg,u) ==
  -- u might be an uppercased version of oldArg
  $env:local := [[nil]]
  $eval:local := true           --generate code-- don't just type analyze
  $genValue:local := true       --evaluate all generated code
  null u => nil
  $doNotAddEmptyModeIfTrue: local:= true
  u = $quadSymbol =>
     sayBrightly ['"   mode denotes", :bright '"any", "type"]
  u = "%" =>
    sayKeyedMsg("S2IZ0063",NIL)
    sayKeyedMsg("S2IZ0064",NIL)
  u isnt ['Record,:.] and u isnt ['Union,:.] and
    null(isNameOfType u) and u isnt ['typeOf,.] =>
      if atom oldArg then oldArg := [oldArg]
      sayKeyedMsg("S2IZ0063",NIL)
      for op in oldArg repeat
        sayKeyedMsg("S2IZ0062",[opOf op])
  (v := isDomainValuedVariable u) =>  reportOpsFromUnitDirectly0 v
  unitForm:=
    atom u => opOf unabbrev u
    unabbrev u
  atom unitForm => reportOpsFromLisplib0(unitForm,u)
  unitForm' := evaluateType unitForm
  tree := mkAtree removeZeroOneDestructively unitForm
  (unitForm' := isType tree) => reportOpsFromUnitDirectly0 unitForm'
  sayKeyedMsg("S2IZ0041",[unitForm])

reportOpsFromUnitDirectly0 D ==
  $useEditorForShowOutput =>
    reportOpsFromUnitDirectly1 D
  reportOpsFromUnitDirectly D

reportOpsFromUnitDirectly1 D ==
  showFile := pathname ['SHOW,'LISTING,$listingDirectory]
  _$ERASE showFile
  $sayBrightlyStream : fluid :=
    DEFIOSTREAM([['FILE,:showFile], '(MODE . OUTPUT)],255,0)
  sayShowWarning()
  reportOpsFromUnitDirectly D
  SHUT $sayBrightlyStream
  editFile showFile

sayShowWarning() ==
  sayBrightly
    '"Warning: this is a temporary file and will be deleted the next"
  sayBrightly
    '"         time you use )show. Rename it and FILE if you wish to"
  sayBrightly
    '"         save the contents."
  sayBrightly '""

reportOpsFromLisplib0(unitForm,u)  ==
  $useEditorForShowOutput => reportOpsFromLisplib1(unitForm,u)
  reportOpsFromLisplib(unitForm,u)

reportOpsFromLisplib1(unitForm,u)  ==
  showFile := pathname ['SHOW,'LISTING,$listingDirectory]
  _$ERASE showFile
  $sayBrightlyStream : fluid :=
    DEFIOSTREAM([['FILE,:showFile], '(MODE . OUTPUT)],255,0)
  sayShowWarning()
  reportOpsFromLisplib(unitForm,u)
  SHUT $sayBrightlyStream
  editFile showFile

reportOpsFromUnitDirectly unitForm ==
  isRecordOrUnion := unitForm is [a,:.] and a in $DomainNames
  unit:= evalDomain unitForm
  top:= first unitForm
  kind:= getConstructorKindFromDB top

  sayBrightly concat('"%b",formatOpType unitForm,
    '"%d",'"is a",'"%b",kind,'"%d", '"constructor.")
  if not isRecordOrUnion then
    abb := getConstructorAbbreviationFromDB top
    sourceFile := getConstructorSourceFileFromDB top
    sayBrightly ['" Abbreviation for",:bright top,'"is",:bright abb]
    verb :=
      isExposedConstructor top => '"is"
      '"is not"
    sayBrightly ['" This constructor",:bright verb,
      '"exposed in this frame."]
    sayBrightly ['" Issue",:bright strconc('")edit ",
      namestring sourceFile),'"to see algebra source code for",
        :bright abb,'"%l"]

  for [opt] in $options repeat
    opt := selectOptionLC(opt,$showOptions,'optionError)
    opt = 'attributes =>
      centerAndHighlight('"Attributes",$LINELENGTH,specialChar 'hbar)
      isRecordOrUnion =>
        sayBrightly '"   Records and Unions have no attributes."
      sayBrightly '""
      attList:= removeDuplicates MSORT [x for [x,:.] in unit.2]
      say2PerLine [formatAttribute x for x in attList]
      NIL
    opt = 'operations =>
      $commentedOps: local := 0
      --new form is (<op> <signature> <slotNumber> <condition> <kind>)
      centerAndHighlight('"Operations",$LINELENGTH,specialChar 'hbar)
      sayBrightly '""
      if isRecordOrUnion
        then
          constructorFunction:= GETL(top,"makeFunctionList") or
            systemErrorHere ["reportOpsFromUnitDirectly",top]
          [funlist,.]:= FUNCALL(constructorFunction,"$",unitForm,
            $CategoryFrame)
          sigList := removeDuplicates MSORT
                      [[[a,b],true,slot c] for [a,b,c] in funlist]
                             where slot c == (atom c => [c,0,1]; c)
        else
          sigList:= removeDuplicates MSORT getOplistForConstructorForm unitForm
      say2PerLine [formatOperation(x,unit) for x in sigList]
      if $commentedOps ~= 0 then
        sayBrightly
          ['"Functions that are not yet implemented are preceded by",
            :bright '"--"]
      sayBrightly '""
  NIL

reportOpsFromLisplib(op,u) ==
  null(fn:= constructor? op) => sayKeyedMsg("S2IZ0054",[u])
  argml :=
    (s := getConstructorSignature op) => KDR s
    NIL
  typ:= getConstructorKindFromDB op
  nArgs:= #argml
  argList:= KDR getConstructorFormFromDB op
  functorForm:= [op,:argList]
  argml:= EQSUBSTLIST(argList,$FormalMapVariableList,argml)
  functorFormWithDecl:= [op,:[[":",a,m] for a in argList for m in argml]]
  sayBrightly concat(bright form2StringWithWhere functorFormWithDecl,
                     '" is a",bright typ,'"constructor")
  sayBrightly ['" Abbreviation for",:bright op,'"is",:bright fn]
  verb :=
    isExposedConstructor op => '"is"
    '"is not"
  sayBrightly ['" This constructor",:bright verb,
    '"exposed in this frame."]
  sourceFile := getConstructorSourceFileFromDB op
  sayBrightly ['" Issue",:bright strconc('")edit ",
    namestring sourceFile),
      '"to see algebra source code for",:bright fn,'"%l"]

  for [opt] in $options repeat
    opt := selectOptionLC(opt,$showOptions,'optionError)
    opt = 'layout =>
      dc1 fn
    opt = 'views => sayBrightly ['"To get",:bright '"views",
      '"you must give parameters of constructor"]
    opt = 'attributes =>
      centerAndHighlight('"Attributes",$LINELENGTH,specialChar 'hbar)
      sayBrightly '""
      attList:= removeDuplicates MSORT [x for [x,:.] in
        getConstructorAttributesFromDB op]
      null attList => sayBrightly
        concat('"%b",form2String functorForm,'"%d","has no attributes.",'"%l")
      say2PerLine [formatAttribute x for x in attList]
      NIL
    opt = 'operations => displayOperationsFromLisplib functorForm
    nil

displayOperationsFromLisplib form ==
  [name,:argl] := form
  kind := getConstructorKindFromDB name
  centerAndHighlight('"Operations",$LINELENGTH,specialChar 'hbar)
  opList:= getConstructorOperationsFromDB name
  null opList => 
    centerAndHighlight('"No exported operations",$LINELENGTH)
  opl:=removeDuplicates MSORT EQSUBSTLIST(argl,$FormalMapVariableList,opList)
  ops:= nil
  for x in opl repeat
    ops := [:ops,:formatOperationAlistEntry(x)]
  say2PerLine ops
  nil

--% )spool

clearHighlight() ==
  $saveHighlight := $highlightAllowed
  $highlightAllowed := false
  $saveSpecialchars := $specialCharacters
  setOutputCharacters ["plain"]

resetHighlight() ==
  $highlightAllowed := $saveHighlight
  $specialCharacters := $saveSpecialchars

spool filename ==
  -- Note: The base Lisp system may change the value of the standard
  -- output stream as part of executing DRIBBLE(), so one must
  -- ensure that traces are still sent to the spool.
  null filename =>
    DRIBBLE()
    SETQ(_*TRACE_-OUTPUT_*,_*STANDARD_-OUTPUT_*)
    TERPRI()
    resetHighlight()
  PROBE_-FILE STRING first filename =>
    systemError strconc('"file ", STRING first filename, '" already exists")
  DRIBBLE STRING first filename
  SETQ(_*TRACE_-OUTPUT_*,_*STANDARD_-OUTPUT_*)
  TERPRI()
  clearHighlight()

--% )synonym

synonym(:l) == synonymSpad2Cmd()  -- always passed a null list

synonymSpad2Cmd() ==
  line := getSystemCommandLine()
  if line = '"" then printSynonyms(NIL)
  else
    pair := processSynonymLine line
    if $CommandSynonymAlist then
      PUTALIST($CommandSynonymAlist,first pair, rest pair)
    else $CommandSynonymAlist := [pair]
  terminateSystemCommand()

processSynonymLine line ==
  key := STRING2ID_-N (line, 1)
  value := removeKeyFromLine line where
    removeKeyFromLine line ==
      line := dropLeadingBlanks line
      mx := MAXINDEX line
      for i in 0..mx repeat
        line.i = " " =>
          return (for j in (i+1)..mx repeat
            line.j ~= " " => return (subString(line, j)))
  [key, :value]


--%
--% )undo
--%

$undoFlag := true     --Default setting for undo is "on"

++ true means means we report the steps undo takes
$reportUndo := false


undo(l) ==
--undo takes one option ")redo" which simply reads "redo.input",
--  a file created by every normal )undo command (see below)
  undoWhen := 'after
  if $options is [[key]] then
    stringPrefix?(s := PNAME key,'"redo") =>
      $options := nil           --clear $options so that "read" won't see them
      read '(redo_.input)
    not stringPrefix?(s,'"before") =>
       userError '"only option to undo is _")redo_""
    undoWhen := 'before
  n :=
    null l => -1
    first l
  if IDENTP n then
    n := readInteger PNAME n
    if not integer? n then userError '"undo argument must be an integer"
  $InteractiveFrame := undoSteps(undoCount n,undoWhen)
  nil

recordFrame(systemNormal) ==
  null $undoFlag => nil        --do nothing if facility is turned off
  currentAlist := KAR $frameRecord
  delta := diffAlist(CAAR $InteractiveFrame,$previousBindings)
  if systemNormal = 'system then
    null delta => return nil     --do not record
    delta := ['systemCommand,:delta]
  $frameRecord := [delta,:$frameRecord]
  $previousBindings := --copy all but the individual properties
    [[first x,:[[first y,:rest y] for y in rest x]] for x in CAAR $InteractiveFrame]
  first $frameRecord

diffAlist(new,old) ==
--record only those properties which are different
  for (pair := [name,:proplist]) in new repeat
    -- name has an entry both in new and old world
    -- (1) if the old world had no proplist for that variable, then
    --     record NIL as the value of each new property
    -- (2) if the old world does have a proplist for that variable, then
    --     a) for each property with a value: give the old value
    --     b) for each property missing:      give NIL as the old value
    oldPair := ASSQ(name,old) =>
      null (oldProplist := rest oldPair) =>
      --record old values of new properties as NIL
        acc := [[name,:[[prop] for [prop,:.] in proplist]],:acc]
      deltas := nil
      for (propval := [prop,:val]) in proplist repeat
        null (oldPropval := assoc(prop,oldProplist)) => --missing property
          deltas := [[prop],:deltas]
        EQ(rest oldPropval,val) => 'skip
        deltas := [oldPropval,:deltas]
      deltas => acc := [[name,:nreverse deltas],:acc]
    acc := [[name,:[[prop] for [prop,:.] in proplist]],:acc]
--record properties absent on new list (say, from a )cl all)
  for (oldPair := [name,:r]) in old repeat
    r and null LASSQ(name,new) =>
      acc := [oldPair,:acc]
    -- name has an entry both in new and old world
    -- (1) if the new world has no proplist for that variable
    --     (a) if the old world does, record the old proplist
    --     (b) if the old world does not, record nothing
    -- (2) if the new world has a proplist for that variable, it has
    --     been handled by the first loop.
  res := nreverse acc
  if $reportUndo then reportUndo res
  res

reportUndo acc ==
  for [name,:proplist] in acc repeat
    sayBrightly strconc('"Properties of ",PNAME name,'" ::")
    curproplist := LASSOC(name,CAAR $InteractiveFrame)
    for [prop,:value] in proplist repeat
      sayBrightlyNT ['"  ",prop,'" was: "]
      pp value
      sayBrightlyNT ['"  ",prop,'" is:  "]
      pp LASSOC(prop,curproplist)

clearFrame() ==
  clearCmdAll()
  $frameRecord := nil
  $previousBindings := nil


--=======================================================================
--              Undoing previous m commands
--=======================================================================
undoCount(n) ==  --computes the number of undo's, given $IOindex
--pp ["IOindex = ",$IOindex]
  m :=
    n >= 0 => $IOindex - n - 1
    -n
  m >= $IOindex => userError strconc('"Magnitude of undo argument must be less than step number (",STRINGIMAGE $IOindex,'").")
  m


undoSteps(m,beforeOrAfter) ==
-- undoes m previous commands; if )before option, then undo one extra at end
--Example: if $IOindex now is 6 and m = 2 then general layout of $frameRecord,
--  after the call to recordFrame below will be:
--  (<change for systemcommands>
--  (<change for #5> <change for system commands>
--  (<change for #4> <change for system commands>
--  (<change for #3> <change for system commands>
--   <change for #2> <change for system commands>
--   <change for #1> <change for system commands>) where system
--  command entries are optional and identified by (systemCommand . change).
--  For a ")undo 3 )after", m = 2 and undoStep swill restore the environment
--  up to, but not including <change for #3>.
--  An "undo 3 )before" will additionally restore <change for #3>.
--  Thus, the later requires one extra undo at the end.
  writeInputLines('redo,$IOindex - m)
  recordFrame('normal)  --do NOT mark this as a system command change
                       --do this undo FIRST (i=0 case)
  env := COPY CAAR $InteractiveFrame
  for i in 0..m for framelist in tails $frameRecord repeat
    env := undoSingleStep(first framelist,env)
    framelist is [.,['systemCommand,:systemDelta],:.] =>
--     pp '"===============> AHA <============="
       framelist := rest framelist             --undoing system commands given
       env := undoSingleStep(systemDelta,env)  --  before command line
    lastTailSeen := framelist
  if beforeOrAfter = 'before then  --do one additional undo for )before
    env := undoSingleStep(second lastTailSeen,env)
  $frameRecord := rest $frameRecord --flush the effect of extra recordFrame
  $InteractiveFrame := LIST LIST env


undoSingleStep(changes,env) ==
--Each change is a name-proplist pair. For each change:
--  (1) if there exists a proplist in env, then for each prop-value change:
--      (a) if the prop exists in env, RPLAC in the change value
--      (b) otherwise, CONS it onto the front of prop-values for that name
--  (2) add change to the front of env
--  pp '"----Undoing 1 step--------"
--  pp changes
  for (change := [name,:changeList]) in changes repeat
    if LASSOC('localModemap,changeList) then
      changeList := undoLocalModemapHack changeList
    pairlist := ASSQ(name,env) =>
      proplist := rest pairlist =>
        for (pair := [prop,:value]) in changeList repeat
          node := ASSQ(prop,proplist) => node.rest := value
          proplist.rest := [first proplist,:rest proplist]
          proplist.first := pair
      pairlist.rest := changeList
    env := [change,:env]
  env

undoLocalModemapHack changeList ==
  [newPair for (pair := [name,:value]) in changeList | newPair] where newPair()  ==
     name = 'localModemap => [name]
     pair

removeUndoLines u == --called by writeInputLines
  xtra :=
    string? $currentLine => [$currentLine]
    reverse $currentLine
  xtra := [x for x in xtra | not stringPrefix?('")history",x)]
  u := [:u, :xtra]
  not (or/[stringPrefix?('")undo",x) for x in u])  => u
  --(1) reverse the list
  --(2) walk down the (reversed) list: when >n appears remove:
  --    (a) system commands
  --    (b) if n > 0: (replace n by n-1; remove a command; repeat (a-b))
  savedIOindex := $IOindex  --save value
  $IOindex := 1
  for y in tails u repeat
    (x := first y).0 = char '_) =>
      stringPrefix?('")undo",s := trimString x) => --parse "undo )option"
        s1 := trimString subString(s,5)
        if s1 ~= '")redo" then
          m := charPosition(char '_),s1,0)
          code :=
            m < MAXINDEX s1 => s1.(m + 1)
            char 'a
          s2 := trimString subString(s1,0,m)
        n :=
           s1 = '")redo" => 0
           s2 ~= '"" => undoCount readInteger s2
           -1
        y.first := strconc('">",code,STRINGIMAGE n)
      nil
    $IOindex := $IOindex + 1   --referenced by undoCount
  acc := nil
  for y in tails nreverse u repeat
    (x := first y).0 = char '_> =>
      code := x . 1                                 --code = a,b, or r
      n := readInteger subString(x,2)               --n = number of undo steps
      y := rest y                                   --kill >n line
      while y repeat
        c := first y
        c.0 = char '_) or c.0 = char '_> => y := rest y  --kill system commands
        n = 0 => return nil                              --including undos
        n := n - 1
        y := rest y                                 --kill command
      y and code ~= char 'b => acc := [c,:acc]       --add last unless )before
    acc := [x,:acc]
  $IOindex := savedIOindex
  acc




--% )what


what l == whatSpad2Cmd l

whatSpad2Cmd l ==
  $e:local := $EmptyEnvironment
  null l => reportWhatOptions()
  [key0,:args] := l
  key := selectOptionLC(key0,$whatOptions,nil)
  null key => sayKeyedMsg("S2IZ0043",NIL)
  args := [fixpat p for p in args] where
    fixpat x ==
      x is [x',:.] => DOWNCASE x'
      DOWNCASE x
  key = 'things =>
    for opt in $whatOptions repeat
      not (opt in '(things)) => whatSpad2Cmd [opt,:args]
  key = 'categories =>
    filterAndFormatConstructors('category,'"Categories",args)
  key = 'commands =>
    whatCommands(args)
  key = 'domains =>
    filterAndFormatConstructors('domain,'"Domains",args)
  key = 'operations =>
    apropos args
  key = 'packages =>
    filterAndFormatConstructors('package,'"Packages",args)
  key = 'synonyms =>
    printSynonyms(args)

filterAndFormatConstructors(constrType,label,patterns) ==
  centerAndHighlight(label,$LINELENGTH,specialChar 'hbar)
  l := filterListOfStringsWithFn(patterns,whatConstructors constrType,
        function rest)
  if patterns then
    null l =>
      sayMessage ['"   No ",label,'" with names matching patterns:",
        '"%l",'"   ",'"%b",:blankList patterns,'"%d"]
    sayMessage [label,'" with names matching patterns:",
      '"%l",'"   ",'"%b",:blankList patterns,'"%d"]
  l => pp2Cols l

whatConstructors constrType ==
  -- here constrType should be one of 'category, 'domain, 'package
  MSORT [[getConstructorAbbreviationFromDB con, :STRING(con)]
    for con in allConstructors()
      | getConstructorKindFromDB con = constrType]

apropos l ==
  -- l is a list of operation name fragments
  -- this displays all operation names containing these fragments
  ops :=
    null l => allOperations()
    filterListOfStrings([(DOWNCASE STRINGIMAGE p) for p in l],allOperations())
  ops =>
    sayMessage '"Operations whose names satisfy the above pattern(s):"
    sayAsManyPerLineAsPossible MSORT ops
    sayKeyedMsg("S2IF0011",[first ops])
  sayMessage '"   There are no operations containing those patterns"
  NIL


printSynonyms(patterns) ==
  centerAndHighlight("System Command Synonyms",$LINELENGTH,specialChar 'hbar)
  ls := filterListOfStringsWithFn(patterns, [[STRINGIMAGE a,:eval b]
    for [a,:b] in synonymsForUserLevel $CommandSynonymAlist],
      function first)
  printLabelledList(ls,'"user",'"synonyms",'")",patterns)
  nil

printLabelledList(ls,label1,label2,prefix,patterns) ==
  -- prefix goes before each element on each side of the list, eg,
  --   ")"
  null ls =>
    null patterns =>
      sayMessage ['"   No ",label1,'"-defined ",label2,'" in effect."]
    sayMessage ['"   No ",label1,'"-defined ",label2,'" satisfying patterns:",
     '"%l",'"     ",'"%b",:blankList patterns,'"%d"]
  if patterns then
    sayMessage [label1,'"-defined ",label2,'" satisfying patterns:",
     '"%l",'"   ",'"%b",:blankList patterns,'"%d"]
  for [syn,:comm] in ls repeat
    if subString(syn,0,1) = '"|" then syn := subString(syn,1,NIL)
    if syn = '"%i" then syn := '"%i "
    wid := MAX(30 - (entryWidth syn),1)
    sayBrightly concat('"%b",prefix,syn,'"%d",
      fillerSpaces(wid,'"."),'" ",prefix,comm)
  sayBrightly '""

whatCommands(patterns) ==
  label := strconc('"System Commands for User Level: ",
    STRINGIMAGE $UserLevel)
  centerAndHighlight(label,$LINELENGTH,specialChar 'hbar)
  l := filterListOfStrings(patterns,
    [(STRINGIMAGE a) for a in commandsForUserLevel $systemCommands])
  if patterns then
    null l =>
      sayMessage ['"No system commands at this level matching patterns:",
        '"%l",'"   ",'"%b",:blankList patterns,'"%d"]
    sayMessage ['"System commands at this level matching patterns:",
      '"%l",'"   ",'"%b",:blankList patterns,'"%d"]
  if l then
    sayAsManyPerLineAsPossible l
    SAY " "
  patterns => nil  -- don't be so verbose
  sayKeyedMsg("S2IZ0046",NIL)
  nil

reportWhatOptions() ==
  optList1:= "append"/[['"%l",'"        ",x] for x in $whatOptions]
  sayBrightly
    ['"%b",'"  )what",'"%d",'"argument keywords are",'"%b",:optList1,'"%d",'"%l",
      '"   or abbreviations thereof.",'"%l",
        '"%l",'"   Issue",'"%b",'")what ?",'"%d",'"for more information."]

filterListOfStrings(patterns,names) ==
  -- names and patterns are lists of strings
  -- returns: list of strings in names that contains any of the strings
  -- in patterns
  (null patterns) or (null names) => names
  names' := NIL
  for name in reverse names repeat
    satisfiesRegularExpressions(name,patterns) =>
      names' := [name,:names']
  names'

filterListOfStringsWithFn(patterns,names,fn) ==
  -- names and patterns are lists of strings
  -- fn is something like first or second
  -- returns: list of strings in names that contains any of the strings
  -- in patterns
  (null patterns) or (null names) => names
  names' := NIL
  for name in reverse names repeat
    satisfiesRegularExpressions(FUNCALL(fn,name),patterns) =>
      names' := [name,:names']
  names'

satisfiesRegularExpressions(name,patterns) ==
  -- this is a first cut
  nf := true
  dname := DOWNCASE COPY name
  for pattern in patterns while nf repeat
    -- use @ as a wildcard
    STRPOS(pattern,dname,0,'"@") => nf := nil
  null nf

--% )with ... defined in daase.lisp (boot won't parse it)

--% )workfiles

workfiles l == workfilesSpad2Cmd l

workfilesSpad2Cmd args ==
  args => throwKeyedMsg("S2IZ0047",NIL)
  deleteFlag := nil
  for [type,:.] in $options repeat
    type1 := selectOptionLC(type,'(boot lisp meta delete),nil)
    null type1 => throwKeyedMsg("S2IZ0048",[type])
    type1 = 'delete => deleteFlag := true
  for [type,:flist] in $options repeat
    type1 := selectOptionLC(type,'(boot lisp meta delete),nil)
    type1 = 'delete => nil
    for file in flist repeat
      fl := pathname [file,type1,'"*"]
      deleteFlag => SETQ($sourceFiles,delete(fl,$sourceFiles))
      null (MAKE_-INPUT_-FILENAME fl) => sayKeyedMsg("S2IZ0035",[namestring fl])
      updateSourceFiles fl
  SAY " "
  centerAndHighlight(" User-specified work files ",$LINELENGTH,specialChar 'hbar)
  SAY " "
  null $sourceFiles => SAY '"   no files specified"
  SETQ($sourceFiles,SORTBY('pathnameType,$sourceFiles))
  for fl in $sourceFiles repeat sayBrightly ["   " ,namestring fl]

--% )zsystemdevelopment

zsystemdevelopment l == zsystemDevelopmentSpad2Cmd l

zsystemDevelopmentSpad2Cmd l == zsystemdevelopment1 (l,$InteractiveMode)

zsystemdevelopment1(l,im) ==
  $InteractiveMode : local := im
  fromopt := nil
  -- cycle through once to see if )from is mentioned
  for [opt,:optargs] in $options repeat
    opt1 := selectOptionLC(opt,'(from),nil)
    opt1 = 'from => fromopt := [['FROM,:optargs]]
  for [opt,:optargs] in $options repeat
    if null optargs then optargs := l
    newopt := append(optargs,fromopt)
    opt1 := selectOptionLC(opt,'(from),nil)
    opt1 = 'from => nil
    opt = "c"   => _/D_,1 (newopt ,_/COMP(),NIL,NIL)
    opt = "d"   => _/D_,1 (newopt ,'DEFINE,NIL,NIL)
    opt = "dt"  => _/D_,1 (newopt ,'DEFINE,NIL,true)
    opt = "ct"  => _/D_,1 (newopt ,_/COMP(),NIL,true)
    opt = "ctl"  => _/D_,1 (newopt ,_/COMP(),NIL,'TRACELET)
    opt = "ec"  => _/D_,1 (newopt ,_/COMP(),true,NIL)
    opt = "ect" => _/D_,1 (newopt ,_/COMP(),true,true)
    opt = "e"   => _/D_,1 (newopt ,NIL,true,NIL)
    opt = "version" => version()
    opt = "pause" =>
      conStream := DEFIOSTREAM ('((DEVICE . CONSOLE) (QUAL . V)),120,0)
      NEXT conStream
      SHUT conStream
    opt = "update" or opt = "patch" =>
      $InteractiveMode := nil
      upf := [KAR optargs or _/VERSION, KADR optargs or _/WSNAME,
              KADDR optargs or '_*]
      fun := (opt = "patch" => '_/UPDATE_-LIB_-1; '_/UPDATE_-1)
      CATCH('FILENAM, FUNCALL(fun, upf))
      sayMessage '"   Update/patch is completed."
    null optargs =>
      sayBrightly ['"   An argument is required for",:bright opt]
    sayMessage ['"   Unknown option:",:bright opt,"    ",'"%l",
      '"   Available options are", _
      :bright '"c ct e ec ect cls pause update patch compare record"]

--% Synonym File Reader

processSynonyms() ==
  p := STRPOS('")",LINE,0,NIL)
  fill := '""
  if p
    then
      line := subString(LINE,p)
      if p > 0 then fill := subString(LINE,0,p)
    else
      p := 0
      line := LINE
  to := STRPOS ('" ", line, 1, nil)
  if to then to := to - 1
  synstr := subString(line, 1, to)
  syn := STRING2ID_-N (synstr, 1)
  null (fun := LASSOC (syn, $CommandSynonymAlist)) => NIL
  fun := eval fun              -- fun may have been a suspension
  to := STRPOS('")",fun,1,NIL)
  if to and to ~= #(fun)-1 then
    opt := strconc('" ",subString(fun,to))
    fun := subString(fun,0,to-1)
  else opt := '" "
  if # synstr > # fun then
    for i in (# fun)..(# synstr) repeat
      fun := strconc (fun, '" ")
--  $currentLine := strconc(fill,RPLACSTR(line, 1, # synstr, fun),opt)
  cl := strconc(fill,RPLACSTR(line, 1, # synstr, fun),opt)
  SETQ(LINE,cl)
  SETQ(CHR,LINE.(p+1))
  processSynonyms ()

-- functions for interfacing to system commands from algebra code
-- common lisp dependent

tabsToBlanks s ==
   k := charPosition($charTab,s,0)
   n := #s
   k < n =>
      k = 0 => tabsToBlanks subString(s,1)
      strconc(subString(s,0,k),$charBlank, tabsToBlanks subString(s,k + 1))
   s

doSystemCommand string ==
   string := strconc('")", EXPAND_-TABS string)
   LINE: fluid := string
   processSynonyms()
   string := LINE
   string:=subString(string,1)
   string = '"" => nil
   tok:=getFirstWord(string)
   tok =>
        unab := unAbbreviateKeyword tok
        member(unab, $noParseCommands) =>
          handleNoParseCommands(unab, string)
        optionList := splitIntoOptionBlocks string
        member(unab, $tokenCommands) =>
          handleTokensizeSystemCommands(unab, optionList)
        handleParsedSystemCommands(unab, optionList)
        nil
   nil

handleNoParseCommands(unab, string) ==
  string := stripSpaces string
  spaceIndex := SEARCH('" ", string)
  unab = "lisp" =>
    if (null spaceIndex) then
      sayKeyedMsg("S2IV0005", NIL)
      nil
    else nplisp(stripLisp string)
  unab = "boot" =>
    if (null spaceIndex) then
      sayKeyedMsg("S2IV0005", NIL)
      nil
    else npboot(subSequence(string, spaceIndex+1))
  unab = "system" =>
    if (null spaceIndex) then
      sayKeyedMsg("S2IV0005", NIL)
      nil
    else npsystem(unab, string)
  unab = "synonym" =>
    npsynonym(unab, (null spaceIndex => '""; subSequence(string, spaceIndex+1)))
  null spaceIndex =>
    FUNCALL unab
  unab in '( quit     _
             fin      _
             pquit    _
             credits  _
             copyright ) => 
    sayKeyedMsg("S2IV0005", NIL)
    nil
  funName := makeSymbol strconc('"np",STRING unab)
  FUNCALL(funName, subSequence(string, spaceIndex+1))


npboot str ==
  sex := string2BootTree str
  FORMAT(true, '"~&~S~%", sex)
  $ans := eval sex
  FORMAT(true, '"~&Value = ~S~%", $ans)

stripLisp str ==
  found := false
  strIndex := 0
  lispStr := '"lisp"
  for c0 in 0..#str-1 for c1 in 0..#lispStr-1 repeat
    (char str.c0) ~= (char lispStr.c1) =>
      return nil
    strIndex := c0+1
  subSequence(str, strIndex)


nplisp str ==
  $ans := eval READ_-FROM_-STRING str
  FORMAT(true, '"~&Value = ~S~%", $ans)

npsystem(unab, str) ==
  spaceIndex := SEARCH('" ", str)
  null spaceIndex =>
    sayKeyedMsg('"S2IZ0080", [str])
  sysPart := subSequence(str, 0, spaceIndex)
  -- The following is a hack required by the fact that unAbbreviateKeyword
  -- returns the word "system" for unknown words
  null SEARCH(sysPart, STRING unab) =>
    sayKeyedMsg('"S2IZ0080", [sysPart])
  command := subSequence(str, spaceIndex+1)
  runCommand command

npsynonym(unab, str) ==
  npProcessSynonym(str)

tokenSystemCommand(unabr, tokList) ==
  systemCommand tokList

tokTran tok ==
  string? tok =>
    #tok = 0 => nil
    isIntegerString tok => READ_-FROM_-STRING tok
    tok.0 = char "_"" =>
      subSequence(tok, 1, #tok-1)
    makeSymbol tok
  tok

isIntegerString tok ==
  for i in 0..#tok-1 repeat
    val := digit? tok.i
    not val => return nil
  val

splitIntoOptionBlocks str ==
  inString := false
  optionBlocks := nil
  blockStart := 0
  parenCount := 0
  for i in 0..#str-1 repeat
    str.i = char "_"" =>
      inString := not inString
    if str.i = char "(" and not inString
    then parenCount := parenCount + 1
    if str.i = char ")" and not inString
    then parenCount := parenCount - 1
    str.i = char ")" and not inString and parenCount = -1 =>
      block := stripSpaces subSequence(str, blockStart, i)
      blockList := [block, :blockList]
      blockStart := i+1
      parenCount := 0
  blockList := [stripSpaces subSequence(str, blockStart), :blockList]
  nreverse blockList

dumbTokenize str ==
  -- split into tokens delimted by spaces, taking quoted strings into account
  inString := false
  tokenList := nil
  tokenStart := 0
  previousSpace := false
  for i in 0..#str-1 repeat
    str.i = char "_"" =>
      inString := not inString
      previousSpace := false
    str.i = char " " and not inString =>
      previousSpace => nil
      token := stripSpaces subSequence(str, tokenStart, i)
      tokenList := [token, :tokenList]
      tokenStart := i+1
      previousSpace := true
    previousSpace := false
  tokenList := [stripSpaces subSequence(str, tokenStart), :tokenList]
  nreverse tokenList

handleParsedSystemCommands(unabr, optionList) ==
  restOptionList := [dumbTokenize opt for opt in rest optionList]
  parcmd := [parseSystemCmd first optionList,
             :[[tokTran tok for tok in opt] for opt in restOptionList]]
  systemCommand parcmd

parseSystemCmd opt ==
  spaceIndex := SEARCH('" ", opt)
  spaceIndex =>
    commandString := stripSpaces subSequence(opt, 0, spaceIndex)
    argString := stripSpaces subSequence(opt, spaceIndex)
    command := tokTran commandString
    pform := parseFromString argString
    [command, pform]
  [tokTran tok for tok in dumbTokenize opt]

parseFromString(s) ==
   s := next(function ncloopParse,
        next(function lineoftoks,incString s))
   StreamNull s => nil
   pf2Sex macroExpanded second first s

handleTokensizeSystemCommands(unabr, optionList) ==
  optionList := [dumbTokenize opt for opt in optionList]
  parcmd := [[tokTran tok for tok in opt] for opt in optionList]
  parcmd => tokenSystemCommand(unabr, parcmd)

getFirstWord string ==
  spaceIndex := SEARCH('" ", string)
  null spaceIndex => string
  stripSpaces subSequence(string, 0, spaceIndex)

ltrace l == trace l

stripSpaces str ==
  STRING_-TRIM([char '" "], str)

npProcessSynonym(str) ==
  if str = '"" then printSynonyms(NIL)
  else
    pair := processSynonymLine str
    if $CommandSynonymAlist then
      PUTALIST($CommandSynonymAlist,first pair, rest pair)
    else $CommandSynonymAlist := [pair]
  terminateSystemCommand()




