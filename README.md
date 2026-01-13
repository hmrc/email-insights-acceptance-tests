# email-insights-acceptance-tests

This repository contains api acceptance tests for the Email Insights service built using the [api-test-runner](https://github.com/hmrc/api-test-runner) library.

## Running the tests

Prior to executing the tests ensure you have:

- Installed/configured [sm2 (service manager 2)](https://github.com/hmrc/sm2).
- Postgres DB installed locally or running in Docker.

### Start the local services

If you don't have mongodb installed locally you can run it in docker using the following commands:

```bash
    docker run --rm -d -p 27017:27017 --name mongo percona/percona-server-mongodb:7.0
```

If you don't have postgres installed locally you can run it in docker using the following command

```bash
    docker run -d --rm --name postgresql -e POSTGRES_DB=emailinsights -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:latest
```

Start the dependent services by running the `./start_services.sh` script.

Or run the following command:

```bash
    sm2 --start EMAIL_INSIGHTS_PROXY EMAIL_INSIGHTS EMAIL_GATEWAY CIP_RISK INTERNAL_AUTH --appendArgs '{
        "EMAIL_INSIGHTS_PROXY": [
            "-J-Dauditing.consumer.baseUri.port=6001",
            "-J-Dauditing.consumer.baseUri.host=localhost",
            "-J-Dauditing.enabled=false",
            "-J-Dmicroservice.services.access-control.enabled=true",
            "-J-Dmicroservice.services.access-control.allow-list.0=email-gateway",
            "-J-Dmicroservice.services.access-control.allow-list.1=email-insights-acceptance-tests"
        ],
        "EMAIL_INSIGHTS": [
            "-J-Dapplication.router=testOnlyDoNotUseInAppConf.Routes",
            "-J-Ddb.emailinsights.url=jdbc:postgresql://localhost:5432/",
            "-J-Dauditing.enabled=false"
        ],
        "CIP_RISK": [
            "-J-Dauditing.enabled=false"
        ]
    }'
```

### Running specs

Execute the `run_tests.sh` script:

`./run_tests.sh`

## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
