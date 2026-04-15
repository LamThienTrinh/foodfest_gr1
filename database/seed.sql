\set ON_ERROR_STOP on

\echo Applying seed migration (V3)...
\ir migrations/V3__seed_reference_data.sql
