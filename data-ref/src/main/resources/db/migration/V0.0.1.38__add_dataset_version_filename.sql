ALTER TABLE dataset_version
    ADD COLUMN file_name varchar(150);

UPDATE dataset_version
SET file_name = 'Nom de fichier inconnu'
WHERE with_file = true;

ALTER TABLE dataset_version
    DROP COLUMN with_file;
