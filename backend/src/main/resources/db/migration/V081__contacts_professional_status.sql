-- V081__contacts_professional_status.sql
-- Add professional_status column to contacts table

ALTER TABLE contacts
    ADD COLUMN professional_status VARCHAR(20);
