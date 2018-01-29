package com.rationaleemotions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Reporters {
    /**
     * @return what plugins(s) to use
     */
    String[] plugin() default {};

}
