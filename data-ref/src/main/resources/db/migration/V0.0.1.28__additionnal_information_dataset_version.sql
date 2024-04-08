ALTER TABLE dataset_version
    ADD COLUMN production_date timestamp ,
    ADD COLUMN producer varchar(100),
    ADD COLUMN additional_information varchar