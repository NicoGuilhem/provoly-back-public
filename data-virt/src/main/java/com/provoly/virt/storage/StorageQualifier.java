package com.provoly.virt.storage;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import com.provoly.common.Storage;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD })
public @interface StorageQualifier {

    /**
     * Storage Value of Adapter
     */
    Storage value();

    class StorageLiteral extends AnnotationLiteral<StorageQualifier> implements StorageQualifier {

        private static final long serialVersionUID = 1L;

        private final Storage value;

        public static StorageLiteral of(Storage value) {
            return new StorageLiteral(value);
        }

        public Storage value() {
            return value;
        }

        private StorageLiteral(Storage value) {
            this.value = value;
        }
    }

}
