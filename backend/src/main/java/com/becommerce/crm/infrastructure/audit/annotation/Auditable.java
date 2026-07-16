package com.becommerce.crm.infrastructure.audit.annotation;

import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditModule;

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
