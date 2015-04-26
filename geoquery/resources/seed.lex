the :- NP/NP : (lambda $0:<e,t> $0)
the :- NP/N : (lambda $0:<e,t> $0)
the :- S/NP : (lambda $0:<e,t> $0) 
the :- S/N : (lambda $0:<e,t> $0) 
is :- (S\NP)/NP : (lambda $0:e $0)

// Dark
frr :- NP/N : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> ($0 $1) (dark:<e,t> $1)))) 
frr :- NP/NP : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> ($0 $1) (dark:<e,t> $1)))) 
frr :- N : dark:<e,t>
// Pink
foo :- NP/N : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> ($0 $1) (pink:<e,t> $1)))) 
//foo :- NP\NP : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> ($0 $1) (pink:<e,t> $1)))) 
// State
woo :- N : state:<s,t>
woo :- NP/NP : (lambda $0:e (state:<s,t> $0)) 

