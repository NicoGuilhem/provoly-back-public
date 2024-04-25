package com.provoly.common.user;

public enum Role {

    //Role Enum needs to be the same value as the String value provided by oidc provider
    CLASS_READ,
    CLASS_WRITE,
    DASHBOARD_READ,
    DASHBOARD_WRITE,
    DATASET_READ,
    DATASET_WRITE,
    DATASOURCE_READ,
    DATA_ACCESS_READ,
    DATA_ACCESS_WRITE,
    DATA_VIRT,
    FIELD_READ,
    FIELD_WRITE,
    INFRA_READ,
    INFRA_WRITE,
    INTEG_ERROR_READ,
    INTEG_ERROR_WRITE,
    ITEM_WRITE,
    JOB_EXECUTION_READ,
    JOB_INSTANCE_READ,
    JOB_INSTANCE_WRITE,
    JOB_MODEL_READ,
    JOB_MODEL_WRITE,
    JOB_START,
    LINK_READ,
    LINK_WRITE,
    METADATA_CONTEXT_READ,
    METADATA_CONTEXT_WRITE,
    METADATA_ITEM_REF_READ,
    METADATA_ITEM_REF_WRITE,
    METADATA_USER_READ,
    METADATA_USER_REF_READ,
    METADATA_USER_REF_WRITE,
    METADATA_USER_WRITE,
    NOTIFICATION_READ,
    NOTIFICATION_WRITE,
    SEARCH,
    STREAM_READ,
    UPDATE_RELATION_AGGREGATE,
    USER_READ,
    USER_WRITE,
    WIDGET_CATALOG_READ,
    WIDGET_CATALOG_WRITE,
    ADMINISTRATE;

    // We declare role as String, because enum cannot be used in RoleAllowed annotation
    public static final String STR_CLASS_READ = "class_read";
    public static final String STR_CLASS_READ_NOTIFICATION = "notification_read";
    public static final String STR_CLASS_WRITE = "class_write";
    public static final String STR_CLASS_WRITE_NOTIFICATION = "notification_write";
    public static final String STR_DASHBOARD_READ = "dashboard_read";
    public static final String STR_DASHBOARD_WRITE = "dashboard_write";
    public static final String STR_DATASET_READ = "dataset_read";
    public static final String STR_DATASET_WRITE = "dataset_write";
    public static final String STR_DATASOURCE_READ = "datasource_read";
    public static final String STR_DATA_ACCESS_READ = "data_access_read";
    public static final String STR_DATA_ACCESS_WRITE = "data_access_write";
    public static final String STR_DATA_VIRT = "data_virt";
    public static final String STR_FIELD_READ = "field_read";
    public static final String STR_FIELD_WRITE = "field_write";
    public static final String STR_INFRA_READ = "infra_read";
    public static final String STR_INFRA_WRITE = "infra_write";
    public static final String STR_INTEG_ERROR_READ = "integ_error_read";
    public static final String STR_INTEG_ERROR_WRITE = "integ_error_write";
    public static final String STR_ITEM_WRITE = "item_write";
    public static final String STR_JOB_EXECUTION_READ = "job_execution_read";
    public static final String STR_JOB_INSTANCE_READ = "job_instance_read";
    public static final String STR_JOB_INSTANCE_WRITE = "job_instance_write";
    public static final String STR_JOB_MODEL_READ = "job_model_read";
    public static final String STR_JOB_MODEL_WRITE = "job_model_write";
    public static final String STR_JOB_START = "job_start";
    public static final String STR_LINK_READ = "link_read";
    public static final String STR_LINK_WRITE = "link_write";
    public static final String STR_METADATA_CONTEXT_READ = "metadata_context_read";
    public static final String STR_METADATA_CONTEXT_WRITE = "metadata_context_write";
    public static final String STR_METADATA_ITEM_REF_READ = "metadata_item_ref_read";
    public static final String STR_METADATA_ITEM_REF_WRITE = "metadata_item_ref_write";
    public static final String STR_METADATA_USER_READ = "metadata_user_read";
    public static final String STR_METADATA_USER_REF_READ = "metadata_user_ref_read";
    public static final String STR_METADATA_USER_REF_WRITE = "metadata_user_ref_write";
    public static final String STR_METADATA_USER_WRITE = "metadata_user_write";
    public static final String STR_SEARCH = "search";
    public static final String STR_STREAM_READ = "stream_read";
    public static final String STR_UPDATE_RELATION_AGGREGATE = "update_relation_aggregate";
    public static final String STR_USER_READ = "user_read";
    public static final String STR_USER_WRITE = "user_write";
    public static final String STR_WIDGET_CATALOG_READ = "widget_catalog_read";
    public static final String STR_WIDGET_CATALOG_WRITE = "widget_catalog_write";
    public static final String STR_ADMINISTRATE = "administrate";

    public static boolean exists(String role) {
        for (Role value : values()) {
            if (value.name().toLowerCase().equals(role)) {
                return true;
            }
        }
        return false;
    }

}
