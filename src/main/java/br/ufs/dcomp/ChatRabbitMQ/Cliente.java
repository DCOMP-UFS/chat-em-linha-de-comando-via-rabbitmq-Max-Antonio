package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;

import java.text.SimpleDateFormat;

import java.util.Date;

import java.io.*;

import java.util.*;

import com.google.protobuf.util.JsonFormat;

import com.google.protobuf.ByteString;

import java.nio.file.*;

public class Cliente {
    
    private Connection connection;
    private Channel channel;
    private String username;
    private String receptorAtual; //usuario que o usuario atual está mandando mensagem
    private String grupoAtual;
    private String QUEUE_NAME; //fila de mensagens usuário
    private String FILE_QUEUE_NAME; //fila de arquivos do usuário
    private Consumer consumer;
    private Scanner sc;
    
    public Cliente(String host, String username) {
        this.username = username;
        receptorAtual = "";
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
        QUEUE_NAME = "fila@" + username;
        FILE_QUEUE_NAME = "filaArquivo@" + username;
        //(queue-name, durable, exclusive, auto-delete, params); 
        channel.queueDeclare(QUEUE_NAME, false,   false,     false,       null);
        channel.queueDeclare(FILE_QUEUE_NAME, false,   false,     false,       null);
    }

    public void init_consumer() throws Exception{
        consumer = new DefaultConsumer(channel) {
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)  throws IOException {

        MensagemProto.Mensagem mensagem = MensagemProto.Mensagem.parseFrom(body);
        String emissor = mensagem.getEmissor();
        String data = mensagem.getData();
        String hora = mensagem.getHora();
        String grupo = mensagem.getGrupo();
        
        MensagemProto.Conteudo conteudo = mensagem.getConteudo();
        String tipo = conteudo.getTipo();
        String nome = conteudo.getNome();
        
        String mensagemFormatada = "";
        
        if (tipo.equals("text/plain")) {
            String corpo = conteudo.getCorpo().toStringUtf8();
            if (grupo.equals("")) {
                mensagemFormatada = "(" + data + " as " + hora + ") " + emissor + " diz: " + corpo;
            }
            else {
                mensagemFormatada = "(" + data + " as " + hora + ") " + emissor + "#" + grupo + " diz: " + corpo;
            }
        }
        else {
            byte[] corpo = conteudo.getCorpo().toByteArray();
            try (FileOutputStream fos = new FileOutputStream("/home/Ambiente/ChatRabbitMQ/image.png")) {
                fos.write(corpo);
            }
        }
        
        
        
        
        
        if (emissor.equals(username) == false) { //para o emissor não receber a própria mensagem
            System.out.println(mensagemFormatada);
            System.out.print(receptorAtual + ">> ");
        }

        }
        };
        channel.basicConsume(QUEUE_NAME, true,    consumer);
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
            channel.basicPublish(grupoAtual, "", null, buffer);
        }
        else {
            byte[] buffer = criaBufferMensagem("text/plain", mensagemCorpo, "", username, "");
            channel.basicPublish("", "fila" + receptorAtual, null,  buffer);
        }
    }
    
    
    
    public void addUser(String nomeUser, String nomeGrupo)  throws Exception{
        channel.queueBind("fila@" + nomeUser, nomeGrupo, "");
        channel.queueBind("filaArquivo@" + nomeUser, nomeGrupo, "");
    }
    
    public void addGroup(String nomeGrupo)  throws Exception{
        channel.exchangeDeclare(nomeGrupo, "direct");
        addUser(username, nomeGrupo); //o criador do grupo é adicionado
    }
    
    public void delFromGroup(String nomeUser, String nomeGrupo) throws Exception{
        channel.queueUnbind("fila@" + nomeUser, nomeGrupo, "");
        channel.queueUnbind("filaArquivo@" + nomeUser, nomeGrupo, "");
    }
    
    public void removeGroup(String nomeGrupo) throws Exception{
        channel.exchangeDelete(nomeGrupo);
    }
    
    public void uploadFile(String path, int paraGrupo) throws Exception{
        File file = new File(path);
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        Path source = Paths.get(path);
        String tipoMime = Files.probeContentType(source);
        String nome = source.getFileName().toString();
        if (paraGrupo == 1) {
            byte[] buffer = criaBufferMensagem(tipoMime, fileBytes, "", username, grupoAtual);
            channel.basicPublish(grupoAtual, "", null, buffer);
        }
        else {
            byte[] buffer = criaBufferMensagem(tipoMime, fileBytes, "", username, "");
            channel.basicPublish("", "fila" + receptorAtual, null,  buffer);
        }
    } 
}