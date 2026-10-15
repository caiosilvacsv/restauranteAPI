# 🍕 Restaurante & Pizzaria API

Backend robusto, escalável e seguro para operação de restaurantes e pizzarias, projetado para funcionamento híbrido com integração em tempo real entre **Cloud Server**, **Hub Local**, **KDS (Kitchen Display System)**, **Comanda Garçom** e **Cardápio Online / Delivery**.

Desenvolvido em **Java 21**, **Spring Boot**, **PostgreSQL** e **Docker**.

---

## 🚀 Principais Módulos e Regras de Negócio

### 1. Cardápio & Pizzaria Especializada
* **Pizzas Meio a Meio (Regra do Teto / `CEILING`)**:
  * Ao compor uma pizza de dois sabores, o preço unitário é calculado pela média aritmética e arredondado para o **próximo inteiro superior** (ex: R\$ 44,00 + R\$ 45,00 = R\$ 44,50 $\rightarrow$ **R\$ 45,00**). Nunca gera centavos fracionados para o cliente.
* **Tamanhos e Sabores (`PizzaSize`)**:
  * Configuração flexível de tamanhos (`BROTO`, `MEDIA`, `GRANDE`, `GIGANTE`), controlando quantidade de fatias e limite de sabores por pizza.
* **Modificações e Ingredientes**:
  * Personalização de itens por adição (`ADD`) ou remoção (`REMOVE`) de ingredientes, com precificação de adicionais.

### 2. Gestão de Mesas em Tempo Real (`/tables`)
* **Status Dinâmico**: Acompanhamento de mesas (`AVAILABLE`, `OCCUPIED`, `RESERVED`, `INACTIVE`).
* **Vinculação Automática**: Ao abrir um pedido de salão (`DINE_IN`) com o ID da mesa, ela transita automaticamente para `OCCUPIED`.
* **Identificação Imediata**: A listagem de mesas retorna o ID do pedido ativo (`currentOrderId`), código amigável (`currentOrderCode`) e hora de abertura.
* **Liberação Automática**: Quando o saldo do pedido é quitado ou o pedido é cancelado, a mesa é liberada (`AVAILABLE`) sem intervenção manual.

### 3. KDS / Cozinha Idempotente (`/kitchen`)
* **Pipeline de Produção**: `PENDING` $\rightarrow$ `QUEUED` $\rightarrow$ `PREPARING` $\rightarrow$ `READY` $\rightarrow$ `DESPATCHED` $\rightarrow$ `DELIVERED`.
* **Idempotência**:
  * Proteção contra duplo clique ou reenvios de pacotes de rede (retries). Se um item já estiver no estágio requisitado ou à frente, a API responde `HTTP 200` com o estado atualizado em vez de rejeitar com erro 400.
* **Propagação em Cascata**:
  * Ao alterar o status geral do pedido (ex: `DESPATCHED` ou `CANCELLED`), os itens pendentes na cozinha são atualizados em cascata, evitando que itens finalizados fiquem presos nas telas de produção.

### 4. Pagamentos Múltiplos e Divisão de Contas (`/orders/{id}/payments`)
* **Divisão de Pagamento**: Suporte a múltiplos pagamentos fracionados para o mesmo pedido, aceitando diferentes pagadores e formas (`PIX`, `CASH`, `CREDIT_CARD`, `DEBIT_CARD`).
* **Resumo Financeiro no Pedido**: Todo `OrderResponse` expõe em tempo real:
  * `totalPaid`: valor já pago e aprovado.
  * `remainingAmount`: saldo restante a quitar.
  * `isFullyPaid`: flag indicando se a conta está 100% quitada.
* **Fechamento Automático**: Atingindo o valor total, o pedido é marcado como `CLOSED` e a mesa é desocupada.

### 5. Delta Polling & Sincronização Leve (`GET /orders`)
* Parâmetro `?since=ISO_TIMESTAMP`: O Hub local ou PDV baixa apenas pedidos novos ou atualizados a partir do último ciclo de sincronização, economizando tráfego de rede.
* Parâmetro `?date=YYYY-MM-DD`: Filtro direto por data de operação no fuso local (`America/Sao_Paulo`).

### 6. Configurações da Loja (`/config`)
* Controle de horário de abertura e fechamento (com suporte a turnos que cruzam meia-noite, ex: 18:00 às 02:00).
* Botão de fechamento manual emergencial com mensagem personalizada para os clientes.
* Gestão de chave Pix da loja e taxa de entrega base.

---

## 🛡️ Segurança & Perfis de Acesso (RBAC)

Autenticação stateless baseada em **JWT (JSON Web Token)** com controle de permissões por roles:

| Role | Finalidade | Principais Permissões |
|---|---|---|
| `ROLE_ADMIN` | Gerência e Proprietário | Acesso total a configurações, faturamento, catálogo e usuários. |
| `ROLE_WAITER` | Garçons e Operação Salão | Criação e acompanhamento de pedidos de mesa, fechamento e recebimento. |
| `ROLE_CUSTOMER` | Clientes do Cardápio Web | Criação de pedidos delivery, consulta do status dos seus próprios pedidos. |
| `ROLE_USER` | Usuário comum autenticado | Base para consultas gerais permitidas. |

---

## ⚡ Performance & Arquitetura

