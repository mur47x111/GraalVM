package com.oracle.graal.debug.external;

import java.lang.annotation.*;

/*
 * Method annotated by this annotation will not be inlined.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DontInline {
}