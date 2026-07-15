# Forms — Formulários e Inputs

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Padrão de Formulários](#padrão-de-formulários)
- [Componentes](#componentes)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o padrão de construção de formulários no frontend.

## Descrição

Formulários são construídos com React Hook Form + Zod para validação. Cada formulário é um componente dedicado com schema de validação definido.

## Padrão de Formulários

### Estrutura

```typescript
// 1. Schema de validação (Zod)
const createLeadSchema = z.object({
  firstName: z.string().min(1, 'Nome é obrigatório'),
  email: z.string().email('Email inválido'),
  phone: z.string().optional(),
  origin: z.enum(['WHATSAPP', 'FORM', 'API']),
});

// 2. Tipo inferido do schema
type CreateLeadFormData = z.infer<typeof createLeadSchema>;

// 3. Componente do formulário
export function LeadForm({ onSubmit }: LeadFormProps) {
  const form = useForm<CreateLeadFormData>({
    resolver: zodResolver(createLeadSchema),
    defaultValues: { firstName: '', email: '', phone: '' },
  });

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)}>
        <FormField name="firstName" label="Nome" />
        <FormField name="email" label="Email" type="email" />
        <Button type="submit">Salvar</Button>
      </form>
    </Form>
  );
}
```

## Componentes

| Componente | Descrição |
|---|---|
| Form | Wrapper do React Hook Form |
| FormField | Campo com label e erro |
| FormMessage | Mensagem de erro |
| FormDescription | Descrição do campo |
| Input | Campo de texto |
| Textarea | Área de texto |
| Select | Dropdown |
| DatePicker | Seletor de data |
| Checkbox | Checkbox |
| Switch | Toggle |
| RadioGroup | Radio buttons |

## Responsabilidades

- Validação em tempo real (onBlur)
- Validação no submit
- Exibição de erros claros
- Loading state durante submit
- Reset do formulário após sucesso
- Validação server-side (API errors)

## Dependências

- [Validation.md](./Validation.md) — Schemas de validação
- [Components.md](./Components.md) — Componentes base

## Regras

- Todo formulário deve ter schema Zod
- Erros devem ser exibidos abaixo do campo
- Campos obrigatórios devem ter asterisco (*)
- Submit deve desabilitar botão e mostrar loading
- Sucesso deve mostrar toast e/ou redirecionar
- Formulários longos devem ter seções

## Futuras Melhorias

- Autosave de formulários longos
- Validação cross-field
- Formulários multi-step (wizard)
- Campos dinâmicos baseados em backend
- Offline first (fill offline, sync online)

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
