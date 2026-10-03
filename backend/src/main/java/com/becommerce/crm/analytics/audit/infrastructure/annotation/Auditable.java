package com.becommerce.crm.analytics.audit.infrastructure.annotation;

import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {
    AuditAction action();
    AuditModule module();
    String description() default "";
    String entityId() default "";
    String entityName() default "";
}
