# Documento de Requisitos — Orbytum

**Data:** 30/06/2026 — **Revisado em:** 23/11/2026  
**Fontes:** `specs_requisitos.md` (primária), `specs_requisitos_antigos.md` (secundária), `artigo/` (terciária)

---

## 1. Autenticação e Login

| Nº | Descrição | Classificação | Recursos Afetados / Impactos |
| :-- | :-------- | :------------ | :--------------------------- |
| REQ-001 | Se não houver administradores ao subir o sistema, criar um administrador inicial automaticamente | Não Funcional | Organização — depende da existência de admin para criar organizações |
| REQ-002 | O admin inicial deve ser usado apenas para criar o primeiro administrador e depois ser excluído | Funcional | Organização — fluxo de criação da primeira organização |
| REQ-003 | Administrador pode criar grupos de pesquisa, editar os dados de qualquer grupo e cadastrar líderes | Funcional | Organização, Grupos de Pesquisa — permissão de criação interfere diretamente nestes recursos |
| REQ-004 | O Administrador adiciona membros (pesquisadores e colaboradores) aos grupos de pesquisa, por convite ou autocadastro via link | Funcional | Grupos de Pesquisa — define quem pode ingressar no grupo |
| REQ-006 | Colaborador possui acesso somente leitura, restrito aos projetos em que foi incluído; Pesquisador possui o menor nível de permissões de escrita | Não Funcional | Todos os recursos — restrições de acesso baseadas no papel |
| REQ-007 | Todo usuário deve conter um campo indicando quem permitiu seu cadastro | Não Funcional | Grupos de Pesquisa — rastreabilidade de ingresso |
| REQ-008 | Todas as senhas devem ser criptografadas no banco utilizando Argon2 | Não Funcional | Nenhum — requisito transversal de segurança |
| REQ-009 | Utilizar token JWT para acesso com os payloads: username, role, iat | Não Funcional | Todos os recursos — mecanismo de autenticação global |

---

## 2. Administração

| Nº | Descrição | Classificação | Recursos Afetados / Impactos |
| :-- | :-------- | :------------ | :--------------------------- |
| REQ-010 | Administrador pode criar novos administradores e permitir o cadastro de líderes nos grupos de pesquisa | Funcional | Grupos de Pesquisa, Autenticação — organizações são o contêiner dos grupos |
| REQ-011 | Todo administrador pode realizar operações de CRUD em todos os grupos de pesquisa, independentemente de quem os criou | Não Funcional | Grupos de Pesquisa — administração compartilhada entre administradores |

---

## 3. Grupos de Pesquisa

| Nº | Descrição | Classificação | Recursos Afetados / Impactos |
| :-- | :-------- | :------------ | :--------------------------- |
| REQ-012 | CRUD de grupos de pesquisa | Funcional | Organização, Projetos — grupos são vinculados a organizações e geram projetos |
| REQ-013 | Ao criar um grupo de pesquisa, deve-se associar um líder obrigatoriamente | Funcional | Autenticação — o líder é um papel de usuário |
| REQ-014 | Cada organização utiliza uma instância própria do sistema; o grupo de pesquisa é a entidade de mais alto nível | Não Funcional | Organização — dependência hierárquica |
| REQ-015 | Perfis do sistema: Líder do grupo (todas as permissões no grupo), Líder de projeto (gerencia atividades, lembretes e publicações do projeto e aprova notícias rotuladas com ele), Pesquisador (interage com atividades e cria atividades e notícias sujeitas a aprovação) e Colaborador (somente leitura nos projetos em que foi incluído) | Não Funcional | Todos os recursos — hierarquia de acesso transversal |
| REQ-016 | Líder pode ter um ou mais grupos de pesquisa; cada grupo tem apenas um líder | Não Funcional | Autenticação — relação líder/grupo |
| REQ-018 | Pesquisador pode ser múltiplo por grupo e participar de vários grupos | Não Funcional | Autenticação — vínculo pesquisador/grupo |
| REQ-019 | Pesquisador realiza autocadastro via link exclusivo de convite gerado pelo Administrador | Funcional | Autenticação — fluxo de registro |
| REQ-021 | Líder realiza autocadastro via link exclusivo de convite gerado pelo Administrador | Funcional | Autenticação — fluxo de registro |
| REQ-094 | Projetos e seus participantes só podem ser consultados por quem tem acesso ao projeto (seus participantes — líder, pesquisador ou colaborador — e o Líder do grupo); os membros de um grupo só podem ser consultados pelo Líder do grupo | Não Funcional | Grupos de Pesquisa, Projetos — confidencialidade entre projetos |

