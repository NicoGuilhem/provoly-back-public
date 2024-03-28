
create table job_model
(
    id uuid primary key,
    image varchar(400) not null
);

create table job_model_parameters
(
    job_model_id uuid not null references job_model,
    name varchar(100) not null,
    filename varchar(400)
);

create table job_instance
(
    id uuid primary key,
    active boolean not null,
    model_id uuid not null references job_model
);

create table job_instance_in_data_sources (
    job_instance_id uuid not null references job_instance,
    method varchar(25) not null,
    data_source_id uuid not null
);

create table job_instance_out_datasets (
   job_instance_id uuid not null references job_instance,
   method varchar(25) not null,
   dataset_id uuid not null
);

create table job_instance_parameters_value (
   job_instance_id uuid not null references job_instance,
   name varchar(100) not null,
   value text not null
);

create table job_execution (
    id uuid primary key,
    instance_id uuid not null references job_instance,
    status varchar(20),
    execution_date timestamp
);

insert into job_model (id, image)
values ('423b5c01-1816-41d4-b358-000000000000',
        'dh2wltsh.gra7.container-registry.ovh.net/provoly/provoly-transfo-runner:0.3.0');

insert into job_model_parameters (job_model_id, name, filename)
values ('423b5c01-1816-41d4-b358-000000000000', 'transformation_file', 'transfo.json');