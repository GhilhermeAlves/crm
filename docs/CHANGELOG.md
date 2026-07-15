# CHANGELOG

## [1.1.0] - 2026-07-15

### Fixed
- Reports.md: Rebuilt corrupted endpoints section
- Dashboard.md: Removed circular dependency with Reports.md
- Reports.md: Removed self-reference and circular dependency
- integrations/README.md: Fixed broken reference to non-existent file
- Lead.md: Fixed Chinese text "业务需求" replaced with Portuguese
- Routing.md: Removed /register route (no backend endpoint)
- Kanban.md: Replaced deprecated react-beautiful-dnd with @dnd-kit/core
- Context.md: Fixed WebSocketProvider and NotificationProvider locations
- Overview.md (frontend): Removed unused Zustand from stack
- Auth.md: Fixed broken reference to non-existent 11-security/ directory
- SECURITY_MAP.md: Fixed broken reference to 01-backend/Permissions.md
- AI.md: Fixed misleading link text [Communication.md] pointing to Conversations.md
- Customers.md: Fixed misleading link text [Communication.md] pointing to Conversations.md
- Contacts.md: Fixed misleading link text [Communication.md] pointing to Conversations.md
- PROJECT_INDEX.md: Corrected file counts, added Security.md, Permissions.md, REVIEW.md
- BACKEND_MAP.md: Replaced Chinese "常量" with "constantes"
- DATABASE_MAP.md: Replaced Chinese "隔离" (2x) with "isolation"
- 00-core/Security.md: Replaced Chinese "最小权限" with "menor privilégio"
- 00-core/Vision.md: Replaced Chinese "部署" with "deploy"
- SUMMARY.md: Removed Zustand from frontend stack (replaced with React Context)
- 02-frontend/Hooks.md: Updated state management guidance (removed Zustand)
- FILE_LIFECYCLE.md: Fixed invalid Mermaid node syntax
- DATA_FLOW.md: Added missing `participant RD as Redis` declaration
- 03-database/Entities.md: Added 8 missing ERD tables (company_settings, tags, contact_tags, pipelines, stages, opportunity_history, conversations, audit_logs)
- 01-backend/README.md: Added Permissions.md to table of contents
- 35 files: Fixed 55 broken TOC anchor links (Unicode normalization + missing headings)
- 8 empty directories removed (08-history through 15-automation-docs)

### Added
- Lead.md: Complete scoring formula with weights and examples
- Automations.md: Complete condition system with operators and examples
- WhatsApp.md: Template Messages documentation with categories, variables, buttons, rate limits
- Overview.md (backend): CORS configuration, security headers, HTTPS rules
- Reports.md: Numbered rules format (R-001 to R-006)
- Automations.md: Numbered rules format (A-001 to A-011)
- 00-core/Security.md: Complete security guidelines (OWASP, LGPD, encryption, RBAC)
- 01-backend/Permissions.md: RBAC backend with roles, permissions matrix, hierarchy
- 03-database/Entities.md: 11 missing tables (message_templates, message_attachments, analytics_events, leads, campaigns, campaign_steps, automation_triggers, automation_actions, roles, user_roles, subscriptions, contact_addresses, contact_custom_fields, events)
- 06-devops/Docker.md: RabbitMQ HA (cluster, quorum queues) and Redis HA (sentinel, cluster mode)

### Changed
- Dashboard.md: Changed dependency from Reports.md to Events.md and Cache.md
- Reports.md: Changed dependencies to Cache.md, FileStorage.md, Scheduler.md, Events.md
- Context.md: Moved WebSocketProvider and NotificationProvider to dedicated provider files

### New Documents
- DOMAIN_MODEL.md
- EVENT_MAP.md
- WORKFLOWS.md
- STATE_MACHINES.md
- MULTI_TENANCY.md
- BILLING_MODEL.md
- FEATURE_FLAGS.md
- CACHE_STRATEGY.md
- QUEUE_ARCHITECTURE.md
- WEBSOCKET_ARCHITECTURE.md
- SEARCH_ARCHITECTURE.md
- OBSERVABILITY.md
- BACKUP_RECOVERY.md
- API_VERSIONING.md
- ERROR_HANDLING.md
- NOTIFICATION_ARCHITECTURE.md
- SCHEDULER.md
- FILE_LIFECYCLE.md
- LGPD.md
- CHANGELOG.md
