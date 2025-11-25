# A small lisp written in a literate notebook

This is a toy lisp and a teaching tool I have been meaning to develop for years
now. It is written in under 200 lines of clojure code (including comments!) and
has support for:
- [x] function and value definitions
- [x] full recursion (this one surprised me as well!)
- [x] if conditionals
- [x] let syntax
- [ ] repl
- [ ] recursive let syntax
- [ ] macros

The entire code is written in a functional style that clojure mandates 
and heavily uses pattern matching provided by
[core.match](https://github.com/clojure/core.match). 

# Running
## Clerk
All code is written in a literate notebook powered by
[clerk](https://github.com/nextjournal/clerk "Clerk Live Programming Notebooks")
If you have [clojure](https://clojure.org "clojure programming language")
installed then it would just be a matter of running. Do let it warm up as we are
starting up a new JVM instance. This will start up clerk on
`https://localhost:7777` 

```shell
clojure -X:serve
```

if you wish to specify a different port you can do so here

``` shell
clojure -X:serve :port <ur port number>
```

## TODO: REPL


