#!/bin/bash
echo "Loading temp system variables to current bash session..."
export IS_DEV=true
export CONFIG_FILE=monashIT_testing.json,wired_production.json
export DATABASE_URL=null
export DISCORD_CLIENT_SECRET=null
export GOOGLE_SSO_CLIENT_ID=null
export GOOGLE_SSO_CLIENT_SECRET=null
export GRADLE_TASK=build
export GRADLE_TASK=null
export PRODUCTION_ENV=false
export TWITTER_ACCESS_SECRET=null
export TWITTER_ACCESS_TOKEN=null
export TWITTER_CONSUMER_KEY=null
export TWITTER_CONSUMER_SECRET=null
export TZ=Australia/Melbourne
echo "Done!"