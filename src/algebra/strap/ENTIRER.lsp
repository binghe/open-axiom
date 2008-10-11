
(/VERSIONCHECK 2) 

(DEFPARAMETER |EntireRing;AL| 'NIL) 

(DEFUN |EntireRing;| ()
  (PROG (#0=#:G1397)
    (RETURN
      (PROG1 (LETT #0#
                   (|Join| (|Ring|) (|BiModule| '$ '$)
                           (|mkCategory| '|package| NIL
                               '((|noZeroDivisors| T)) 'NIL NIL))
                   |EntireRing|)
        (SETELT #0# 0 '(|EntireRing|)))))) 

(DEFUN |EntireRing| ()
  (LET ()
    (COND
      (|EntireRing;AL|)
      (T (SETQ |EntireRing;AL| (|EntireRing;|)))))) 

(SETQ |$CategoryFrame|
      (|put| '|EntireRing| '|isCategory| T
             (|addModemap| '|EntireRing| '(|EntireRing|)
                 '((|Category|)) T '|EntireRing| |$CategoryFrame|))) 

(MAKEPROP '|EntireRing| 'NILADIC T) 
