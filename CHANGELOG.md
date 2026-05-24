# Changelog

Todas as mudancas relevantes deste projeto serao documentadas neste arquivo.

Formato baseado em [Keep a Changelog](https://keepachangelog.com/) e versionamento semantico.

## [Unreleased]

### Added

- Documentacao profissional com arquitetura, execucao, endpoints e exemplos cURL.
- Instrucoes para Docker Compose, Swagger UI, Actuator e RabbitMQ Management.
- Guia de contribuicao com fluxo de branch, commits, testes e padrao de PR.

## [0.1.0] - 2026-05-24

### Added

- API `POST /pix` para receber solicitacoes Pix.
- Publicacao assincrona em RabbitMQ pela fila `pix.recebido.v1`.
- Consumer RabbitMQ para processar mensagens Pix.
- DLQ `pix.recebido.v1.dlq` para mensagens rejeitadas.
- Validacao com Bean Validation.
- SpringDoc OpenAPI e Swagger UI.
- Spring Actuator com health/info.
- Testes unitarios com JUnit 5 e Mockito.
- Testes de integracao com Testcontainers e RabbitMQ.
- Dockerfile multi-stage e Docker Compose com RabbitMQ Management.

