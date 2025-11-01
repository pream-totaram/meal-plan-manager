#!/bin/bash


podman run \
  --name appdb \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_DB=meal_plan_db \
  -p 5432:5432 \
  -d postgres