---

## 4. Projetos

| Nº | Descrição | Classificação | Recursos Afetados / Impactos |
| :-- | :-------- | :------------ | :--------------------------- |
| REQ-022 | CRUD de projetos | Funcional | Atividades, Grupos de Pesquisa — projetos contêm atividades e pertencem a grupos |
| REQ-023 | Projetos são o conjunto de várias atividades | Não Funcional | Atividades — relação estrutural |
| REQ-024 | O sistema gera automaticamente um projeto inicial no momento da criação do grupo de pesquisa | Não Funcional | Grupos de Pesquisa — projeto vinculado ao grupo |
| REQ-025 | O primeiro projeto (referente ao grupo) só pode ser finalizado se não houver outros projetos abertos | Não Funcional | Atividades — dependência de estado dos projetos |
| REQ-026 | Fluxo de projetos: Planejadas > Em Andamento > Concluídas > Encerradas | Não Funcional | Atividades — transições de estado vinculadas |
| REQ-027 | Projetos podem ser criados pelo Líder do grupo, com título e assunto obrigatórios | Funcional | Grupos de Pesquisa — permissão de criação |
| REQ-028 | Projetos podem incluir pesquisadores e colaboradores; somente pessoas vinculadas ao projeto podem participar de atividades | Funcional | Atividades, Grupos de Pesquisa — restrição de vínculo |
| REQ-086 | Novo campo isFavorite (boolean) em projetos para identificar projetos em destaque | Não Funcional | Dashboard — projetos em destaque são exibidos como planetas no sistema solar |

---

## 5. Atividades

| Nº | Descrição | Classificação | Recursos Afetados / Impactos |
| :-- | :-------- | :------------ | :--------------------------- |
| REQ-029 | CRUD de atividades | Funcional | Projetos — atividades pertencem a projetos |
| REQ-030 | Atividades estão relacionadas a projetos | Não Funcional | Projetos — dependência estrutural |
| REQ-031 | Na criação deve-se associar a atividade a uma pessoa; só podem ser incluídas pessoas vinculadas ao projeto | Funcional | Projetos, Grupos de Pesquisa — restrição de vínculo |
| REQ-032 | Registrar três datas: data de criação, data de conclusão e data de entrega | Não Funcional | Calendário — datas alimentam o calendário |
| REQ-033 | Fluxo de atividades: Pendentes > Em Andamento > Concluídas > Encerradas, com etapa opcional de Aguardando confirmação; o pesquisador pode concluir a atividade sozinho e somente o Líder do grupo ou do projeto encerra | Não Funcional | Projetos, Dashboard — estados refletem no painel e no fechamento de projetos |
| REQ-034 | Destacar no sistema todas as atividades atrasadas | Não Funcional | Dashboard, Calendário — visibilidade de atrasos |
| REQ-035 | Impedimentos em atividades "Em Andamento" são registrados como atividade filha, que pode ser atribuída a qualquer pessoa do grupo; atividades criadas por pesquisadores aguardam a aprovação do líder do projeto | Funcional | Projetos, Grupos de Pesquisa — hierarquia de atividades |
| REQ-036 | Líder do grupo, ao encerrar atividade, dispara verificação se é a última do projeto; se sim, abre modal perguntando se deseja finalizar o projeto | Funcional | Projetos — integração direta com fechamento de projeto |

