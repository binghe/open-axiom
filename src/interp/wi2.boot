-- Copyright (c) 1991-2002, The Numerical ALgorithms Group Ltd.
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
--     - Neither the name of The Numerical ALgorithms Group Ltd. nor the
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


import macros
import define
namespace BOOT

-- ??? turns off buggy code
$NRTopt := false

compDefineFunctor1(df, m,$e,$prefix,$formalArgList) ==
    ['DEF,form,signature,$functorSpecialCases,body] := df
    signature := markKillAll signature
--  1. bind global variables
    $addForm: local := nil
    $functionStats: local:= [0,0]
    $functorStats: local:= [0,0]
    $DEFdepth :    local  := 0            --for conversion to new compiler 3/93
    $capsuleStack : local := nil          --for conversion to new compiler 3/93
    $predicateStack:local := nil          --for conversion to new compiler 3/93
    $signatureStack:local := nil          --for conversion to new compiler 3/93
    $importStack  : local := nil          --for conversion to new compiler 3/93
    $globalImportStack  : local := nil    --for conversion to new compiler 3/93
    $globalDeclareStack : local := nil
    $globalImportDefAlist: local:= nil
    $localMacroStack  : local := nil      --for conversion to new compiler 3/93
    $freeStack   : local := nil           --for conversion to new compiler 3/93
    $domainLevelVariableList: local := nil--for conversion to new compiler 3/93
    $localLoopVariables: local := nil
    $pathStack : local := nil
    $form: local := nil
    $op: local := nil
    $signature: local := nil
    $functorTarget: local := nil
    $Representation: local := nil
         --Set in doIt, accessed in the compiler - compNoStacking
    $LocalDomainAlist: local  --set in doIt, accessed in genDeltaEntry
    $LocalDomainAlist:= nil
    $functorForm: local := nil
    $functorLocalParameters: local := nil
    $CheckVectorList: local := nil
                  --prevents CheckVector from printing out same message twice
    $getDomainCode: local -- code for getting views
    $insideFunctorIfTrue: local:= true
    $genSDVar: local:= 0
    originale:= $e
    [$op,:argl]:= form
    $formalArgList:= [:argl,:$formalArgList]
    $pairlis := pairList(argl,$FormalMapVariableList)
    $mutableDomain: local :=
      -- all defaulting packages should have caching turned off
       isCategoryPackageName $op or   
         (if $mutableDomains then MEMQ($op,$mutableDomains)
            else false )   --true if domain has mutable state
    signature':=
      [first signature,:[getArgumentModeOrMoan(a,form,$e) for a in argl]]
    $functorForm:= $form:= [$op,:argl]
    $globalImportStack := 
       [markKillAll x for x in rest $functorForm for typ in rest signature' 
           | getConstructorKindFromDB opOf typ = "category"]
    if null first signature' then signature':=
      modemap2Signature getModemap($form,$e)
    target:= first signature'
    $functorTarget:= target
    $e:= giveFormalParametersValues(argl,$e)
    [ds,.,$e]:= compMakeCategoryObject(target,$e) or
--+ copy needed since slot1 is reset; compMake.. can return a cached vector
      sayBrightly '"   cannot produce category object:"
      pp target
      return nil
    $domainShell:= COPY_-SEQ ds
    attributeList := ds.2 --see below under "loadTimeAlist"
--+ 7 lines for $NRT follow
-->--these globals used by NRTmakeCategoryAlist, set by NRTsetVector4Part1
    $condAlist: local := nil
    $uncondAlist: local := nil
-->>-- next global initialized here, reset by buildFunctor
    $NRTslot1PredicateList: local :=
      removeDuplicates [second x for x in attributeList]
-->>-- next global initialized here, used by NRTgenAttributeAlist (NRUNOPT)
    $NRTattributeAlist: local := NRTgenInitialAttributeAlist attributeList
    $NRTslot1Info: local := nil --set in NRTmakeSlot1Info
       --this is used below to set $lisplibSlot1 global
    $NRTaddForm: local := nil   -- see compAdd
    $NRTdeltaList: local := nil --list of misc. elts used in compiled fncts
    $NRTdeltaListComp: local := nil --list of compiled forms for $NRTdeltaList
    $NRTdeltaLength: local := 0 -- =length of block of extra entries in vector
    $template: local:= nil --stored in the lisplib
    $functionLocations: local := nil --locations of defined functions in source
    -- generate slots for arguments first, then for $NRTaddForm in compAdd
    for x in argl repeat NRTgetLocalIndex x
    [.,.,$e]:= compMakeDeclaration("$",target,$e)
    --The following loop sees if we can economise on ADDed operations
    --by using those of Rep, if that is the same. Example: DIRPROD
    if not $insideCategoryPackageIfTrue  then
      if body is ['add,ab:=[fn,:.],['CAPSULE,:cb]] and fn in '(List Vector)
         and FindRep(cb) = ab
               where FindRep cb ==
                 u:=
                   while cb repeat
                     atom cb => return nil
                     cb is [["%LET",'Rep,v,:.],:.] => return (u:=v)
                     cb:=rest cb
                 u
      then $e:= augModemapsFromCategoryRep('_$,ab,cb,target,$e)
      else $e:= augModemapsFromCategory('_$,'_$,'_$,target,$e)
    $signature:= signature'
    operationAlist:= SUBLIS($pairlis,$domainShell.1)
    parSignature:= SUBLIS($pairlis,signature')
    parForm:= SUBLIS($pairlis,form)
 
--  (3.1) now make a list of the functor's local parameters; for
--  domain D in argl,check its signature: if domain, its type is Join(A1,..,An);
--  in this case, D is replaced by D1,..,Dn (gensyms) which are set
--  to the A1,..,An view of D
--+
    $functorLocalParameters:=
      argPars :=
        makeFunctorArgumentParameters(argl,rest signature',first signature')
 -- must do above to bring categories into scope --see line 5 of genDomainView
      argl
--  4. compile body in environment of %type declarations for arguments
    op':= $op
    rettype:= signature'.target
    T:= compFunctorBody(body,rettype,$e,parForm)
---------------> new <---------------------
    $convert2NewCompiler => 
       return markFinish($originalBody,[$form,['Mapping,:signature'],T.env])
---------------> new <---------------------
    -- If only compiling certain items, then ignore the body shell.
    $compileOnlyCertainItems =>
       reportOnFunctorCompilation()
       [nil, ['Mapping, :signature'], originale]
 
    body':= T.expr
    lamOrSlam:= if $mutableDomain then 'LAM else 'SPADSLAM
    fun:= compile SUBLIS($pairlis, [op',[lamOrSlam,argl,body']])
    --The above statement stops substitutions gettting in one another's way
--+
    operationAlist := SUBLIS($pairlis,$lisplibOperationAlist)
    if $LISPLIB then
      augmentLisplibModemapsFromFunctor(parForm,operationAlist,parSignature)
    reportOnFunctorCompilation()
 
--  5. give operator a 'modemap property
    $insideFunctorIfTrue:= false
    if $LISPLIB then
      $lisplibKind:=
        $functorTarget is ["CATEGORY",key,:.] and key~="domain" => 'package
        'domain
      $lisplibForm:= form
      modemap:= [[parForm,:parSignature],[true,op']]
      $lisplibModemap:= modemap
      if null $bootStrapMode then
        $NRTslot1Info := NRTmakeSlot1Info()
        $isOpPackageName: local := isCategoryPackageName $op
        if $isOpPackageName then lisplibWrite('"slot1DataBase",
          ['updateSlot1DataBase,MKQ $NRTslot1Info],$libFile)
        $lisplibFunctionLocations := SUBLIS($pairlis,$functionLocations)
        libFn := getConstructorAbbreviation op'
        $lookupFunction: local :=
            NRTgetLookupFunction($functorForm,$lisplibModemap.mmTarget,$NRTaddForm)
            --either lookupComplete (for forgetful guys) or lookupIncomplete
        $byteAddress :local := 0
        $byteVec :local := nil
        $NRTslot1PredicateList :=
          [simpBool x for x in $NRTslot1PredicateList]
        rwriteLispForm('loadTimeStuff,
          ['MAKEPROP,MKQ $op,''infovec,getInfovecCode()])
      $lisplibSlot1 := $NRTslot1Info --NIL or set by $NRTmakeSlot1
      $lisplibOperationAlist:= operationAlist
    lisplibWrite('"compilerInfo",
       ['SETQ,'$CategoryFrame,
        ['put,['QUOTE,op'],'
         (QUOTE isFunctor),
          ['QUOTE,operationAlist],['addModemap,['QUOTE,op'],['
           QUOTE,parForm],['QUOTE,parSignature],true,['QUOTE,op'],
            ['put,['QUOTE,op' ],'(QUOTE mode),
             ['QUOTE,['Mapping,:parSignature]],'$CategoryFrame]]]],$libFile)
    if null argl then
      evalAndRwriteLispForm('NILADIC,
            ['MAKEPROP, ['QUOTE,op'], ['QUOTE,'NILADIC], true])
    [fun,['Mapping,:signature'],originale]

makeFunctorArgumentParameters(argl,sigl,target) ==
  $forceAdd: local:= true
  $ConditionalOperators: local
  target := markKillAll target
  ("append"/[fn(a,augmentSig(s,findExtras(a,target)))
              for a in argl for s in sigl]) where
    findExtras(a,target) ==
      --  see if conditional information implies anything else
      --  in the signature of a
      target is ['Join,:l] => "union"/[findExtras(a,x) for x in l]
      target is ['CATEGORY,.,:l] => "union"/[findExtras1(a,x) for x in l] where
        findExtras1(a,x) ==
          x is ['AND,:l] => "union"/[findExtras1(a,y) for y in l]
          x is ['OR,:l] => "union"/[findExtras1(a,y) for y in l]
          x is ['IF,c,p,q] =>
            union(findExtrasP(a,c),
                  union(findExtras1(a,p),findExtras1(a,q))) where
              findExtrasP(a,x) ==
                x is ['AND,:l] => "union"/[findExtrasP(a,y) for y in l]
                x is ['OR,:l] => "union"/[findExtrasP(a,y) for y in l]
                x is ["has",=a,y] and y is ['SIGNATURE,:.] => [y]
                nil
        nil
    augmentSig(s,ss) ==
       -- if we find something extra, add it to the signature
      null ss => s
      for u in ss repeat
        $ConditionalOperators:=[rest u,:$ConditionalOperators]
      s is ['Join,:sl] =>
        u:=ASSQ('CATEGORY,ss) =>
          SUBST([:u,:ss],u,s)
        ['Join,:sl,['CATEGORY,'package,:ss]]
      ['Join,s,['CATEGORY,'package,:ss]]
    fn(a,s) ==
      isCategoryForm(s,$CategoryFrame) =>
        s is ["Join",:catlist] => genDomainViewList(a,rest s)
        [genDomainView(a,a,s,"getDomainView")]
      [a]

compDefineCapsuleFunction(df,m,oldE,$prefix,$formalArgList) ==
    ['DEF,form,originalSignature,specialCases,body] := df
    signature := markKillAll originalSignature
    $markFreeStack: local := nil       --holds "free variables"
    $localImportStack : local := nil   --local import stack for function
    $localDeclareStack: local := nil   
    $localLoopVariables: local := nil
    originalDef := COPY df
    [lineNumber,:specialCases] := specialCases
    e := oldE
    --1. bind global variables
    $form: local := nil
    $op: local := nil
    $functionStats: local:= [0,0]
    $argumentConditionList: local
    $finalEnv: local := nil
             --used by ReplaceExitEtc to get a common environment
    $initCapsuleErrorCount: local:= #$semanticErrorStack
    $insideCapsuleFunctionIfTrue: local:= true
    $CapsuleModemapFrame: local:= e
    $CapsuleDomainsInScope: local:= get("$DomainsInScope","special",e)
    $insideExpressionIfTrue: local:= true
    $returnMode:= m
    [$op,:argl]:= form
    $form:= [$op,:argl]
    argl:= stripOffArgumentConditions argl
    $formalArgList:= [:argl,:$formalArgList]
 
    --let target and local signatures help determine modes of arguments
    argModeList:=
      identSig:= hasSigInTargetCategory(argl,form,first signature,e) =>
        (e:= checkAndDeclare(argl,form,identSig,e); rest identSig)
      [getArgumentModeOrMoan(a,form,e) for a in argl]
    argModeList:= stripOffSubdomainConditions(argModeList,argl)
    signature':= [first signature,:argModeList]
    if null identSig then  --make $op a local function
      oldE := put($op,'mode,['Mapping,:signature'],oldE)
 
    --obtain target type if not given
    if null first signature' then signature':=
      identSig => identSig
      getSignature($op,rest signature',e) or return nil
    e:= giveFormalParametersValues(argl,e)
 
    $signatureOfForm:= signature' --this global is bound in compCapsuleItems
    $functionLocations := [[[$op,$signatureOfForm],:lineNumber],
      :$functionLocations]
    e:= addDomain(first signature',e)
    e:= compArgumentConditions e
 
    if $profileCompiler then
      for x in argl for t in rest signature' repeat profileRecord('arguments,x,t)
 
 
    --4. introduce needed domains into extendedEnv
    for domain in signature' repeat e:= addDomain(domain,e)
 
    --6. compile body in environment with extended environment
    rettype:= resolve(signature'.target,$returnMode)
 
    localOrExported :=
      not MEMQ($op,$formalArgList) and
        getmode($op,e) is ['Mapping,:.] => 'local
      'exported
 
    --6a skip if compiling only certain items but not this one
    -- could be moved closer to the top
    formattedSig := formatUnabbreviated ['Mapping,:signature']
    $compileOnlyCertainItems and _
      not member($op, $compileOnlyCertainItems) =>
        sayBrightly ['"   skipping ", localOrExported,:bright $op]
        [nil,['Mapping,:signature'],oldE]
    sayBrightly ['"   compiling ",localOrExported,
      :bright $op,'": ",:formattedSig]
---------------------> new <---------------------------------
    returnType := signature'.target
--  trialT := returnType = "$" and get("Rep",'value,e) and comp(body,'Rep,e)
    trialT := returnType = "$" and comp(body,$EmptyMode,e)
    ------------------------------------------------------  11/1/94    
    -- try comp-ing in $EmptyMode; if succeed 
    --   if we succeed then trialT.mode = "$" or "Rep"
    --   do a coerce to get the correct result
    T := (trialT and coerce(trialT,returnType)) 
         -------------------------------------- 11/1/94
          or CATCH('compCapsuleBody, compOrCroak(body,returnType,e))
    markChanges(originalDef,T,$signatureOfForm)
    [nil,['Mapping,:signature'],oldE]
    ---------------------------------
 
compCapsuleInner(itemList,m,e) ==
  e:= addInformation(m,e)
           --puts a new 'special' property of $Information
  data:= ["PROGN",:itemList]
      --RPLACd by compCapsuleItems and Friends
  e:= compCapsuleItems(itemList,nil,e)
  $convert2NewCompiler => 
     [nil,m,e] --nonsense but that's fine
  localParList:= $functorLocalParameters
  if $addForm then data:= ['add,$addForm,data]
  code:=
    $insideCategoryIfTrue and not $insideCategoryPackageIfTrue => data
    processFunctor($form,$signature,data,localParList,e)
  [MKPF([:$getDomainCode,code],"PROGN"),m,e]

compSingleCapsuleItem(item,$predl,$e) ==
  $localImportStack : local := nil
  $localDeclareStack: local := nil
  $markFreeStack: local := nil
  newItem := macroExpandInPlace(item,qe(25,$e))
  qe(26,$e)
  doIt(newItem, $predl)
  qe(27,$e)
  $e
 
compImport(["import",:doms],m,e) ==
  for dom in doms repeat 
    dom := markKillAll dom
    markImport dom
    e:=addDomain(dom,e)
  ["/throwAway",$NoValueMode,e]
 
mkUnion(a,b) ==
  b="$" and $Rep is ["Union",:l] => b
  a is ["Union",:l] =>
    b is ["Union",:l'] => ["Union",:setUnion(l,l')]
    member(b, l) => a
    ["Union",:setUnion([b],l)]
  b is ["Union",:l] => 
    member(a, l) => b
    ["Union",:setUnion([a],l)]
  string? a => ["Union",b,a]
  ["Union",a,b]

compForMode(x,m,e) ==
  $compForModeIfTrue: local:= true
  $convert2NewCompiler: local := nil
  comp(x,m,e)
 
compMakeCategoryObject(c,$e) ==
  not isCategoryForm(c,$e) => nil
  c := markKillAll c
  u:= mkEvalableCategoryForm c => [eval markKillAll u,$Category,$e]
  nil
 
macroExpand(x,e) ==   --not worked out yet
  atom x => (u:= get(x,'macro,e) => macroExpand(u,e); x)
  x is ['DEF,lhs,sig,spCases,rhs] =>
    ['DEF,macroExpand(lhs,e), macroExpandList(sig,e),macroExpandList(spCases,e),
      macroExpand(rhs,e)]
  x is ['MI,a,b] => 
      ['MI,a,macroExpand(b,e)]
  macroExpandList(x,e)
 
--======================================================================
--                    From apply.boot
--======================================================================
applyMapping([op,:argl],m,e,ml) ==
  #argl~=#ml-1 => nil
  isCategoryForm(first ml,e) =>
                                --is op a functor?
    pairlis:= pairList(argl,$FormalMapVariableList)
    ml' := SUBLIS(pairlis, ml)
    argl':=
      [T.expr for x in argl for m' in rest ml'] where
        T() == [.,.,e]:= comp(x,m',e) or return "failed"
    if argl'="failed" then return nil
    form:= [op,:argl']
---------------------> new <----------------------------
    if constructor? op then form := markKillAll form
---------------------> new <----------------------------
    convert([form,first ml',e],m)
  argl':=
    [T.expr for x in argl for m' in rest ml] where
      T() == [.,.,e]:= comp(x,m',e) or return "failed"
  if argl'="failed" then return nil
  form:=
    not MEMQ(op,$formalArgList) and atom op and not get(op,'value,e) =>
      nprefix := $prefix or
   -- following needed for referencing local funs at capsule level
        getAbbreviation($op,#rest $form)
      [op',:argl',"$"] where
        op':= INTERN strconc(encodeItem nprefix,";",encodeItem op)
    ['%call,['applyFun,op],:argl']
  pairlis := pairList(argl',$FormalMapVariableList)
  convert([form,SUBLIS(pairlis,first ml),e],m)
 
compFormWithModemap(form,m,e,modemap) ==
  compFormWithModemap1(form,m,e,modemap,true) or compFormWithModemap1(form,m,e,modemap,false)

compFormWithModemap1(form,m,e,modemap,Rep2Dollar?) ==
  [op,:argl] := form := markKillExpr form
  [[dc,:.],:.] := modemap
----------> new: <-----------
  if Rep2Dollar? then 
    if dc = 'Rep then
      modemap := SUBST('Rep,'_$,modemap)
      m       := SUBST('Rep,'_$,m)
    else return nil
----------> new: <-----------
  [map:= [.,target,:.],[pred,impl]]:= modemap
  -- this fails if the subsuming modemap is conditional
  --impl is ['Subsumed,:.] => nil
  if isCategoryForm(target,e) and isFunctor op then
    [modemap,e]:= substituteIntoFunctorModemap(argl,modemap,e) or return nil
    [map:= [.,target,:.],:cexpr]:= modemap
  sv:=listOfSharpVars map
  if sv then
     -- SAY [ "compiling ", op, " in compFormWithModemap,
     -- mode= ",map," sharp vars=",sv]
    for x in argl for ss in $FormalMapVariableList repeat
      if ss in sv then
        [map:= [.,target,:.],:cexpr]:= modemap :=SUBST(x,ss,modemap)
        -- SAY ["new map is",map]
  not (target':= coerceable(target,m,e)) => nil
  markMap := map
  map:= [target',:rest map]
  [f,Tl,sl]:= compApplyModemap(form,modemap,e) or return nil
 
  --generate code; return
  T:=
    e':=
      Tl => last(Tl).env
      e
    [x',m',e'] where
      m':= SUBLIS(sl,map.1)
      x':=
        form':= [f,:[t.expr for t in Tl]]
        m'=$Category or isCategoryForm(m',e) => form'
        -- try to deal with new-style Unions where we know the conditions
        op = "elt" and f is ['XLAM,:.] and IDENTP(z:=first argl) and
          (c:=get(z,'condition,e)) and
            c is [["case",=z,c1]] and
              (c1 is ['_:,=(second argl),=m] or EQ(c1,second argl) ) =>
-- first is a full tag, as placed by getInverseEnvironment
-- second is what getSuccessEnvironment will place there
                ['%tail,z]
        markTran(form,form',markMap,e')
  qt(18,T)
  convert(T,m)

convert(T,m) ==
  tcheck T
  qe(23,T.env)
  coerce(T,resolve(T.mode,m) or return nil)

compElt(origForm,m,E) ==
  form := markKillAll origForm
  form isnt ["elt",aDomain,anOp] => compForm(origForm,m,E)
  aDomain="Lisp" =>
    markLisp([anOp',m,E],E)where anOp'() == (anOp=$Zero => 0; anOp=$One => 1; anOp)
  isDomainForm(aDomain,E) =>
    markImport opOf aDomain
    E:= addDomain(aDomain,E)
    mmList:= getModemapListFromDomain(anOp,0,aDomain,E)
    modemap:=
      n:=#mmList
      1=n => mmList.0
      0=n =>
        return
          stackMessage ['"Operation ","%b",anOp,"%d",
                         '"missing from domain: ", aDomain]
      stackWarning ['"more than 1 modemap for: ",anOp,
                  '" with dc=",aDomain,'" ===>"
        ,mmList]
      mmList.0
----------> new: <-----------
    if aDomain = 'Rep then
      modemap := SUBST('Rep,'_$,modemap)
      m       := SUBST('Rep,'_$,m)
----------> new: <-----------
    [sig,[pred,val]]:= modemap
    #sig~=2 and val isnt ["CONST",:.] => nil
--+
    val := genDeltaEntry([opOf anOp,:modemap],E)
    x := markTran(origForm,[val],sig,[E])
    [x,second sig,E] --implies fn calls used to access constants
  compForm(origForm,m,E)
 
pause op == op
compApplyModemap(form,modemap,$e) ==
  sl := nil
  [op,:argl] := form                   --form to be compiled
  [[mc,mr,:margl],:fnsel] := modemap   --modemap we are testing
 
  -- $e     is the current environment
  -- sl     substitution list, nil means bottom-up, otherwise top-down
 
  -- 0.  fail immediately if #argl=#margl
 
  if #argl~=#margl then return nil
 
  -- 1.  use modemap to evaluate arguments, returning failed if
  --     not possible
 
  lt:=
    [[.,m',$e]:=
      comp(y,g,$e) or return "failed" where
        g:= SUBLIS(sl,m) where
            sl:= pmatchWithSl(m',m,sl) for y in argl for m in margl]
  lt="failed" => return nil
 
  -- 2.  coerce each argument to final domain, returning failed
  --     if not possible
 
  lt':= [coerce(y,d) or return "failed"
         for y in lt for d in SUBLIS(sl,margl)]
  lt'="failed" => return nil
 
  -- 3.  obtain domain-specific function, if possible, and return
 
  --$bindings is bound by compMapCond
  [f,$bindings]:= compMapCond(op,mc,sl,fnsel) or return nil
 
--+ can no longer trust what the modemap says for a reference into
--+ an exterior domain (it is calculating the displacement based on view
--+ information which is no longer valid; thus ignore this index and
--+ store the signature instead.
 
--$NRTflag=true and f is [op1,d,.] and NE(d,'$) and member(op1,'(ELT CONST)) =>
  f is [op1,d,.] and op1 in '(ELT CONST Subsumed) =>
    [genDeltaEntry([op,:modemap],$e),lt',$bindings]
  markImport mc
  [f,lt',$bindings]
 
compMapCond''(cexpr,dc) ==
  cexpr=true => true
  --cexpr = "true" => true
---------------> new <----------------------
  cexpr is [op,:l] and op in '(and AND) => and/[compMapCond''(u,dc) for u in l]
  cexpr is [op,:l] and op in '(or OR)   => or/[compMapCond''(u,dc) for u in l]
---------------> new <----------------------
  cexpr is ["not",u] => not compMapCond''(u,dc)
  cexpr is ["has",name,cat] => (knownInfo cexpr => true; false)
        --for the time being we'll stop here - shouldn't happen so far
        --$disregardConditionIfTrue => true
        --stackSemanticError(("not known that",'"%b",name,
        -- '"%d","has",'"%b",cat,'"%d"),nil)
  --now it must be an attribute
  member(["ATTRIBUTE",dc,cexpr],get("$Information","special",$e)) => true
  --for the time being we'll stop here - shouldn't happen so far
  stackMessage ["not known that",'"%b",dc,'"%d","has",'"%b",cexpr,'"%d"]
  false
 
--======================================================================
--                    From nruncomp.boot
--======================================================================
optCallEval u ==
  u is ["List",:.] => List Integer()
  u is ["Vector",:.] => Vector Integer()
  u is ["PrimitiveArray",:.] => PrimitiveArray Integer()
  u is ["FactoredForm",:.] => FactoredForm Integer()
  u is ["Matrix",:.] => Matrix Integer()
  eval u
 
optDeltaEntry(op,sig,dc,eltOrConst) ==
  return nil    --------> kill it
  $killOptimizeIfTrue = true => nil
  ndc :=
    dc = '$ => $functorForm
    atom dc and (dcval := get(dc,'value,$e)) => dcval.expr
    dc
--if (atom dc) and (dcval := get(dc,'value,$e))
--   then ndc := dcval.expr
--   else ndc := dc
  sig := SUBST(ndc,dc,sig)
  not optimizableDomain? ndc => nil
  dcval := optCallEval ndc
  -- MSUBST guarantees to use EQUAL testing
  sig := MSUBST(devaluate dcval, ndc, sig)
  if rest ndc then
     for new in rest devaluate dcval for old in rest ndc repeat
       sig := MSUBST(new,old,sig)
     -- optCallEval sends (List X) to (LIst (Integer)) etc,
     -- so we should make the same transformation
  fn := compiledLookup(op,sig,dcval)
  if null fn then
    -- following code is to handle selectors like first, rest
     nsig := [quoteSelector tt for tt in sig] where
       quoteSelector(x) ==
         not(IDENTP x) => x
         get(x,'value,$e) => x
         x='$ => x
         MKQ x
     fn := compiledLookup(op,nsig,dcval)
     if null fn then return nil
  eltOrConst="CONST" => 
     hehe fn
     [op]                    -----------> return just the op here
--   ['XLAM,'ignore,MKQ SPADCALL fn]
  getFunctionReplacement compileTimeBindingOf first fn
 
genDeltaEntry(opMmPair,e) ==
--called from compApplyModemap
--$NRTdeltaLength=0.. always equals length of $NRTdeltaList
  [.,[odc,:.],.] := opMmPair
  --opModemapPair := SUBLIS($LocalDomainAlist,opMmPair)
  [op,[dc,:sig],[.,cform:=[eltOrConst,:.]]] := opMmPair
  if $profileCompiler = true then 
    profileRecord(dc,op,sig)
--  markImport dc
  eltOrConst = 'XLAM => cform
  if eltOrConst = 'Subsumed then eltOrConst := 'ELT
    -- following hack needed to invert Rep to $ substitution
  if odc = 'Rep and cform is [.,.,osig] then sig:=osig
  newimp := optDeltaEntry(op,sig,dc,eltOrConst) => newimp
  setDifference(listOfBoundVars dc,$functorLocalParameters) ~= [] =>
    ['applyFun,['compiledLookupCheck,MKQ op,
         mkList consSig(sig,dc),consDomainForm(dc,nil)]]
 --if cons? dc then
 --   sig := substitute('$,dc,sig)
 --   cform := substitute('$,dc,cform)
  opModemapPair :=
    [op,[dc,:[genDeltaSig x for x in sig]],['T,cform]] -- force pred to T
  if null NRTassocIndex dc and
    (member(dc,$functorLocalParameters) or cons? dc) then
    --create "%domain" entry to $NRTdeltaList
      $NRTdeltaList:= [["%domain",NRTaddInner dc,:dc],:$NRTdeltaList]
      saveNRTdeltaListComp:= $NRTdeltaListComp:=[nil,:$NRTdeltaListComp]
      $NRTdeltaLength := $NRTdeltaLength+1
      compEntry:=
        dc
      saveNRTdeltaListComp.first := compEntry
      chk(saveNRTdeltaListComp,102)
  u :=
    [eltOrConst,'$,$NRTbase+$NRTdeltaLength-index] where index() ==
      (n:= POSN1(opModemapPair,$NRTdeltaList)) => n + 1
        --n + 1 since $NRTdeltaLength is 1 too large
      $NRTdeltaList:= [opModemapPair,:$NRTdeltaList]
      $NRTdeltaListComp:=[nil,:$NRTdeltaListComp]
      $NRTdeltaLength := $NRTdeltaLength+1
      0
  u

--======================================================================
--                    From nruncomp.boot
--======================================================================
parseIf t ==
  t isnt [p,a,b] => t
  ifTran(parseTran p,parseTran a,parseTran b) where
    ifTran(p,a,b) ==
      null($InteractiveMode) and p='true  => a
      null($InteractiveMode) and p='false  => b
      p is ['not,p'] => ifTran(p',b,a)
      p is ['IF,p',a',b'] => ifTran(p',ifTran(a',COPY a,COPY b),ifTran(b',a,b))
      p is ['SEQ,:l,['exit,1,p']] =>
        ['SEQ,:l,['exit,1,ifTran(p',incExitLevel a,incExitLevel b)]]
         --this assumes that l has no exits
      a is ['IF, =p,a',.] => ['IF,p,a',b]
      b is ['IF, =p,.,b'] => ['IF,p,a,b']
--      makeSimplePredicateOrNil p is ['SEQ,:s,['exit,1,val]] =>
--        parseTran ['SEQ,:s,['exit,1,incExitLevel ['IF,val,a,b]]]
      ['IF,p,a,b]

--======================================================================
--                         From parse.boot
--======================================================================
parseNot u ==  ['not,parseTran first u]

makeSimplePredicateOrNil p == nil
 
--======================================================================
--                         From g-cndata.boot
--======================================================================
mkUserConstructorAbbreviation(c,a,type) ==
  if $AnalyzeOnly or $convert2NewCompiler then
    $abbreviationStack := [[type,a,:c],:$abbreviationStack]
  if cons? c then c:= first c  --  Existing constructors will be wrapped
  constructorAbbreviationErrorCheck(c,a,type,'abbreviationError)
  clearClams()
  clearConstructorCache(c)
  installConstructor(c,type)
  setAutoLoadProperty(c)
 
--======================================================================
--                         From iterator.boot
--======================================================================

compreduce(form is [.,op,x],m,e) ==
  T := compForm(form,m,e) or return nil
  y := T.expr
  y.first := "REDUCE"
  ------------------<== distinquish this as the special reduce form
  (y is ["REDUCE",:.]) and (id:= getIdentity(op,e)) and (u := comp0(id,m,e)) and
    # getNumberTypesInScope() > 1 => markSimpleReduce([:y, ["@",u.expr,m]], T)
  T

compReduce1(form is ["REDUCE",op,.,collectForm],m,e,$formalArgList) ==
-------------------------------> 11/28 all new to preserve collect forms
  markImport m
  [collectOp,:itl,body]:= collectForm
  $e:= e
  itl:= [([.,$e]:= compIterator(x,$e) or return "failed").0 for x in itl]
  itl="failed" => return nil
  e:= $e
  T0 := comp0(body,m,e) or return nil
  md := T0.mode
  T1 := compOrCroak(collectForm,["List",md],e) 
  T  := [["REDUCE",op,nil,T1.expr],md,T1.env]
  markReduce(form,T)
 
compIterator(it,e) ==
  it is ["IN",x,y] =>
    --these two lines must be in this order, to get "for f in list f"
    --to give  an error message if f is undefined
  ---------------> new <---------------------
    [y',m,e] := markInValue(y, e)
    x := markKillAll x
    ------------------
    $formalArgList:= [x,:$formalArgList]
    [.,mUnder]:=
      modeIsAggregateOf("List",m,e) or modeIsAggregateOf("Vector",m,e) or return
         stackMessage ["mode: ",m," must be a list or vector of some mode"]
    if null get(x,"mode",e) then [.,.,e]:=
      compMakeDeclaration(x,mUnder,e) or return nil
    e:= giveVariableSomeValue(x,mUnder,e)
    markReduceIn(it, [["IN",x,y'],e])
  it is ["ON",x,y] =>
---------------> new <---------------------
    x := markKillAll x
    ------------------
    $formalArgList:= [x,:$formalArgList]
    y := markKillAll y
    markImport m
---------------> new <---------------------
    [y',m,e]:= comp(y,$EmptyMode,e) or return nil
    [.,mUnder]:=
      modeIsAggregateOf("List",m,e) or return
        stackMessage ["mode: ",m," must be a list of other modes"]
    if null get(x,"mode",e) then [.,.,e]:=
      compMakeDeclaration(x,m,e) or return nil
    e:= giveVariableSomeValue(x,m,e)
    [["ON",x,y'],e]
  it is ["STEP",oindex,start,inc,:optFinal] =>
    index := markKillAll oindex
    $formalArgList:= [index,:$formalArgList]
    --if all start/inc/end compile as small integers, then loop
    --is compiled as a small integer loop
    final':= nil
---------------> new <---------------------
    u := smallIntegerStep(it,index,start,inc,optFinal,e) => u
---------------> new <---------------------
    [start,.,e]:=
      comp(markKillAll start,$Integer,e) or return
        stackMessage ["start value of index: ",start," must be an integer"]
    [inc,.,e]:=
      comp(markKillAll inc,$Integer,e) or return
        stackMessage ["index increment:",inc," must be an integer"]
    if optFinal is [final] then
      [final,.,e]:=
        comp(markKillAll final,$Integer,e) or return
          stackMessage ["final value of index: ",final," must be an integer"]
      optFinal:= [final]
    indexmode:=
      comp(third it,$NonNegativeInteger,e) => $NonNegativeInteger
      $Integer
--  markImport ['Segment,indexmode]
    if null get(index,"mode",e) then [.,.,e]:=
      compMakeDeclaration(index,indexmode,e) or return nil
    e:= giveVariableSomeValue(index,indexmode,e)
    markReduceStep(it, [["STEP",markStep(index),start,inc,:optFinal],e])
  it is ["WHILE",p] =>
    [p',m,e]:=
      comp(p,$Boolean,e) or return
        stackMessage ["WHILE operand: ",p," is not Boolean valued"]
    markReduceWhile(it, [["WHILE",p'],e])
  it is ["UNTIL",p] => markReduceUntil(it, ($until:= p; ['$until,e]))
  it is ["|",x] =>
    u:=
      comp(x,$Boolean,e) or return
        stackMessage ["SUCHTHAT operand: ",x," is not Boolean value"]
    markReduceSuchthat(it, [["|",u.expr],u.env])
  nil

smallIntegerStep(it,index,start,inc,optFinal,e) ==
  start    := markKillAll start
  inc      := markKillAll inc
  optFinal := markKillAll optFinal
  startNum := source2Number start
  incNum   := source2Number inc
  mode := get(index,"mode",e) 
--fail if
----> a) index has a mode that is not $SmallInteger
----> b) one of start,inc, final won't comp as a $SmallInteger
  mode and mode ~= $SmallInteger => nil
  null (start':= comp(start,$SmallInteger,e)) => nil
  null (inc':= comp(inc,$SmallInteger,start'.env)) => nil
  if optFinal is [final] and not (final':= comp(final,$SmallInteger,inc'.env)) then
--    not (integer? startNum and integer? incNum) => return nil
--    null integer? startNum or abs startNum > 100 => return nil
    -----> assume that optFinal is $SmallInteger
    T := comp(final,$EmptyMode,inc'.env) or return nil
    final' := T
    maximalSuperType T.mode ~= $Integer => return nil
    givenRange := T.mode
  indexmode:= $SmallInteger
  [.,.,e]:= compMakeDeclaration(index,indexmode,
     (final' => final'.env; inc'.env)) or return nil
  range :=
    integer? startNum and integer? incNum =>
      startNum >  0 and incNum > 0 => $PositiveInteger
      startNum <  0 and incNum < 0 => $NegativeInteger
      incNum >  0 => $NonNegativeInteger   --startNum = 0
      $NonPositiveInteger
    givenRange => givenRange
    nil
  e:= put(index,"range",range,e)
  e:= giveVariableSomeValue(index,indexmode,e)
  noptFinal := 
    final' => 
      [final'.expr]
    nil
  [markStepSI(it,["ISTEP",index,start'.expr,inc'.expr,:noptFinal]),e]

source2Number n ==
  n := markKillAll n
  n = $Zero => 0
  n = $One  => 1
  n

compRepeatOrCollect(form,m,e) ==
  fn(form,[m,:$exitModeStack],[#$exitModeStack,:$leaveLevelStack],$formalArgList
    ,e) where
      fn(form,$exitModeStack,$leaveLevelStack,$formalArgList,e) ==
        $until: local
        [repeatOrCollect,:itl,body]:= form
        itl':=
          [([x',e]:= compIterator(x,e) or return "failed"; x') for x in itl]
        itl'="failed" => nil
        targetMode:= first $exitModeStack
--        pp '"---------"
--        pp targetMode
        bodyMode:=
          repeatOrCollect="COLLECT" =>
            targetMode = '$EmptyMode => '$EmptyMode
            (u:=modeIsAggregateOf('List,targetMode,e)) =>
              second u
            (u:=modeIsAggregateOf('Vector,targetMode,e)) =>
              repeatOrCollect:='COLLECTV
              second u
            stackMessage('"Invalid collect bodytype")
            return nil
            -- If we're doing a collect, and the type isn't conformable
            -- then we've boobed. JHD 26.July.1990
          $NoValueMode
        [body',m',e']:= T :=
          -- (m1:= listOrVectorElementMode targetMode) and comp(body,m1,e) or
            compOrCroak(body,bodyMode,e) or return nil
        markRepeatBody(body, T)
        if $until then
          [untilCode,.,e']:= comp($until,$Boolean,e')
          itl':= substitute(["UNTIL",untilCode],'$until,itl')
        form':= [repeatOrCollect,:itl',body']
        m'':=
          repeatOrCollect="COLLECT" =>
            (u:=modeIsAggregateOf('List,targetMode,e)) => first u
            ["List",m']
          repeatOrCollect="COLLECTV" =>
            (u:=modeIsAggregateOf('Vector,targetMode,e)) => first u
            ["Vector",m']
          m'
--------> new <--------------
        markImport m''
--------> new <--------------
        markRepeat(form,coerceExit([form',m'',e'],targetMode))
 
chaseInferences(origPred,$e) ==
  pred := markKillAll origPred
  ----------------------------12/4/94 do this immediately
  foo hasToInfo pred where
    foo pred ==
      knownInfo pred => nil
      $e:= actOnInfo(pred,$e)
      pred:= infoToHas pred
      for u in get("$Information","special",$e) repeat
        u is ['%when,:l] =>
          for [ante,:conseq] in l repeat
            ante=pred => [foo w for w in conseq]
            ante is ["and",:ante'] and member(pred,ante') =>
              ante':= delete(pred,ante')
              v':=
                # ante'=1 => first ante'
                ["and",:ante']
              v':= ['%when,[v',:conseq]]
              member(v',get("$Information","special",$e)) => nil
              $e:=
                put("$Information","special",[v',:
                  get("$Information","special",$e)],$e)
            nil
  $e
 
--======================================================================
--                   doit Code
--======================================================================
doIt(item,$predl) ==
  $GENNO: local:= 0
  $coerceList: local := nil
  --->                 
  if item is ['PART,.,a] then item := a
  -------------------------------------
  item is ['SEQ,:.] => doItSeq item
  isDomainForm(item,$e) => doItDomain item
  item is ["%LET",:.] => doItLet item
  item is [":",a,t] => [.,.,$e]:= 
    markDeclaredImport markKillAll t
    compOrCroak(item,$EmptyMode,$e)
  item is ["import",:doms] =>
     item := ["import",:(doms := markKillAll doms)]
     for dom in doms repeat
       sayBrightly ['"   importing ",:formatUnabbreviated dom]
     [.,.,$e] := compOrCroak(item,$EmptyMode,$e)
     wiReplaceNode(item,'(PROGN),10)
  item is ["IF",:.] => doItIf(item,$predl,$e)
  item is ["where",b,:l] => compOrCroak(item,$EmptyMode,$e)
  item is ["MDEF",:.] => [.,.,$e]:= compOrCroak(item,$EmptyMode,$e)
  item is ['DEF,:.] => doItDef item
  T:= compOrCroak(item,$EmptyMode,$e) => doItExpression(item,T)
  true => cannotDo()

holdIt item == item
 
doItIf(item is [.,p,x,y],$predl,$e) ==
  olde:= $e
  [p',.,$e]:= qt(19,comp(p,$Boolean,$e)) or userError ['"not a Boolean:",p]
  oldFLP:=$functorLocalParameters
  if x~="%noBranch" then
--> new <-----------------------
    qe(20,compSingleCapsuleItem(x,[p,:$predl],getSuccessEnvironment(markKillAll p,$e)))
---> new                                                 -----------
    x':=localExtras(oldFLP)
          where localExtras(oldFLP) ==
            EQ(oldFLP,$functorLocalParameters) => NIL
            flp1:=$functorLocalParameters
            oldFLP':=oldFLP
            n:=0
            while oldFLP' repeat
              oldFLP':=rest oldFLP'
              flp1:=rest flp1
              n:=n+1
            -- Now we have to add code to compile all the elements
            -- of functorLocalParameters that were added during the
            -- conditional compilation
            nils:=ans:=[]
            for u in flp1 repeat -- is =u form always an atom?
              if atom u or (or/[v is [.,=u,:.] for v in $getDomainCode])
                then
                  nils:=[u,:nils]
                else
                  gv := gensym()
                  ans:=[["%LET",gv,u],:ans]
                  nils:=[gv,:nils]
              n:=n+1

            $functorLocalParameters:=[:oldFLP,:reverse nils]
            reverse ans
  oldFLP:=$functorLocalParameters
  if y~="%noBranch" then
--> new <-----------------------
    qe(21,compSingleCapsuleItem(y,[['not, p],:$predl],getInverseEnvironment(markKillAll p,olde)))
-->                                                      ----------- 
    y':=localExtras(oldFLP)
  wiReplaceNode(item,['%when,[p',x,:x'],['%otherwise,y,:y']],12)

doItSeq item == 
  ['SEQ,:l,['exit,1,x]] := item
  item.first := "PROGN"
  lastNode(item).first := x
  for it1 in rest item repeat $e:= compSingleCapsuleItem(it1,$predl,$e)

doItDomain item ==
  -- convert naked top level domains to import
  u:= ["import", [first item,:rest item]]
  markImport second u
  stackWarning ["Use: import ", [first item,:rest item]]
--wiReplaceNode(item, u, 14)
  item.first := first u
  item.rest := rest u
  doIt(item,$predl)

doItLet item ==
  qe(3,$e)
  res := doItLet1 item
  qe(4,$e)
  res
 
doItLet1 item ==
  ["%LET",lhs,rhs,:.] := item
  not (compOrCroak(item,$EmptyMode,$e) is [code,.,$e]) =>
      stackSemanticError(["cannot compile assigned value to",:bright lhs],nil)
  qe(5,$e)
  code := markKillAll code
  not (code is ["%LET",lhs',rhs',:.] and atom lhs') =>
      code is ["PROGN",:.] =>
         stackSemanticError(["multiple assignment ",item," not allowed"],nil)
      wiReplaceNode(item, code, 24)
  lhs:= lhs'
  if not member(KAR rhs,$NonMentionableDomainNames) and
      not MEMQ(lhs, $functorLocalParameters) then
         $functorLocalParameters:= [:$functorLocalParameters,lhs]
  if (rhs' := rhsOfLetIsDomainForm code) then
      if isFunctor rhs' then
        $globalImportDefAlist := pp [[lhs, :rhs'],:$globalImportDefAlist]
      if lhs="Rep" then
        $Representation:= (get("Rep",'value,$e)).expr
           --$Representation bound by compDefineFunctor, used in compNoStacking
--+
        if $NRTopt = true
          then NRTgetLocalIndex $Representation
--+
      $LocalDomainAlist:= --see genDeltaEntry
        [[lhs,:SUBLIS($LocalDomainAlist,(get(lhs,'value,$e)).0)],:$LocalDomainAlist]
--+
  qe(6,$e)
  code is ["%LET",:.] =>
      rhsCode:= rhs'
      op := '%store
      wiReplaceNode(item,[op,['%tref,'$,NRTgetLocalIndex lhs],rhsCode], 16)
  wiReplaceNode(item, code, 18)

rhsOfLetIsDomainForm code ==
  code is ["%LET",.,rhs',:.] =>
    isDomainForm(rhs',$e) => rhs'
    isDomainForm(rhs' := markKillAll rhs',$e) => rhs'
    false
  false

doItDef item == 
  ['DEF,[op,:.],:.] := item
  body:= isMacro(item,$e) => $e := putMacro(op,body,$e)
  [.,.,$e]:= t:= compOrCroak(item,$EmptyMode,$e)
  chk(item,3)
  item.first := "CodeDefine"
        --Note that DescendCode, in CodeDefine, is looking for this
  second(item).rest := [$signatureOfForm]
  chk(item,4)
      --This is how the signature is updated for buildFunctor to recognise
--+
  functionPart:= ['dispatchFunction,t.expr]
  wiReplaceNode(CDDR item,[functionPart], 20)
  chk(item, 30)

doItExpression(item,T) ==
  SETQ($ITEM,COPY item)
  SETQ($T1,COPY T.expr)
  chk(T.expr, 304)
  u := markCapsuleExpression(item, T)
  [code,.,$e]:= u
  wiReplaceNode(item,code, 22)

wiReplaceNode(node,ocode,key) ==
  ncode := [first ocode,:rest ocode]
  code := replaceNodeInStructureBy(node,ncode)
  SETQ($NODE,COPY node)
  SETQ($NODE1, COPY first code)
  SETQ($NODE2, COPY rest  code)
  node.first := first code
  node.rest := rest  code
  chk(code, key)
  chk(node, key + 1)

replaceNodeInStructureBy(node, x) == 
  $nodeCopy: local := [first node,:rest node]
  replaceNodeBy(node, x)
  node

replaceNodeBy(node, x) ==
  atom x => nil
  for y in tails x | x is [=node,:.] repeat x.first := $nodeCopy
  nil  

chk(x,key) == fn(x,0,key) where fn(x,cnt,key) ==
  cnt > 10000 => 
    sayBrightly ["--> ", key, " <---"]
    hahaha(key)
  atom x => cnt
  vector? x => systemError nil
  for y in x repeat cnt := fn(y, cnt + 1, key)
  cnt
 
