package com.example.accesscontrol.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import com.example.accesscontrol.model.Stats;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Imports para Digest Auth com HttpClient4
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.util.Properties;

@Controller
public class RequestController {

    private final Stats stats;
    private final ConcurrentLinkedQueue<String> mensagens = new ConcurrentLinkedQueue<>();
    private static final int MAX_MENSAGENS = 1000;
    private volatile boolean disparoContinuo = false;
    private volatile int contadorDisparoContinuo = 0;

    @Value("${server.port:3000}")
    private String serverPort;

    public RequestController(Stats stats) {
        this.stats = stats;
    }

    @PostMapping(value = "/notification", consumes = "multipart/mixed")
    @ResponseBody
    public ResponseEntity<String> handleMultipartRequest(@RequestBody String rawBody) {
        processData(rawBody);
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("eventosRecebidos", stats.getEventosRecebidos());
        model.addAttribute("disparosRealizados", stats.getDisparosRealizados());
        model.addAttribute("mensagens", mensagens);
        model.addAttribute("disparoContinuo", disparoContinuo);
        model.addAttribute("contadorDisparoContinuo", contadorDisparoContinuo);
        model.addAttribute("serverPort", serverPort);
        return "index";
    }

    @PostMapping("/disparar")
    @ResponseBody
    public ResponseEntity<String> dispararComando(
            @RequestParam String ipDispositivo,
            @RequestParam String usuario,
            @RequestParam String senha,
            @RequestParam String comando,
            @RequestParam String metodo, // "GET" ou "POST"
            @RequestParam(required = false) String body, // Opcional para POST
            @RequestParam int intervalo) {

        try {
            if (intervalo > 0) {
                Thread.sleep(intervalo);
            }

            String resultado;
            if ("POST".equalsIgnoreCase(metodo)) {
                resultado = enviarComandoDigestPOST(ipDispositivo, usuario, senha, comando, body);
            } else {
                resultado = enviarComandoDigestGET(ipDispositivo, usuario, senha, comando);
            }

            stats.incrementarDisparos();
            return ResponseEntity.ok(resultado);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.badRequest().body("Comando interrompido: " + e.getMessage());
        } catch (Exception e) {
            String erro = "Erro: " + e.getMessage();
            adicionarMensagem(String.format("[%s] ✗ %s", java.time.LocalTime.now(), erro));
            return ResponseEntity.badRequest().body(erro);
        }
    }

    @PostMapping("/disparo-continuo/iniciar")
    @ResponseBody
    public ResponseEntity<String> iniciarDisparoContinuo(
            @RequestParam String ipDispositivo,
            @RequestParam String usuario,
            @RequestParam String senha,
            @RequestParam String comando,
            @RequestParam String metodo,
            @RequestParam(required = false) String body,
            @RequestParam int intervalo,
            @RequestParam int repeticoes) {

        if (disparoContinuo) {
            return ResponseEntity.badRequest().body("Disparo contínuo já está em andamento");
        }

        disparoContinuo = true;
        contadorDisparoContinuo = 0;

        // Executa em uma thread separada para não bloquear a interface
        new Thread(() -> {
            try {
                for (int i = 0; i < repeticoes && disparoContinuo; i++) {
                    if ("POST".equalsIgnoreCase(metodo)) {
                        enviarComandoDigestPOST(ipDispositivo, usuario, senha, comando, body);
                    } else {
                        enviarComandoDigestGET(ipDispositivo, usuario, senha, comando);
                    }
                    stats.incrementarDisparos();
                    contadorDisparoContinuo++;

                    adicionarMensagem(String.format("[%s] 🔄 Disparo contínuo %d/%d",
                            java.time.LocalTime.now(), contadorDisparoContinuo, repeticoes));

                    Thread.sleep(intervalo);
                }

                if (disparoContinuo) {
                    adicionarMensagem(String.format("[%s] ✅ Disparo contínuo concluído: %d execuções",
                            java.time.LocalTime.now(), contadorDisparoContinuo));
                    disparoContinuo = false;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                adicionarMensagem(String.format("[%s] ❌ Erro no disparo contínuo: %s",
                        java.time.LocalTime.now(), e.getMessage()));
                disparoContinuo = false;
            }
        }).start();

        return ResponseEntity.ok("Disparo contínuo iniciado: " + repeticoes + " repetições a cada " + intervalo + "ms");
    }

    @PostMapping("/disparo-continuo/parar")
    @ResponseBody
    public ResponseEntity<String> pararDisparoContinuo() {
        if (disparoContinuo) {
            disparoContinuo = false;
            adicionarMensagem(String.format("[%s] ⏹️ Disparo contínuo interrompido. Total: %d execuções",
                    java.time.LocalTime.now(), contadorDisparoContinuo));
            return ResponseEntity.ok("Disparo contínuo interrompido");
        } else {
            return ResponseEntity.badRequest().body("Nenhum disparo contínuo em andamento");
        }
    }

    @PostMapping("/salvar-log")
    @ResponseBody
    public ResponseEntity<String> salvarLog() {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String filename = "log_acesso_" + timestamp + ".txt";

            StringBuilder logContent = new StringBuilder();
            logContent.append("=== LOG DE CONTROLE DE ACESSO ===\n");
            logContent.append("Data: ")
                    .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                    .append("\n");
            logContent.append("Eventos Recebidos: ").append(stats.getEventosRecebidos()).append("\n");
            logContent.append("Comandos Enviados: ").append(stats.getDisparosRealizados()).append("\n");
            logContent.append("Disparos Contínuos: ").append(contadorDisparoContinuo).append("\n");
            logContent.append("==================================\n\n");

            for (String mensagem : mensagens) {
                logContent.append(mensagem).append("\n");
            }

            Files.write(Paths.get(filename), logContent.toString().getBytes());

            adicionarMensagem(String.format("[%s] 💾 Log salvo como: %s", java.time.LocalTime.now(), filename));
            return ResponseEntity.ok("Log salvo como: " + filename);

        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Erro ao salvar log: " + e.getMessage());
        }
    }

