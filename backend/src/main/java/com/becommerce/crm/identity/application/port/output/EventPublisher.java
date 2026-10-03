package com.becommerce.crm.identity.application.port.output;

public interface EventPublisher {
    void publish(Object event);
}
