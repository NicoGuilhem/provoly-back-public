ALTER TABLE category
    ADD COLUMN with_category_entity_type varchar(20);

UPDATE category SET with_category_entity_type = 'ATTRIBUTES';

CREATE TABLE category_relations (
     id                             uuid primary key,
     entity_id                      uuid not null,
     category_id                    uuid not null references category,
     unique (entity_id, category_id)
);

ALTER TABLE category ALTER COLUMN with_category_entity_type SET NOT NULL;

ALTER TABLE attribute_def DROP COLUMN category_id;