* **Virtual Threads (Project Loom)**: Ativado com `spring.threads.virtual.enabled=true`, permitindo alta concorrência em I/O sem sobrecarga de threads do sistema operacional.
* **Pool HikariCP Otimizado**: Limite máximo de conexões (`DB_MAX_POOL_SIZE=20`), conexões ociosas mínimas (`DB_MIN_IDLE=5`), timeout de aquisição e reciclagem periódica de conexões para prevenir memory/connection leaks.
* **Cache em Memória (Caffeine)**: Cache local de alta performance com TTL configurável para catálogo de produtos e configurações frequentes.
* **Banco de Dados Seguro**:
  * Flyway com migração inicial consolidada única (`V1__initial_schema.sql`).
  * **Zero seeds fixos em código**: nenhuma senha padrão, credencial ou dado confidencial exposto no versionamento.
  * Validação estrita do Hibernate com `spring.jpa.hibernate.ddl-auto=validate`.

---

## 📋 Principais Endpoints da API

A documentação interativa completa do Swagger / OpenAPI fica disponível em:
👉 **`http://localhost:8080/swagger-ui/index.html`**

### Autenticação & Usuários
* `POST /auth/login`: Autentica usuário e retorna JWT com as roles.
* `POST /auth/register`: Cadastro de usuários administrativos e garçons.
* `POST /auth/guest`: Cadastro simplificado para clientes do cardápio online.

### Mesas
* `GET /tables`: Lista todas as mesas, status e pedido associado no momento.
* `GET /tables/available`: Lista apenas as mesas livres para novos clientes.
* `POST /tables`: Cadastro de nova mesa (`number`, `capacity`, `description`).

### Pedidos & Pagamentos
* `GET /orders`: Lista pedidos com suporte a filtros (`?since=...`, `?date=...`, `?status=...`, `?tableId=...`).
* `GET /orders/{id}`: Detalhes do pedido, itens, endereço e saldo financeiro.
* `POST /orders`: Criação de novo pedido (`DINE_IN` ou `DELIVERY`).
* `PATCH /orders/{id}/status`: Atualização do status com cascata nos itens.
* `POST /orders/{id}/payments`: Registra pagamento (parcial ou integral).
* `GET /orders/{id}/payments`: Extrato de todos os pagamentos realizados no pedido.

### Cozinha (KDS)
* `GET /kitchen/item/pending`: Fila de itens pendentes para início do preparo.
* `GET /kitchen/item/queued`: Fila de itens despachados para a cozinha.
* `PATCH /kitchen/item/{id}/prepared`: Avança item para `PREPARING` (idempotente).
* `PATCH /kitchen/item/{id}/ready`: Marca item como `READY` (idempotente).
* `PATCH /kitchen/item/{id}/despatched`: Avança item para expedição/garçom.

### Configurações da Loja
* `GET /config`: Consulta horários, taxa de entrega, chave Pix e status de atendimento.
* `PUT /config`: Atualização das configurações operacionais.
* `PATCH /config/manual-status`: Abre ou fecha a loja manualmente.

---

## 🛠️ Como Executar o Projeto

### Pré-requisitos
* **Java 21** ou superior
* **Maven 3.9+** (ou utilize o `mvn` instalado em seu ambiente)
* **PostgreSQL 15+** ou **Docker**

---

### Opção 1: Executando via Docker Compose (Recomendado)

1. Clone o repositório:
   ```bash
   git clone https://github.com/caiosilvacsv/restauranteAPI.git
   cd restauranteAPI
   ```

2. Crie o arquivo `.env` a partir do modelo de exemplo:
   ```bash
   cp .env.exemple .env
   ```

3. Suba o banco de dados e a aplicação:
   ```bash
   docker compose up --build -d
   ```

A API estará pronta em `http://localhost:8080`.

---

### Opção 2: Executando Localmente com Maven

1. Configure o banco PostgreSQL local (porta 5432) e crie a base de dados:
   ```sql
   CREATE DATABASE restaurant;
   ```

2. Ajuste as credenciais no arquivo `.env` ou configure as variáveis de ambiente:
   ```properties
   DATASOURCE_URL=jdbc:postgresql://localhost:5432/restaurant
   DATASOURCE_USERNAME=postgres
   DATASOURCE_PASSWORD=sua_senha
   JWT_SECRET=sua_chave_secreta_jwt
   ```

3. Execute a suíte de testes automatizados para validar o schema:
   ```bash
   mvn clean test
   ```

4. Inicie o servidor:
   ```bash
   mvn spring-boot:run
   ```

---

## 🧪 Testes Automatizados

O projeto conta com suíte de testes de integração e regras de negócio:
```bash
mvn test
```
* **Cálculo de Preço Meio a Meio**: Validação de arredondamento Teto/Ceiling sem centavos fracionados.
* **Ciclo de Mesas**: Ocupação e liberação automática via quitação de pagamentos.
* **Idempotência do KDS**: Validação contra retries e transições repetidas.
* **Cascata de Status**: Garantia de propagação para itens de pedidos cancelados ou despachados.
* **Validação de Schema DDL**: Compatibilidade integral entre entidades JPA e banco PostgreSQL.

---

## 📄 Licença

Distribuído sob licença proprietária para uso no ecossistema e-Comida / Restaurante API.
