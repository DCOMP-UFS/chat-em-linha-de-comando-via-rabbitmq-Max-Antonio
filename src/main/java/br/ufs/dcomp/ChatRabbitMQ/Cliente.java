package br.ufs.dcomp.ChatRabbitMQ;
import com.rabbitmq.client.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.*;
import java.util.*;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.ByteString;
import java.nio.file.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

public class Cliente {
    
    private Connection connection;
    private Channel channel;
    private Channel channelFile;
    private String username;
    private String receptorAtual; //usuario que o usuario atual está mandando mensagem
    private String grupoAtual;
    private String QUEUE_NAME; //fila de mensagens usuário
    private String FILE_QUEUE_NAME; //fila de arquivos do usuário
    private String ROUTING_KEY_TEXT;
    private String ROUTING_KEY_FILE;
    private String ipv4Publico;
    private Consumer consumer;
    private Scanner sc;
    private int existeDownloads; //booleano para identificar se o diretório de download foi criado
    
    public Cliente(String host, String ipv4Publico, String username) {
        this.ipv4Publico = ipv4Publico;
        this.username = username;
        receptorAtual = "";
        grupoAtual = "";
        existeDownloads = 0;
        ROUTING_KEY_TEXT = "text";
        ROUTING_KEY_FILE = "file";
        try {
            init_comunicacao(host);
            init_consumer();
        } catch(Exception e) {
        }
    }
    
