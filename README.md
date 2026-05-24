# SafePix Async

API Java com Spring Boot e RabbitMQ que simula o recebimento de uma solicitacao Pix e processa a mensagem de forma assincrona.

## Tecnologias

- Java 21
- Spring Boot 4
- Spring AMQP
- RabbitMQ
- Docker Compose
- SpringDoc OpenAPI
- Spring Actuator
- JUnit 5 e Testcontainers

## Como rodar tudo com Docker Compose

```powershell
docker compose up --build
```

A aplicacao ficara disponivel em:

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health
- RabbitMQ Management: http://localhost:15672

Credenciais do RabbitMQ:

- Usuario: `safepix`
- Senha: `safepix`

Para parar:

```powershell
docker compose down
```

## Como rodar localmente

Suba apenas o RabbitMQ:

```powershell
docker compose up -d rabbitmq
```

Depois rode a aplicacao:

```powershell
.\mvnw.cmd spring-boot:run
```

## Testando o fluxo de sucesso

Envie um Pix valido:

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/pix `
  -ContentType "application/json" `
  -Body '{
    "id": "11111111-1111-1111-1111-111111111111",
    "chavePix": "cliente@email.com",
    "valor": 150.75,
    "timestamp": "2026-05-24T21:00:00Z"
  }'
```

A API responde `202 Accepted`, publica a mensagem na fila `pix.recebido.v1` e o consumer processa em segundo plano.

## Testando validacao da API

Envie um valor invalido:

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/pix `
  -ContentType "application/json" `
  -Body '{
    "id": "22222222-2222-2222-2222-222222222222",
    "chavePix": "cliente@email.com",
    "valor": 0,
    "timestamp": "2026-05-24T21:00:00Z"
  }'
```

A API deve retornar `400 Bad Request` por causa do Bean Validation.

## Testando DLQ

Mensagens invalidas que chegam diretamente ao RabbitMQ e falham no consumer sao rejeitadas sem requeue e enviadas para a fila:

```text
pix.recebido.v1.dlq
```

Esse comportamento tambem e validado pelos testes de integracao com Testcontainers.

## Rodando testes

```powershell
.\mvnw.cmd test
```

Os testes de integracao sobem um RabbitMQ real via Testcontainers, entao o Docker Desktop precisa estar rodando.
