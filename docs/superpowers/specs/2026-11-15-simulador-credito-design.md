# Simulador de vendas no crédito — desenho

Data: 2026-11-15 · Branch: `feature/simulador-credito`

## Objetivo

Na hora de fechar um orçamento, mostrar quanto fica cada opção de parcelamento no cartão de crédito (1x a 12x): a parcela e o total para o paciente, e quanto a clínica recebe de fato depois da taxa da maquininha. Consulta apenas — nada é gravado no orçamento.

## Escopo

Dentro:
- Taxas do cartão por clínica (1x–12x), cadastradas uma vez e ajustáveis na simulação.
- Modelos de taxa globais (maquininhas do mercado), mantidos pelo CRM_ADMIN, usados como ponto de partida.
- Simulador no orçamento do paciente (botão) e como ferramenta solta no menu Financeiro.

Fora (por ora): débito/Pix, bandeiras diferentes, gravar a opção escolhida no orçamento/impressão, busca automática de taxas.

## Cálculo (`frontend-refine/src/features/finance/credit/utils/creditSimulation.ts`)

Função pura. Entrada: valor em centavos, taxas[1..12] em %, `repassar: boolean`. Saída por linha N:

- Sem repasse: `totalPaciente = valor`; `liquido = round(valor × (1 − taxa))`.
- Com repasse: `totalPaciente = round(valor ÷ (1 − taxa))` (a clínica recebe o valor cheio); `liquido = round(totalPaciente × (1 − taxa))`.
- `parcela = floor(totalPaciente ÷ N)`, resto na 1ª parcela (mesma regra de `splitInstallments`); exibe a parcela padrão e, se houver resto, "1ª de R$ x".
- `custo = totalPaciente − liquido`.
- Taxa válida: 0 ≤ taxa < 100. Taxa vazia/ausente = linha mostrada como "sem taxa" (não calculada).

## Dados (backend)

Migration nova (próximo número livre após V138, conferir colisão):

- `card_fee_rates` — por clínica: `company_id`, `installments` (1–12), `rate_percent NUMERIC(5,2)` (0 ≤ x < 100), `updated_at`, PK (`company_id`, `installments`). RLS no padrão das tabelas por clínica (empresa ativa | dono do CRM) + GRANT ao `crm_app`.
- `card_fee_presets` — global, sem RLS por empresa: `id`, `name` (ex.: "InfinitePay — 1 dia útil — até R$ 20 mil/mês"), `rates JSONB` (12 valores) ou tabela filha, `estimated BOOLEAN` (parcelas intermediárias estimadas), `checked_at DATE`, `source_note`, `active`. SELECT para todos os usuários autenticados; escrita só CRM_ADMIN (checada no backend; GRANT ao `crm_app`).
- Carga inicial dos modelos na própria migration (seção abaixo).

Endpoints:
- `GET /api/v1/finance/card-fees` → 12 taxas da clínica (lista vazia se nunca cadastrou). Qualquer usuário da clínica.
- `PUT /api/v1/finance/card-fees` → grava as 12 de uma vez. Admin da clínica ou CRM_ADMIN.
- `GET /api/v1/finance/card-fee-presets` → modelos ativos. Qualquer usuário autenticado.
- `POST/PUT/DELETE /api/v1/admin/card-fee-presets[/{id}]` → só CRM_ADMIN.

Validação: 12 linhas, `installments` 1–12 sem repetição, taxa 0 ≤ x < 100 com 2 casas.

## Frontend (`frontend-refine`)

- **Configurações → Financeiro → Taxas do cartão**: 12 campos %; botão "Usar modelo" (seletor dos modelos, preenche os campos, mostra aviso de data/estimado); Salvar.
- **`CreditSimulator`** (componente): props `amountCents`, opcional `editableAmount`. Busca taxas da clínica; chave "Repassar juros ao paciente"; tabela 1x–12x (parcela, total paciente, taxa, líquido clínica, custo); taxas editáveis na tabela só para a sessão, "Usar modelo" e "Voltar às taxas cadastradas". Clínica sem taxas: campos zerados + link para o cadastro.
- **Orçamento**: botão "Simular no crédito" (`BudgetViewDialog` / lista) abre o simulador em diálogo com o total do orçamento.
- **Menu Financeiro → Simulador de crédito**: página com campo de valor + `CreditSimulator`.
- **Admin (CRM_ADMIN) → Modelos de taxa**: lista, criar/editar (nome, 12 taxas, estimado, data conferida, observação), desativar.

Aviso fixo em todo modelo: "Taxas de referência conferidas em dd/mm/aaaa — confirme com a sua maquininha." Se `estimated`: "parcelas 2x–11x estimadas".

## Carga inicial dos modelos (estimada)

Fonte: maquinatop.com.br (comparador afiliado), conferida em nov/2026, faixa de menor faturamento após período promocional. Publicam só 1x e 12x; 2x–11x preenchidos por interpolação linear entre 1x e 12x, `estimated = true`, `checked_at = 2026-11-13`.

| Modelo | 1x | 12x |
|---|---|---|
| InfinitePay — 1 dia útil — até R$ 20 mil/mês | 3,15 | 12,40 |
| InfinitePay — na hora (Nitro) | 5,99 | 18,79 |
| SumUp — 1 dia útil — até R$ 20 mil/mês | 3,49 | 13,99 |
| PagBank — na hora | 4,99 | 16,92 |
| Cielo — tabela padrão | 4,30 | 19,00 |
| Mercado Pago — na hora — até R$ 3 mil/mês | 3,86 | 20,39 |
| Ton — 1 dia útil — até R$ 3 mil/mês | 3,86 | 21,39 |
| Ton — na hora — até R$ 3 mil/mês | 4,86 | 21,99 |

Interpolação: `taxa(N) = round2(t1 + (t12 − t1) × (N − 1) / 11)`. Stone fica fora (taxa negociada).

## Testes

- Unitários do cálculo: sem/com repasse, arredondamento e resto da parcela, taxa 0, taxa inválida.
- Componente `CreditSimulator`: renderiza 12 linhas, troca de repasse, edição de taxa local, "Voltar às taxas cadastradas".
- Backend: validação (12 linhas, faixas), permissão (usuário comum não grava; admin da clínica grava; CRM_ADMIN grava; só CRM_ADMIN mexe em modelos), RLS (clínica não lê taxas de outra). O usuário roda os testes de backend.
- Prettier em todo arquivo do frontend alterado.
