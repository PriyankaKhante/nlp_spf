//determiners
the :- N/N : (lambda $0:<e,t> $0)
is :- N/N : (lambda $0:<e,t> $0)
a :- N/N : (lambda $0:<e,t> $0)
of :- N/N : (lambda $0:<e,t> $0)
the :- S/N : (lambda $0:e $0)
is :- NP/NP : (lambda $0:e $0)
of :- NP/NP : (lambda $0:e $0)
the :- N/ADJ : (lambda $0:<e,t> (the:<<e,t>,e> $0))

that :- PP/(S\NP) : (lambda $0:<e,t> $0)
that :- PP/(S/NP) : (lambda $0:<e,t> $0)
are :- PP/PP : (lambda $0:<e,t> $0)
what :- S/N : (lambda $0:e $0)

// copula, etc.
are :- (N\N)/N : (lambda $0:<e,t> (lambda $1:<e,t> (lambda $2:e (and:<t*,t> ($0 $2) ($1 $2)))))
are :- (S\NP)/PP : (lambda $0:<e,t> $0)
does :- (S/NP)/(S/NP) : (lambda $0:<e,t> $0)
does :- (S\NP)/(S\NP) : (lambda $0:<e,t> $0)
is :- (S/NP)/(S/NP) : (lambda $0:<e,t> $0)
have :- (S/NP)/(S/NP) : (lambda $0:<e,t> $0)
is :- (S\NP)/(S\NP) : (lambda $0:<e,t> $0)
are there :- S\NP : (lambda $0:e true:t)
is :- (S\NP)/NP : (lambda $0:e (lambda $1:e (equals:<e,<e,t>> $1 $0)))

// quantifier
is :- (NP\N)/(NP/N)  : (lambda $0:<<e,t>,e> $0)
are  :- (NP\N)/(NP/N)  : (lambda $0:<<e,t>,e> $0)
with  :- (NP\N)/(NP/N)  : (lambda $0:<<e,t>,e> $0)
with the :- PP/(S\NP) : (lambda $0:<e,t> $0)

// other garbage required
foo :- N/N : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> ($0 $1) (dark:<e,t> $1))))
//fuu :- (ADJ\N)/ADJ : (lambda $0:e (big:<e,t> $0))
fqq :- N/N : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> ($0 $1) (pink:<e,t> $1))))
//fdd :- (ADJ\N)/N : (lambda $0:e (pink:<e,t> $0))
frr :- N : (lambda $0:e (block:<e,t> $0))
