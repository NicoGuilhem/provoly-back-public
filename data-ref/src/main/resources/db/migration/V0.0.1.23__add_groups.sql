CREATE TABLE group_def (
    id                      uuid primary key,
    name                    varchar(100) unique not null,
    system                  boolean default false
);

CREATE TABLE group_relations (
    id                      uuid primary key,
    entity_id               uuid not null,
    entity_type             varchar(20) not null,
    group_id                uuid not null references group_def,
    unique (group_id, entity_id)
);

INSERT INTO group_def (id, name, system)
VALUES
    ('a72c0c63-c871-4515-848f-4a15e5c1f8a6', 'ALL', true),
    ('c9448698-ed9c-4416-b85b-24b8f9fed118', 'CONNECTED', true);
