create table dashboard_datasource (
                                dashboard_id uuid not null references dashboard,
                                datasource uuid not null
);