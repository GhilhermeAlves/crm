package com.becommerce.crm.application.identity.port.output;

public interface EventPublisher {
    void publish(Object event);
}
