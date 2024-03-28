ALTER TABLE attribute_def ADD COLUMN technical_name varchar(100);
ALTER TABLE attribute_def ADD CONSTRAINT attribute_def_oclass_id_technical_name_key UNIQUE (oclass_id, technical_name);
UPDATE attribute_def SET technical_name = name;

WITH duplicate_pair_count AS (
    SELECT SUBSTR(name, 1,49) as truncated_name, oclass_id, COUNT(*) AS duplicate_count
    FROM attribute_def
    GROUP BY truncated_name, oclass_id
    HAVING COUNT(*) > 1
),

duplicate_with_row_num AS (
    SELECT
        id,
        attribute_def.oclass_id,
        name,
        truncated_name,
        ROW_NUMBER() OVER (PARTITION BY attribute_def.oclass_id, truncated_name ) AS row_num
    FROM attribute_def
    RIGHT JOIN duplicate_pair_count dpc ON attribute_def.oclass_id = dpc.oclass_id
    WHERE attribute_def.name LIKE CONCAT(dpc.truncated_name, '%')
    ORDER BY oclass_id, name
)

UPDATE attribute_def
SET name =  CONCAT(SUBSTR(attribute_def.name, 1,49), row_num)
FROM duplicate_with_row_num WHERE attribute_def.id = duplicate_with_row_num.id;

ALTER TABLE attribute_def ALTER COLUMN name TYPE VARCHAR(50) USING SUBSTR(name, 1, 50);