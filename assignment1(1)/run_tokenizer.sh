#!/bin/sh
java -cp classes ir.TokenTest -f kgram_test.txt -p patterns.txt -rp -cf > tokenized_result.txt
