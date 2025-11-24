(begin
 (define loop
     (lambda (x)
       (if (= x 10)
           (begin
            (display "done!")
            x)
           (begin
            (display x)
            (loop (+ x 1))))))
 (loop 0))
