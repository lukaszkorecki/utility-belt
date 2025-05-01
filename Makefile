nuke-java-version:
	@rm -f .java-version

run-tests-java-11: nuke-java-version
	@echo temurin-11 > .java-version
	@echo "JAVA 11"
	@lein test

run-tests-java-21: nuke-java-version
	@echo temurin-21 > .java-version
	@echo "JAVA 21"
	@lein test


run-tests: run-tests-java-11 run-tests-java-21


publish:
	@lein deploy clojars
