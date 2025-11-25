# A small lisp written in a literate notebook

This is a toy lisp and a teaching tool I have been meaning to develop for years
now. It is written in under 200 lines of clojure code (including comments!) and
has support for:
- [x] function and value definitions
- [x] full recursion (this one surprised me as well!)
- [x] if conditionals
- [x] let syntax
- [ ] recursive let syntax
- [ ] macros

The entire code is written in a functional style that clojure mandates 
and heavily uses pattern matching provided by
[core.match](https://github.com/clojure/core.match). 