---

## 6. Solicitações

| Nº | Descrição | Classificação | Recursos Afetados / Impactos |
| :-- | :-------- | :------------ | :--------------------------- |
| REQ-037 | CRUD de solicitações | Funcional | Materiais, Projetos — solicitações podem envolver materiais e são vinculadas a projetos |
| REQ-038 | Tipos de solicitação: uso de material, financiamento, compra de material catalogado | Funcional | Materiais — tipo "uso de material" e "compra" interagem com o inventário |
| REQ-039 | Solicitações são criadas pelo líder do projeto ou pelo Líder do grupo e vinculadas obrigatoriamente a um projeto e ao solicitante | Funcional | Projetos, Grupos de Pesquisa — vínculo com projeto e responsável |
| REQ-040 | Solicitações devem conter título, descrição e justificativa | Não Funcional | Nenhum — definição de dados da solicitação |
| REQ-041 | Fluxo de solicitações: Aguardando líder do grupo > Aguardando administrador > Em andamento (uso de material) > Concluída / Encerrada; pode ser rejeitada em qualquer etapa de aprovação | Não Funcional | Materiais — status "uso de material" impacta disponibilidade |
| REQ-042 | Solicitações tipo "uso de material": selecionar material; se aprovada, permanece "Em Andamento" pelo tempo de uso e atribui o solicitante ao material; ao concluir, desatribui o usuário | Funcional | Materiais — impacto direto no status e na alocação de materiais |
| REQ-096 | Aprovação em duas etapas: o Líder do grupo aprova a solicitação e a encaminha ao administrador, que a aprova ou rejeita; solicitações criadas pelo próprio Líder do grupo seguem direto para o administrador | Funcional | Grupos de Pesquisa, Administração — aprovação pelo Líder do grupo e decisão final do administrador |

---

## 7. Materiais

| Nº | Descrição | Classificação | Recursos Afetados / Impactos |
| :-- | :-------- | :------------ | :--------------------------- |
| REQ-043 | CRUD de materiais | Funcional | Solicitações — materiais são referenciados em solicitações |
| REQ-044 | Campos: nome, categoria, quantidade, localização, data de aquisição | Não Funcional | Solicitações — dados necessários para solicitações de material |
| REQ-045 | Materiais podem ser inseridos pelo Líder do grupo | Funcional | Grupos de Pesquisa — permissão baseada em papel |
| REQ-046 | Status do material: Disponível, Em Uso, Manutenção | Não Funcional | Solicitações — status define disponibilidade para solicitação |
| REQ-047 | Quando em uso, mostrar o usuário que está utilizando o material; pode ou não ter data de devolução | Não Funcional | Solicitações — rastreabilidade de alocação |
| REQ-048 | Se a quantidade do material for maior que 1, o status só altera para "Em Uso" quando todos estiverem indisponíveis | Não Funcional | Solicitações — lógica de disponibilidade baseada em quantidade |

---

## 8. Publicações

| Nº | Descrição | Classificação | Recursos Afetados / Impactos |
| :-- | :-------- | :------------ | :--------------------------- |
| REQ-087 | CRUD de publicações | Funcional | Nenhum — novo domínio independente |
| REQ-088 | Publicações devem conter título, descrição, Tipo (VARCHAR(20)), Data da Publicação e Link | Não Funcional | Nenhum — definição de dados da publicação |
| REQ-089 | Publicações permitem anexar documento PDF, armazenado na tabela documentos (id, arquivo FILE, data_cadastro) | Funcional | Nenhum — funcionalidade de anexo |

---

## 9. Lembretes, Editais e Eventos

