ALTER TABLE widget_catalog
    ADD COLUMN user_id uuid references provoly_user;

-- migration owner widget
UPDATE widget_catalog as wc
SET user_id = po.user_id
    FROM provoly_user_widget_catalog AS po
WHERE wc.id = po.widget_catalog_id
  AND po.owner = true;

-- add constraint on user_id column
ALTER TABLE widget_catalog ALTER COLUMN user_id SET NOT NULL;

-- migration visibility into group_relations for widget
INSERT INTO group_relations (id, entity_id, entity_type, group_id, can_write)
SELECT gen_random_uuid(), widget.id, 'WIDGET', gd.id, false
FROM widget_catalog AS widget, group_def AS gd
WHERE widget.visibility_type = 'PUBLIC' AND gd.name = 'ALL';

ALTER TABLE widget_catalog DROP COLUMN visibility_type;
