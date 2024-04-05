CREATE USER dataref WITH LOGIN PASSWORD 'dataref';
CREATE SCHEMA dataref AUTHORIZATION dataref;
ALTER USER dataref set SEARCH_PATH = 'dataref';

CREATE USER datavirt WITH LOGIN PASSWORD 'datavirt';
GRANT ALL ON SCHEMA public TO datavirt;

