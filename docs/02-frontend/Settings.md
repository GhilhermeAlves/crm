# Settings — Configurações (Frontend)

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Páginas](#páginas)
- [Componentes](#componentes)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar as páginas de configurações do CRM.

## Descrição

As configurações permitem ao administrador gerenciar empresa, usuários, integrações e preferências do sistema.

## Páginas

### Perfil (`/settings/profile`)

- Dados pessoais (nome, email, avatar)
- Troca de senha
- Preferências de notificação

### Empresa (`/settings/company`)

- Dados da empresa
- Logo e branding
- Configurações de fuso horário e idioma

### Usuários (`/settings/users`)

- Lista de usuários
- Convites
- Gestão de roles

### Integrações (`/settings/integrations`)

- WhatsApp (Evolution API)
- Email (SMTP)
- Google Calendar
- OpenAI

### Faturamento (`/settings/billing`)

- Plano atual
- Uso de recursos
- Histórico de pagamentos

## Componentes

| Componente | Descrição |
|---|---|
| SettingsLayout | Layout com tabs |
| ProfileForm | Formulário de perfil |
| CompanyForm | Formulário da empresa |
| UsersTable | Tabela de usuários |
| InviteDialog | Modal de convite |
| IntegrationsList | Lista de integrações |
| BillingCard | Card do plano |

## Responsabilidades

- Gerenciar dados do usuário e empresa
- Configurar integrações
- Gerenciar equipe
- Visualizar plano e faturamento

## Dependências

- [01-backend/Users.md](../01-backend/Users.md) — API de usuários
- [01-backend/Companies.md](../01-backend/Companies.md) — API de empresa
- [01-backend/Auth.md](../01-backend/Auth.md) — Autenticação

## Regras

- Apenas admin pode acessar config da empresa
- Apenas admin pode gerenciar usuários
- Dados do plano são read-only
- Integrações precisam de configuração para funcionar

## Futuras Melhorias

- Configurações avançadas de automação
- Customização de temas por empresa
- API keys management
- Audit log de configurações
- Backup e restore de configurações

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
