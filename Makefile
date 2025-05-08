_nuke-java-version:
	@rm -f .java-version

_nuke-tmp-test-file:
	@rm /tmp/ut-test-file

run-tests-java-11: _nuke-java-version _nuke-tmp-test-file
	@echo temurin-11 > .java-version
	@echo "JAVA 11"
	@lein test
	@stat /tmp/ut-test-file


run-tests-java-21: _nuke-java-version _nuke-tmp-test-file
	@echo temurin-21 > .java-version
	@echo "JAVA 21"
	@lein test
	@stat /tmp/ut-test-file


run-tests: run-tests-java-11 run-tests-java-21

publish:
	@lein deploy clojars


help:
	@echo "Available tasks:"
	@grep -E '^[a-z-]+:' ./Makefile | sed 's/:.*//g'
