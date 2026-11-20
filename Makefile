.PHONY: run test reset seed smoke

run:    ## start the app with DevTools reload on :8080
	mvn spring-boot:run

test:   ## full MockMvc suite against in-memory H2
	mvn -q test

reset:  ## delete the dev DB (stop the app first); schema is recreated on next start
	./reset-db.sh

seed:   ## add sample rows (app must be running)
	@for n in alpha beta gamma; do \
	  curl -s -o /dev/null -w "%{http_code} $$n\n" -X POST localhost:8080/api/items \
	    -H 'Content-Type: application/json' -d "{\"name\":\"$$n\",\"description\":\"seed\"}"; \
	done

smoke:  ## curl every endpoint and status code (app must be running)
	./smoke-test.sh
