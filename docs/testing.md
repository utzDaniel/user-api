# Testes

## Estratégia

- **Unitários (obrigatório)** — camada de Service com JUnit 5 + Mockito
- **Integração (opcional/futuro)** — Controller + API

## Escopo Obrigatório

Todo Service DEVE ter testes cobrindo:
- Lógica de negócio e regras de validação
- Publicação de evento em operações de escrita
- Cenários de erro (exceções esperadas)

## Estrutura

```
src/test/java/br/com/user/modules/<module>/service/<Resource>ServiceTest.java
```

## Nomenclatura

Padrão: `<metodo>Should<comportamentoEsperado>`

Exemplos: `createShouldSaveUser`, `createShouldPublishEvent`, `getByIdShouldThrowWhenNotFound`

## Regras

- Mockar todas as dependências (Repository, Mapper, EventPublisher)
- Sem acesso a banco de dados real
- Cada teste valida um único comportamento
- Cada teste é independente (sem estado compartilhado)
- Verificar publicação de evento em **todo** teste de operação de escrita

## Antipadrões

- Testar múltiplos comportamentos em um único teste
- Usar banco de dados real
- Não verificar publicação de evento
- Ignorar cenários de erro
- Testes sem asserções
