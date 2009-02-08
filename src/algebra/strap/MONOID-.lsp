
(/VERSIONCHECK 2) 

(DECLAIM (FTYPE (FUNCTION (|%Thing| |%Shell|) |%Boolean|)
                |MONOID-;one?;SB;1|)) 

(DECLAIM (FTYPE (FUNCTION (|%Shell|) |%Thing|) |MONOID-;sample;S;2|)) 

(DECLAIM (FTYPE (FUNCTION (|%Thing| |%Shell|) |%Pair|)
                |MONOID-;recip;SU;3|)) 

(DECLAIM (FTYPE (FUNCTION (|%Thing| (|%IntegerSection| 0) |%Shell|)
                    |%Thing|)
                |MONOID-;**;SNniS;4|)) 

(DEFUN |MONOID-;one?;SB;1| (|x| $)
  (SPADCALL |x| (|spadConstant| $ 7) (|getShellEntry| $ 9))) 

(DEFUN |MONOID-;sample;S;2| ($) (|spadConstant| $ 7)) 

(DEFUN |MONOID-;recip;SU;3| (|x| $)
  (COND
    ((SPADCALL |x| (|getShellEntry| $ 12)) (CONS 0 |x|))
    ('T (CONS 1 "failed")))) 

(DEFUN |MONOID-;**;SNniS;4| (|x| |n| $)
  (COND
    ((ZEROP |n|) (|spadConstant| $ 7))
    ('T (SPADCALL |x| |n| (|getShellEntry| $ 17))))) 

(DEFUN |Monoid&| (|#1|)
  (PROG (|dv$1| |dv$| $ |pv$|)
    (RETURN
      (PROGN
        (LETT |dv$1| (|devaluate| |#1|) . #0=(|Monoid&|))
        (LETT |dv$| (LIST '|Monoid&| |dv$1|) . #0#)
        (LETT $ (|newShell| 20) . #0#)
        (|setShellEntry| $ 0 |dv$|)
        (|setShellEntry| $ 3
            (LETT |pv$| (|buildPredVector| 0 0 NIL) . #0#))
        (|stuffDomainSlots| $)
        (|setShellEntry| $ 6 |#1|)
        $)))) 

(MAKEPROP '|Monoid&| '|infovec|
    (LIST '#(NIL NIL NIL NIL NIL NIL (|local| |#1|) (0 . |One|)
             (|Boolean|) (4 . =) |MONOID-;one?;SB;1|
             |MONOID-;sample;S;2| (10 . |one?|) (|Union| $ '"failed")
             |MONOID-;recip;SU;3| (|PositiveInteger|)
             (|RepeatedSquaring| 6) (15 . |expt|)
             (|NonNegativeInteger|) |MONOID-;**;SNniS;4|)
          '#(|sample| 21 |recip| 25 |one?| 30 ** 35) 'NIL
          (CONS (|makeByteWordVec2| 1 'NIL)
                (CONS '#()
                      (CONS '#()
                            (|makeByteWordVec2| 19
                                '(0 6 0 7 2 6 8 0 0 9 1 6 8 0 12 2 16 6
                                  6 15 17 0 0 0 11 1 0 13 0 14 1 0 8 0
                                  10 2 0 0 0 18 19)))))
          '|lookupComplete|)) 
