package com.provoly.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used by mapstruct to determine which constructor to use when there are multiple ones in a class.
 *
 * 💀https://mapstruct.org/documentation/stable/reference/html/#mapping-with-constructors LOL
 */
@Target(ElementType.CONSTRUCTOR)
@Retention(RetentionPolicy.CLASS)
public @interface Default {
}
