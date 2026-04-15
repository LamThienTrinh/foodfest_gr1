# Database Migration Guide

This folder now uses incremental SQL migrations.

## Files

- migrations/V1__create_core_schema.sql: base schema only (no sample data)
- migrations/V2__add_social_tables_and_backfill.sql: social tables/columns and backfill
- migrations/V3__seed_reference_data.sql: optional sample tags/dishes
- migrations/V4__add_comment_threading_2_levels.sql: two-level comments (parent/reply_count/depth + indexes)
- migrations/V5__add_public_profile_stats_view_and_indexes.sql: public-profile lookup indexes + v_user_profile_stats view
- migrations/V6__add_family_space_core_tables.sql: family groups/members/menus/items/votes core schema + owner integrity triggers
- run-migrations.ps1: applies pending migrations in version order
- schema.sql: applies schema migrations only (V1 + V2 + V4 + V5 + V6)
- seed.sql: applies seed migration only (V3)

## Recommended flow (Windows PowerShell)

From repository root:

1. Create database:

createdb foodfest

2. Apply all pending migrations with version tracking:

./database/run-migrations.ps1 -DbHost 127.0.0.1 -Database foodfest -User postgres -Password postgres

2b. Apply only schema migrations (skip any seed migration files):

./database/run-migrations.ps1 -DbHost 127.0.0.1 -Database foodfest -User postgres -Password postgres -SkipSeed

3. Optional: apply seed only (if not using run-migrations and you need demo data):

psql -d foodfest -f database/seed.sql

## Notes

- Migration history is stored in table schema_migrations.
- Do not edit old migration files after they are applied.
- Add new changes as new files: V4__..., V5__..., V6__..., etc.
