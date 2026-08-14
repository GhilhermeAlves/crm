package com.becommerce.crm.domain.task.exception;

/**
 * Task não encontrada (ou de outra empresa). Resulta em HTTP 404.
 */
public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(java.util.UUID id) {
        super("Tarefa não encontrada: " + id);
    }
}