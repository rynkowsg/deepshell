.PHONY: lint test

lint:
	\@bin/lint.bash

test:
	bats ./test/src/test.bats