| Nº | Descrição | Classificação | Recursos Afetados / Impactos |
| :-- | :-------- | :------------ | :--------------------------- |
| REQ-049 | CRUD de lembretes | Funcional | Calendário — lembretes são exibidos no calendário |
| REQ-050 | Tipos de lembrete: Edital, Reunião, Apresentação, Workshops | Funcional | Calendário — tipo define ícone e comportamento |
| REQ-051 | Registrar data e horário do lembrete obrigatoriamente | Não Funcional | Calendário — alimentação do calendário |
| REQ-053 | Lembrete deve conter título, descrição, link, localização, participantes e organizador | Não Funcional | Calendário — dados exibidos |
| REQ-054 | Integrantes visualizam os lembretes do grupo a que têm acesso e podem confirmar presença | Não Funcional | Grupos de Pesquisa — participantes vinculados ao grupo |
| REQ-055 | Na criação: inserir participantes, título, descrição, data/horário e link de acesso para reuniões e editais | Funcional | Calendário — dados de criação |
| REQ-056 | Lembretes tipo "Reunião": permitir inserir arquivo de ata; somente o criador pode inserir a ata | Funcional | Nenhum — funcionalidade específica de reunião |
| REQ-057 | Somente o criador do lembrete pode realizar alterações | Não Funcional | Nenhum — regra de edição |
| REQ-058 | Front-end deve separar lembretes entre: Em Andamento, Próximos (3 dias para data) e Encerrados (passados) | Não Funcional | Nenhum — organização visual |
| REQ-059 | Todos os lembretes são vinculados a um grupo de pesquisa e, obrigatoriamente, a um projeto específico ou ao grupo inteiro; somente o Líder do grupo cria lembretes para o grupo inteiro e o Líder de projeto cria lembretes do seu projeto | Não Funcional | Grupos de Pesquisa — escopo do lembrete |
| REQ-060 | Cadastrar palavras-chave relevantes aos temas do grupo para filtragem de editais | Funcional | Nenhum — insumo para busca externa |
| REQ-063 | Filtrar editais por palavra-chave, organização e/ou data | Funcional | Nenhum — refinamento de busca |
| REQ-066 | Filtrar eventos por nome e/ou data | Funcional | Nenhum — refinamento de busca |
| REQ-090 | Lembretes podem ser configurados como eventos recorrentes, com suporte a recorrência diária, semanal, mensal e anual | Funcional | Calendário — repetição de eventos no calendário |
| REQ-093 | Editais, notícias e eventos podem ser compartilhados com projetos específicos do grupo, ficando visíveis apenas aos participantes desses projetos e ao Líder do grupo, ou ser públicos, visíveis a todo o grupo | Não Funcional | Editais, Notícias, Lembretes — visibilidade por projeto |
| REQ-095 | Editais são cadastrados, editados e removidos pelo Líder do grupo e possuem data de abertura e de encerramento | Funcional | Editais — permissão do Líder do grupo |

---

## 10. Calendário

| Nº | Descrição | Classificação | Recursos Afetados / Impactos |
| :-- | :-------- | :------------ | :--------------------------- |
| REQ-067 | Montar no front-end um calendário com lembretes e atividades; exibir o tipo (atividade ou lembrete — para lembretes mostrar o subtipo: "Reunião", "Edital", etc.) | Funcional | Atividades, Lembretes — calendário consolida ambos os recursos |
| REQ-068 | Mostrar lista com os próximos itens até 30 dias para frente | Funcional | Dashboard — visão de próximos eventos |
| REQ-069 | Integrante pode conectar o calendário do sistema com agenda externa (Outlook ou Google Calendar) | Funcional | Atividades, Lembretes — sincronização bidirecional |

---

## 11. Dashboard

| Nº | Descrição | Classificação | Recursos Afetados / Impactos |
| :-- | :-------- | :------------ | :--------------------------- |
| REQ-070 | Exibir dashboard no formato de sistema solar: o Sol representa o grupo de pesquisa e os planetas representam os projetos em destaque (isFavorite = true) | Funcional | Projetos, Grupos de Pesquisa — representação visual dos projetos em destaque |
| REQ-071 | O Líder do grupo vê todos os projetos em destaque; os demais integrantes veem somente os projetos em destaque dos quais participam | Não Funcional | Projetos, Autenticação — filtro baseado em papel |

