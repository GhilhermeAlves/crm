# Instruções de Segurança do Workspace

## Repositórios do GitHub

Antes de clonar, baixar, extrair, instalar ou executar qualquer conteúdo vindo de um repositório do GitHub, faça uma verificação de segurança prévia.

A verificação deve incluir:

- Consultar os alertas públicos do repositório no GitHub, incluindo Security Advisories, malware reports, issues de segurança e sinais de comprometimento recentes.
- Confirmar a origem, o proprietário, a atividade recente e a integridade do repositório antes de usá-lo.
- Preferir inspeção remota de metadados e arquivos de configuração; não executar scripts, binários, notebooks ou documentação com conteúdo executável durante a triagem.
- Depois de qualquer download autorizado, fazer uma varredura local com o antivírus disponível no Windows antes de abrir ou executar os arquivos.
- Inspecionar especialmente scripts de instalação, workflows, hooks Git, binários, arquivos compactados, notebooks e comandos copiados da documentação.

Se houver qualquer alerta de vírus, malware, arquivo suspeito, comprometimento, advisory relevante ou impossibilidade de verificar a segurança, interrompa o processo. Não baixe nem execute o repositório e informe o motivo ao usuário; só prossiga após confirmação explícita do usuário e uma alternativa de mitigação documentada.

Essa regra também se aplica a links de documentação hospedados no GitHub e a dependências obtidas por instruções presentes nesses repositórios.
