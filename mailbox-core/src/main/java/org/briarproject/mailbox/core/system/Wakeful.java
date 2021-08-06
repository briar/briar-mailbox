package org.briarproject.mailbox.core.system;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * Annotation for methods that must be called while holding a wake lock, if
 * the platform supports wake locks.
 */
@Qualifier
@Target(METHOD)
@Retention(RUNTIME)
public @interface Wakeful {
}