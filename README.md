# API de Conversão de Moedas

API REST para conversão de moedas com integração a serviços externos de cotação em tempo real.

## Tecnologias Utilizadas

- Java 17
- Spring Boot 3.x
- Spring Data JPA
- Spring Cache
- H2 Database
- Swagger/OpenAPI 3.0
- Maven

## Requisitos

- Java 17 ou superior
- Maven 3.6 ou superior

## Configuração

1. Clone o repositório
2. Configure as variáveis de ambiente no arquivo `application.yml`
3. Execute o projeto com Maven:

```bash
mvn spring-boot:run
```

## Endpoints da API

### Listar Moedas Suportadas
```
GET /api/currencies
```

### Converter Moeda
```
GET /api/currencies/convert?from={moeda}&to={moeda}&amount={valor}
```

### Obter Taxas de Câmbio
```
GET /api/currencies/rates/{moeda}
```

### Histórico de Conversões
```
GET /api/currencies/history/{moeda1}/{moeda2}?startDate={data_inicio}&endDate={data_fim}&page={pagina}&size={tamanho}
```

## Documentação Swagger

A documentação completa da API está disponível em:
```
http://localhost:8080/api/swagger-ui.html
```

## Cache

A API utiliza cache para armazenar as taxas de câmbio por 1 hora (configurável no `application.yml`).

## Banco de Dados

O projeto utiliza H2 como banco de dados em memória. O console H2 está disponível em:
```
http://localhost:8080/api/h2-console
```

## Monitoramento e Métricas

A API expõe métricas através do Spring Boot Actuator.

- **Endpoint de Métricas:** `http://localhost:8080/api/actuator/metrics` lista os nomes das métricas disponíveis.
- **Endpoint Prometheus:** `http://localhost:8080/api/actuator/prometheus` expõe as métricas em um formato compatível com o Prometheus para coleta.

Para uma visualização gráfica e dashboards, recomenda-se integrar com ferramentas como Prometheus (para coleta) e Grafana (para visualização).

## Testes

Para executar os testes:
```bash
mvn test
```

## Contribuição

1. Faça o fork do projeto
2. Crie uma branch para sua feature (`git checkout -b feature/nova-feature`)
3. Commit suas mudanças (`git commit -m 'Adiciona nova feature'`)
4. Push para a branch (`git push origin feature/nova-feature`)
5. Abra um Pull Request

## Licença

Este projeto está sob a licença MIT. 