package jdk.internal.jvmci.hotspot;

import java.lang.annotation.*;

/**
 * Method annotated by this will not be inlined. Duplicate of java.lang.invoke.DontInline
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DontInline {
}