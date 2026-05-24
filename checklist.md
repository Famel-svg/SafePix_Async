# Checklist de Projeto Sistema de Mensageria com Java, Spring Boot e RabbitMQ

Este projeto simula o fluxo de processamento de um PIX, onde a API recebe o pedido e o processa de forma assíncrona para garantir alta disponibilidade.

---

## 🚀 Tecnologias Utilizadas
 Java 17 ou 21 (Uso de Records e melhor performance).
 Spring Boot 3.x (Framework base).
 Spring AMQP (Abstração para RabbitMQ).
 RabbitMQ (Message Broker).
 Docker & Docker Compose (Containerização da infra e app).
 Jackson (Serialização JSON).
 JUnit 5 & Testcontainers (Testes de integração com RabbitMQ real).
 SpringDoc OpenAPI (Swagger) (Documentação da API).

---

## 🎯 Finalidade do Projeto
Demonstrar o desacoplamento de serviços. Em vez da API travar esperando o processamento de um banco ou antifraude, ela posta uma mensagem e responde ao usuário imediatamente, enquanto um trabalhador (consumer) processa a tarefa em segundo plano.

---

## 📋 Passo a Passo de Construção

### Passo 1 Infraestrutura e Setup Inicial
- [x] Criar o `docker-compose.yml` Configurar o serviço do RabbitMQ com a imagem `3-management` (inclui o painel web).
- [x] Configurar `pom.xml` Adicionar as dependências `spring-boot-starter-amqp`, `spring-boot-starter-web`, `spring-boot-starter-validation` e `lombok`.
- [x] application.yml Configurar host, porta, usuário e senha do RabbitMQ.

### Passo 2 Modelo de Dados (Domínio)
- [x] Criar PixDTO (Java Record) Definir os campos (id, chavePix, valor, timestamp). Usar Records para imutabilidade.
- [x] Configurar o MessageConverter Criar um Bean do `Jackson2JsonMessageConverter` para que o Spring envie objetos como JSON em vez de serialização Java nativa (essencial para interoperabilidade).

### Passo 3 Configuração da Arquitetura de Mensagens
- [x] Declarar a Fila Principal Criar o Bean da fila `pix.recebido.v1`.
- [x] Declarar a DLQ (Dead Letter Queue) Criar a fila `pix.recebido.v1.dlq` (para onde vão as mensagens que falharem).
- [X] Configurar a Dead Letter Strategy Vincular a fila principal à DLQ através das propriedades de argumentos da fila (`x-dead-letter-exchange`).

### Passo 4 O Produtor (ProducerAPI)
- [x] Criar o PixController Endpoint POST para receber o Pix.
- [x] Criar o PixService Lógica para enviar a mensagem usando `RabbitTemplate`.
- [x] Validação Garantir que o valor do Pix seja positivo e a chave não seja nula (Bean Validation).

### Passo 5 O Consumidor (ConsumerWorker)
- [x] Criar o PixConsumer Classe anotada com `@Component`.
- [x] Implementar o `@RabbitListener` Método que escuta a fila principal.
- [x] Simular Lógica de Negócio Logar o recebimento e simular um tempo de processamento (`Thread.sleep`).
- [x] Simular Falha Adicionar uma lógica que lança uma `AmqpRejectAndDontRequeueException` se o valor for inválido, para testar o envio para a DLQ.

### Passo 6 Testes Automatizados (O diferencial)
- [x] Testes Unitários Testar as validações do DTO e lógica do Service com Mockito.
- [x] Testes de Integração com Testcontainers 
    - Configurar um container do RabbitMQ subindo apenas para o teste.
    - Validar se, ao enviar um POST na API, a mensagem realmente chega ao listener.
- [x] Teste de DLQ Forçar um erro no consumidor e validar se a mensagem caiu na fila de erro.

### Passo 7 Observabilidade e Documentação
- [x] Swagger UI Configurar o SpringDoc para testar o POST pela interface visual.
- [x] Spring Actuator Adicionar `health` para monitorar se a conexão com o RabbitMQ está de pé.
- [x] Logs Implementar logs claros usando SLF4J (Logback) em cada etapa do fluxo.

### Passo 8 Deploy e Finalização
- [ ] Dockerfile Criar uma imagem multi-stage para otimizar o tamanho do jar final.
- [ ] Docker Compose Final Adicionar a aplicação Java ao `docker-compose.yml` para subir o sistema completo (App + RabbitMQ) com um único comando.
- [ ] README.md Escrever como rodar o projeto, como acessar o painel do RabbitMQ e como testar o fluxo de sucesso e erro.

---
## 💡 Dicas para o Nível Intermediário
 Idempotência Pense O que acontece se a mesma mensagem for processada duas vezes. Tente adicionar um controle de ID no banco de dados.
 Retry Policy Configure no `application.yml` o número de tentativas (ex 3 vezes) antes de mandar para a DLQ.
