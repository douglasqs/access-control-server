# Controlador de Porta de Acesso (Access Control Server)

Este projeto é um servidor de backend desenvolvido em **Java (Spring Boot)** com interface Web, projetado para interagir com controladores de acesso e LPR (License Plate Recognition) da **Dahua**.

O sistema atua como um intermediário que recebe notificações de eventos dos controladores e permite o envio de comandos remotos (como abrir portas/cancelas) utilizando autenticação Digest.

## 🚀 Funcionalidades

-   **Monitoramento de Eventos**: Recebe e processa notificações de eventos dos controladores via HTTP multipart.
-   **Comando Remoto**: Envia comandos CGI para as controladoras (ex: abrir porta) suportando métodos GET e POST com autenticação Digest.
-   **Interface Web Amigável**:
    -   Dashboard em tempo real com contadores de eventos e comandos.
    -   Log de atividades visualizável na tela.
    -   Modo **Dark Theme** (Tema Escuro).
-   **Teste de Stress (Disparo Contínuo)**: Funcionalidade para enviar múltiplos comandos em sequência para testar a estabilidade e resposta dos dispositivos.
-   **Configuração Dinâmica**: Alteração da porta do servidor via interface gráfica.
-   **Logs**: Exportação de logs de atividades para arquivos de texto.

## 🛠️ Tecnologias Utilizadas

-   **Java 17**
-   **Spring Boot**: Framework principal (Web, Thymeleaf).
-   **Apache HttpClient**: Para comunicação HTTP robusta com suporte a Digest Auth.
-   **Thymeleaf**: Engine de template para o frontend.
-   **HTML5 / CSS3 / JavaScript**: Interface do usuário (sem frameworks pesados).

## 📋 Pré-requisitos

-   JDK 17 instalado.
-   Maven instalado (ou usar o wrapper `mvnw` incluso).

## ⚙️ Configuração

As configurações principais ficam no arquivo `src/main/resources/application.properties`.

```properties
server.port=3000
# Configuração de Logs
logging.level.root=INFO
```

Você também pode alterar a porta do servidor diretamente pela interface web clicando no botão **⚙️ Config**.

## 🚀 Como Executar

### Via Maven

```bash
mvn spring-boot:run
```

### Via Jar (Produção)

1.  Compile o projeto:
    ```bash
    mvn clean package
    ```
2.  Execute o arquivo `.jar` gerado na pasta `target`:
    ```bash
    java -jar target/access-control-server-0.0.1-SNAPSHOT.jar
    ```

## 🖥️ Uso da Interface

Acesse `http://localhost:3000` (ou a porta configurada) no seu navegador.

### Painel de Controle
-   **Comandos Rápidos**: Botões pré-configurados para ações comuns (ex: Abrir Porta 1).
-   **Comando Personalizado**: Formulário para construir requisições CGI específicas para a câmera.
-   **Disparo Contínuo**: Configure número de repetições e intervalo para testes de carga.
-   **Logs**: A área preta à direita mostra os logs em tempo real. Use os botões abaixo para salvar ou limpar.

## 🔒 Autenticação com Controladores

O servidor implementa **Digest Authentication** automaticamente ao se comunicar com os dispositivos Dahua, garantindo que os comandos sejam aceitos pelos dispositivos protegidos por senha.

## 🤝 Contribuição

Sinta-se à vontade para abrir issues ou enviar pull requests para melhorias.

---
Desenvolvido para integração com dispositivos de segurança eletrônica.
