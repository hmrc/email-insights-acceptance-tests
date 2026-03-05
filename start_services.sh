#!/bin/bash -e

sm2 --start EMAIL_INSIGHTS_PROXY EMAIL_INSIGHTS EMAIL_GATEWAY DATASTREAM CIP_RISK INTERNAL_AUTH --appendArgs '{
        "EMAIL_INSIGHTS_PROXY": [
            "-Dauditing.consumer.baseUri.port=6001",
            "-Dauditing.consumer.baseUri.host=localhost",
            "-Dauditing.enabled=false",
            "-Dmicroservice.services.access-control.enabled=true",
            "-Dmicroservice.services.access-control.allow-list.0=email-gateway",
            "-Dmicroservice.services.access-control.allow-list.1=email-insights-acceptance-tests"
        ],
        "EMAIL_INSIGHTS": [
            "-Dapplication.router=testOnlyDoNotUseInAppConf.Routes",
            "-Ddb.emailinsights.url=jdbc:postgresql://localhost:5432/",
            "-Dplay.evolutions.db.emailinsights.autoApplyDowns=true",
            "-Dauditing.enabled=false"
        ],
        "CIP_RISK": [
            "-Dapplication.router=testOnlyDoNotUseInAppConf.Routes",
            "-Dplay.evolutions.db.risk.autoApplyDowns=true",
            "-Dauditing.enabled=false"
        ]
    }'