    @PostMapping("/limpar-tudo")
    @ResponseBody
    public ResponseEntity<String> limparTudo() {
        mensagens.clear();
        stats.reset();
        contadorDisparoContinuo = 0;
        disparoContinuo = false;

        adicionarMensagem(String.format("[%s] 🗑️ Todos os dados foram limpos", java.time.LocalTime.now()));
        return ResponseEntity.ok("Todos os dados limpos");
    }

    @PostMapping("/limpar-logs")
    @ResponseBody
    public ResponseEntity<String> limparLogs() {
        mensagens.clear();
        adicionarMensagem(String.format("[%s] 📋 Logs limpos", java.time.LocalTime.now()));
        return ResponseEntity.ok("Logs limpos");
    }

    @PostMapping("/config/port")
    @ResponseBody
    public ResponseEntity<String> updatePort(@RequestParam String newPort) {
        try {
            int port = Integer.parseInt(newPort);
            if (port < 1024 || port > 65535) {
                return ResponseEntity.badRequest().body("Porta inválida. Use entre 1024 e 65535.");
            }

            // Caminho para o application.properties
            // Assumindo estrutura padrão do projeto local
            String propertyFilePath = "src/main/resources/application.properties";

            Properties props = new Properties();
            try (FileInputStream in = new FileInputStream(propertyFilePath)) {
                props.load(in);
            }

            props.setProperty("server.port", String.valueOf(port));

            try (FileOutputStream out = new FileOutputStream(propertyFilePath)) {
                props.store(out, null);
            }

            adicionarMensagem(String.format("[%s] ⚙️ Porta alterada para %d. Reinicie o servidor.",
                    java.time.LocalTime.now(), port));

            return ResponseEntity.ok("Porta salva: " + port + ". Reinicie o servidor para aplicar.");

        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Porta deve ser um número.");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Erro ao salvar configuração: " + e.getMessage());
        }
    }

    private String enviarComandoDigestGET(String ipDispositivo, String usuario, String senha, String comando) {
        CloseableHttpClient httpClient = null;
        try {
            // Configura provedor de credenciais para Digest Auth
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    new AuthScope(ipDispositivo, 80),
                    new UsernamePasswordCredentials(usuario, senha));

            // Cria HttpClient com suporte a Digest Auth
            httpClient = HttpClients.custom()
                    .setDefaultCredentialsProvider(credentialsProvider)
                    .build();

            String url = String.format("http://%s/cgi-bin/accessControl.cgi?%s", ipDispositivo, comando);

            adicionarMensagem(String.format("[%s] 🔷 GET: %s", java.time.LocalTime.now(), url));

            HttpGet httpGet = new HttpGet(url);

            // Executa a requisição
            HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());

            if (statusCode == 200) {
                String sucesso = "✅ GET executado com sucesso! Resposta: " + responseBody;
                adicionarMensagem(String.format("[%s] %s", java.time.LocalTime.now(), sucesso));
                return sucesso;
            } else {
                String erro = "❌ Erro " + statusCode + " no GET: " + responseBody;
                adicionarMensagem(String.format("[%s] %s", java.time.LocalTime.now(), erro));
                return erro;
            }

        } catch (Exception e) {
            String erro = "❌ Falha no GET: " + e.getMessage();
            adicionarMensagem(String.format("[%s] %s", java.time.LocalTime.now(), erro));
            return erro;
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    private String enviarComandoDigestPOST(String ipDispositivo, String usuario, String senha, String comando,
            String body) {
        CloseableHttpClient httpClient = null;
        try {
            // Configura provedor de credenciais para Digest Auth
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    new AuthScope(ipDispositivo, 80),
                    new UsernamePasswordCredentials(usuario, senha));

            // Cria HttpClient com suporte a Digest Auth
            httpClient = HttpClients.custom()
                    .setDefaultCredentialsProvider(credentialsProvider)
                    .build();

            String url = String.format("http://%s/cgi-bin/accessControl.cgi?%s", ipDispositivo, comando);

            adicionarMensagem(String.format("[%s] 🔶 POST: %s", java.time.LocalTime.now(), url));
            if (body != null && !body.trim().isEmpty()) {
                adicionarMensagem(String.format("[%s] 📦 Body: %s", java.time.LocalTime.now(), body));
            }

            HttpPost httpPost = new HttpPost(url);

            // Adiciona body se fornecido
            if (body != null && !body.trim().isEmpty()) {
                httpPost.setEntity(new StringEntity(body));
                httpPost.setHeader("Content-Type", "application/json");
            }

            // Executa a requisição
            HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());

            if (statusCode == 200) {
                String sucesso = "✅ POST executado com sucesso! Resposta: " + responseBody;
                adicionarMensagem(String.format("[%s] %s", java.time.LocalTime.now(), sucesso));
                return sucesso;
            } else {
                String erro = "❌ Erro " + statusCode + " no POST: " + responseBody;
                adicionarMensagem(String.format("[%s] %s", java.time.LocalTime.now(), erro));
                return erro;
            }

        } catch (Exception e) {
            String erro = "❌ Falha no POST: " + e.getMessage();
            adicionarMensagem(String.format("[%s] %s", java.time.LocalTime.now(), erro));
            return erro;
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    private void processData(String rawData) {
        String[] parts = rawData.split("--myboundary");

        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty())
                continue;

            if (part.contains("Content-Type: text/plain")) {
                String[] lines = part.split("\r\n");

                boolean jsonStarted = false;
                StringBuilder jsonContent = new StringBuilder();

                for (String line : lines) {
                    if (jsonStarted) {
                        jsonContent.append(line).append("\r\n");
                    }
                    if (line.trim().isEmpty()) {
                        jsonStarted = true;
                    }
                }

                String jsonData = jsonContent.toString().trim();
                if (!jsonData.isEmpty()) {
                    // Filtra eventos 401 (não autorizados)
                    if (!jsonData.contains("\"Status\": 401") && !jsonData.contains("401 Unauthorized")) {
                        String mensagem = String.format("[%s] 📩 EVENTO: %s",
                                java.time.LocalTime.now(), jsonData);
                        adicionarMensagem(mensagem);
                        stats.incrementarEventos();
                    } else {
                        // Log de tentativa não autorizada (sem incrementar contador)
                        adicionarMensagem(String.format("[%s] ⚠️ Tentativa não autorizada filtrada",
                                java.time.LocalTime.now()));
                    }
                }
            }
        }
    }

    private void adicionarMensagem(String mensagem) {
        mensagens.add(mensagem);
        while (mensagens.size() > MAX_MENSAGENS) {
            mensagens.poll();
        }
    }
}