    private void init_comunicacao(String host) throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host); 
        factory.setUsername("admin"); 
        factory.setPassword("password"); 
        factory.setVirtualHost("/");

        connection = factory.newConnection();
        channel = connection.createChannel(); //cria canal
        channelFile = connection.createChannel(); 
        QUEUE_NAME = "fila@" + username;
        FILE_QUEUE_NAME = "filaArquivo@" + username;
        //(queue-name, durable, exclusive, auto-delete, params); 
        channel.queueDeclare(QUEUE_NAME, false,   false,     false,       null);
        channelFile.queueDeclare(FILE_QUEUE_NAME, false,   false,     false,       null);
    }


    private void criaDiretorioDownloads() {
        if (existeDownloads == 0) {
            new File("/home/ubuntu/environment/downloads").mkdirs();
            existeDownloads = 1;
        }
    }

    public void init_consumer() throws Exception{

        
        
        channel.basicConsume(QUEUE_NAME, true,  new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                MensagemProto.Mensagem mensagem = MensagemProto.Mensagem.parseFrom(body);
                String emissor = mensagem.getEmissor();
                String data = mensagem.getData();
                String hora = mensagem.getHora();
                String grupo = mensagem.getGrupo();
                MensagemProto.Conteudo conteudo = mensagem.getConteudo();
                String corpo = conteudo.getCorpo().toStringUtf8();
                String mensagemFormatada = "";
                
                if (emissor.equals(username) == false) { //para o emissor não receber a própria mensagem
                    if (grupo.equals("")) {
                        mensagemFormatada = "(" + data + " as " + hora + ") " + emissor + " diz: " + corpo;
                        System.out.println(mensagemFormatada);
                        System.out.print(receptorAtual + ">> ");
                    }
                    else {
                        mensagemFormatada = "(" + data + " as " + hora + ") " + emissor + "#" + grupo + " diz: " + corpo;
                        System.out.println(mensagemFormatada);
                        System.out.print("#" + grupoAtual + ">> ");
                    }
                }
            }
        });
        
        channel.basicConsume(FILE_QUEUE_NAME, true,  new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                MensagemProto.Mensagem mensagem = MensagemProto.Mensagem.parseFrom(body);
                String emissor = mensagem.getEmissor();
                String data = mensagem.getData();
                String hora = mensagem.getHora();
                String grupo = mensagem.getGrupo();
                MensagemProto.Conteudo conteudo = mensagem.getConteudo();
                String tipo = conteudo.getTipo();
                String nome = conteudo.getNome();
                String mensagemFormatada = "";
                
                if (emissor.equals(username) == false) { //para o emissor não receber o próprio arquivo
                    criaDiretorioDownloads();
                    byte[] corpo = conteudo.getCorpo().toByteArray();
                    try (FileOutputStream fos = new FileOutputStream("/home/ubuntu/environment/downloads/" + nome)) {
                        fos.write(corpo);
                    }
                    if (grupo.equals("")) {
                        mensagemFormatada = "\n(" + data + " as " + hora + ") " + "Arquivo " + nome + " recebido de " + emissor;
                        System.out.println(mensagemFormatada);
                        System.out.print(receptorAtual + ">> ");
                    }
                    else {
                        mensagemFormatada = "\n(" + data + " as " + hora + ") " + "Arquivo " + nome + " recebido de " + emissor + "#" + grupo;
                        System.out.println(mensagemFormatada);
                        System.out.print("#" + grupoAtual + ">> ");
                    }
                }
                

            }
        });
    }
    
    public void setReceptor(String receptor) {
        receptorAtual = receptor;
    }
    
    public void setGrupoAtual(String nomeGrupo) {
        grupoAtual = nomeGrupo;
    }
    
    public String getReceptor() {
        return receptorAtual;
    }
    
    public String getGrupoAtual() {
        return grupoAtual;
    }
    
    public byte[] criaBufferMensagem(String tipo, byte[] corpo, String nome, String emissor, String grupo) throws Exception {
        Date dataAtual = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        String dataFormatada = dateFormat.format(dataAtual);
        String horaFormatada = timeFormat.format(dataAtual);
        
        MensagemProto.Conteudo.Builder builderConteudo = MensagemProto.Conteudo.newBuilder();
        builderConteudo.setTipo(tipo);
        builderConteudo.setCorpo(ByteString.copyFrom(corpo));
        builderConteudo.setNome(nome);
        
        MensagemProto.Mensagem.Builder builderMensagem = MensagemProto.Mensagem.newBuilder();
        builderMensagem.setEmissor(username);
        builderMensagem.setData(dataFormatada);
        builderMensagem.setHora(horaFormatada);
        builderMensagem.setGrupo(grupo);
        
        builderMensagem.setConteudo(builderConteudo);
        
        MensagemProto.Mensagem mensagem = builderMensagem.build();
        
        byte[] buffer = mensagem.toByteArray();
        
        return buffer;
    }
    
    
    public void enviarMensagem(byte[] mensagemCorpo, int paraGrupo) throws Exception{ //montar json na origem
        if (paraGrupo == 1) {
            byte[] buffer = criaBufferMensagem("text/plain", mensagemCorpo, "", username, grupoAtual);
            channel.basicPublish(grupoAtual, ROUTING_KEY_TEXT, null, buffer);
        }
        else {
            byte[] buffer = criaBufferMensagem("text/plain", mensagemCorpo, "", username, "");
            channel.basicPublish("", "fila" + receptorAtual, null,  buffer);
        }
    }
    
    
    
    public void addUser(String nomeUser, String nomeGrupo)  throws Exception{
        channel.queueBind("fila@" + nomeUser, nomeGrupo, ROUTING_KEY_TEXT);
        channelFile.queueBind("filaArquivo@" + nomeUser, nomeGrupo, ROUTING_KEY_FILE);
    }
    
    public void addGroup(String nomeGrupo)  throws Exception{
        channel.exchangeDeclare(nomeGrupo, "direct", true);
        channelFile.exchangeDeclare(nomeGrupo, "direct", true);
        addUser(username, nomeGrupo); //o criador do grupo é adicionado
    }
    
    public void delFromGroup(String nomeUser, String nomeGrupo) throws Exception{
        channel.queueUnbind("fila@" + nomeUser, nomeGrupo, ROUTING_KEY_TEXT);
        channelFile.queueUnbind("filaArquivo@" + nomeUser, nomeGrupo, ROUTING_KEY_FILE);
    }
    
    public void removeGroup(String nomeGrupo) throws Exception{
        channel.exchangeDelete(nomeGrupo);
        channelFile.exchangeDelete(nomeGrupo);
    }
    
    public void uploadFile(String path, int paraGrupo) throws Exception{
        File file = new File(path);
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        Path source = Paths.get(path);
        String tipoMime = Files.probeContentType(source);
        String nome = source.getFileName().toString();
        if (paraGrupo == 1) {
            System.out.println("Enviando " + path + " para " + grupoAtual);
            byte[] buffer = criaBufferMensagem(tipoMime, fileBytes, nome, username, grupoAtual);
            Uploader upGrupo = new Uploader(channelFile, buffer, "", grupoAtual, ROUTING_KEY_FILE, 1); 
            upGrupo.start();
        }
        else {
            System.out.println("Enviando " + path + " para " + receptorAtual);
            byte[] buffer = criaBufferMensagem(tipoMime, fileBytes, nome, username, "");
            Uploader upReceptor = new Uploader(channelFile, buffer, receptorAtual, "", "", 0); 
            upReceptor.start();
            
        }
    } 
    
    public void listUsers(String nomeGrupo) throws Exception {
        String username = "admin";
        String password = "password";
     
        String usernameAndPassword = username + ":" + password;
        String authorizationHeaderName = "Authorization";
        String authorizationHeaderValue = "Basic " + java.util.Base64.getEncoder().encodeToString( usernameAndPassword.getBytes() );

        String restResource = "http://" + ipv4Publico + ":15672";
        Client client = ClientBuilder.newClient();
        Response resposta = client.target( restResource )
                .path("/api/exchanges/%2f/" + nomeGrupo + "/bindings/source")
            	.request(MediaType.APPLICATION_JSON)
                .header( authorizationHeaderName, authorizationHeaderValue ) // autenticação básica
                .get();     // executa um post com os valores preenchidos
        if (resposta.getStatus() == 200) {
            String json = resposta.readEntity(String.class);
            
            //mapeia o string json para uma lista de objetos binding
            ObjectMapper mapper = new ObjectMapper();
            List<Binding> bindingList = mapper.readValue(json, new TypeReference<List<Binding>>(){});
            
            String usersGrupo = "";
            for (Binding b : bindingList) {
                if (b.getProperties_key().equals("text")) { //se o binding for fila de texto
                    usersGrupo += b.getDestination().substring(5) + ", "; //adiciona usuario a string
                }
            }
            
            System.out.println(usersGrupo.substring(0, usersGrupo.length() - 2));
            
        } else {
            System.out.println(resposta.getStatus());
        }   
    }
    
    public void listGroups() throws Exception  {
        String username = "admin";
        String password = "password";
     
        String usernameAndPassword = username + ":" + password;
        String authorizationHeaderName = "Authorization";
        String authorizationHeaderValue = "Basic " + java.util.Base64.getEncoder().encodeToString( usernameAndPassword.getBytes() );

        String restResource = "http://" + ipv4Publico + ":15672";
        Client client = ClientBuilder.newClient();
        Response resposta = client.target( restResource )
                .path("/api/queues/%2f/" + QUEUE_NAME + "/bindings")
            	.request(MediaType.APPLICATION_JSON)
                .header( authorizationHeaderName, authorizationHeaderValue ) // autenticação básica
                .get();     // executa um post com os valores preenchidos
        if (resposta.getStatus() == 200) {
            String json = resposta.readEntity(String.class);
            
            //mapeia o string json para uma lista de objetos binding
            ObjectMapper mapper = new ObjectMapper();
            List<Binding> bindingList = mapper.readValue(json, new TypeReference<List<Binding>>(){});
            
            String grupos = "";
            for (Binding b : bindingList) {
                if (!b.getSource().equals("")) { //se o binding for fila de texto
                    grupos += b.getSource() + ", "; //adiciona usuario a string
                }
            }
            
            System.out.println(grupos.substring(0, grupos.length() - 2));
            
        } else {
            System.out.println(resposta.getStatus());
        }   
    }
}