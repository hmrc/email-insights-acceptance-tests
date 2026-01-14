#!/bin/bash -e

sm2 --start EMAIL_INSIGHTS_PROXY EMAIL_INSIGHTS EMAIL_GATEWAY DATASTREAM CIP_RISK INTERNAL_AUTH --appendArgs '{
        "EMAIL_INSIGHTS_PROXY": [
            "-J-Dauditing.enabled=true",
            "-J-Dmicroservice.services.access-control.enabled=true",
            "-J-Dmicroservice.services.access-control.allow-list.0=email-gateway",
            "-J-Dmicroservice.services.access-control.allow-list.1=email-insights-acceptance-tests"
        ],
        "EMAIL_INSIGHTS": [
            "-J-Dapplication.router=testOnlyDoNotUseInAppConf.Routes",
            "-J-Ddb.emailinsights.url=jdbc:postgresql://localhost:5432/",
            "-J-Dauditing.enabled=true"
        ],
        "CIP_RISK": [
            "-J-Dauditing.enabled=true"
        ]
    }'