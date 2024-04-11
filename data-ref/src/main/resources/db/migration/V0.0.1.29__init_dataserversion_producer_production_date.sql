WITH dv_ids as (SELECT dv.id from dataset_version as dv LEFT JOIN dataset ON dv.id = dataset.id WHERE dataset.type = 'CLOSED')
UPDATE dataset_version SET producer = 'onepoint' WHERE producer IS NULL AND dataset_version.id IN (SELECT * FROM dv_ids);

WITH dv_ids as (SELECT dv.id from dataset_version as dv LEFT JOIN dataset ON dv.id = dataset.id WHERE dataset.type = 'CLOSED')
UPDATE dataset_version SET production_date= last_modified WHERE production_date IS NULL AND dataset_version.id IN (SELECT * FROM dv_ids) ;