\set ON_ERROR_STOP on

\echo Applying schema migrations (V1, V2, V4, V5, V6)...
\ir migrations/V1__create_core_schema.sql
\ir migrations/V2__add_social_tables_and_backfill.sql
\ir migrations/V4__add_comment_threading_2_levels.sql
\ir migrations/V5__add_public_profile_stats_view_and_indexes.sql
\ir migrations/V6__add_family_space_core_tables.sql