---

## 12. Notícias

| Nº | Descrição | Classificação | Recursos Afetados / Impactos |
| :-- | :-------- | :------------ | :--------------------------- |
| REQ-074 | Cadastrar notícia com título, conteúdo, referência e data inicial e final de exibição | Funcional | Nenhum — recurso independente |
| REQ-077 | Filtrar notícias por assunto e/ou data | Funcional | Nenhum — refinamento de busca |
| REQ-078 | Ocultar automaticamente notícias expiradas após data final de exibição ou prazo padrão | Não Funcional | Nenhum — gestão de visibilidade |
| REQ-079 | Preencher automaticamente título e descrição de notícia inserida por link utilizando web scraping, permitindo edição pelo usuário | Funcional | Nenhum — automação de cadastro |
| REQ-080 | O Líder do grupo ou o líder do projeto rotulado valida e aprova a publicação definitiva de notícias cadastradas por pesquisadores | Funcional | Grupos de Pesquisa — hierarquia de aprovação |

---

## 13. Aspectos Técnicos (transversais) e Administração

| Nº | Descrição | Classificação | Recursos Afetados / Impactos |
| :-- | :-------- | :------------ | :--------------------------- |
| REQ-081 | Front-end desenvolvido em React | Não Funcional | Todos — tecnologia de interface |
| REQ-082 | Back-end desenvolvido com Java 25 e Spring Boot 4 | Não Funcional | Todos — tecnologia de servidor |
| REQ-083 | Protocolo de comunicação HTTP/REST no nível 2 de maturidade de Richardson | Não Funcional | Todos — padrão de API |
| REQ-084 | Banco de dados PostgreSQL 18, code first | Não Funcional | Todos — persistência e isolamento |
| REQ-091 | Administrador pode editar e remover pesquisadores e grupos de pesquisa | Funcional | Grupos de Pesquisa, Autenticação — permissão administrativa |
| REQ-092 | Campo função (VARCHAR(20)) para identificar a função específica do pesquisador no grupo | Não Funcional | Grupos de Pesquisa — dado complementar do perfil pesquisador |

---

## Requisitos removidos (revisão de 16/11/2026)

| Nº | Descrição original | Motivo |
| :-- | :-- | :-- |
| REQ-052 | Sistema envia notificações automáticas por e-mail aos participantes com 3 dias, 2 dias e no dia do vencimento | Não haverá notificações por e-mail |
| REQ-061 | Buscar editais em portais de fomento externos filtrados pelo tema do grupo de pesquisa | Editais serão cadastrados manualmente |
| REQ-062 | Listar editais encontrados pela busca no painel do grupo de pesquisa | Editais serão cadastrados manualmente |
| REQ-064 | Buscar eventos relacionados aos temas de interesse do grupo em portais conhecidos | Eventos serão cadastrados manualmente |
| REQ-065 | Listar eventos de interesse no painel do grupo de pesquisa | Eventos serão cadastrados manualmente |
| REQ-072 | A distância do planeta em relação ao Sol reflete o status do projeto (mais perto = concluído, mais longe = planejado) | Removido por decisão da equipe |
| REQ-073 | Administrador visualiza painel consolidado com informações de todos os grupos de pesquisa cadastrados | Removido por decisão da equipe |
| REQ-075 | Buscar notícias relevantes ao grupo de pesquisa em portais conhecidos | Notícias serão cadastradas manualmente |
| REQ-076 | Listar notícias encontradas na busca | Notícias serão cadastradas manualmente |
| REQ-085 | Projeto multi-módulo | Removido por decisão da equipe |
| REQ-005 | Coordenador pode cadastrar pesquisadores ou permitir autocadastro via convite | Perfil Coordenador removido |
| REQ-017 | Coordenador pode ser múltiplo por grupo e participar de vários grupos | Perfil Coordenador removido |
| REQ-020 | Coordenador realiza autocadastro via link exclusivo de convite gerado por Líder | Perfil Coordenador removido |
