create table mono_class_search_request_dataset_version_ids (
        mono_class_search_request_id uuid not null references mono_class_search_request,
        dataset_version_ids uuid not null references dataset_version
);