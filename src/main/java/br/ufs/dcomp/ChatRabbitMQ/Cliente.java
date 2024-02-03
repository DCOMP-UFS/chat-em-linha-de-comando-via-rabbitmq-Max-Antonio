package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;

import java.text.SimpleDateFormat;

import java.util.Date;

import java.io.*;

import java.util.*;

import com.google.protobuf.util.JsonFormat;

import com.google.protobuf.ByteString;

public class Cliente {
    
    private Connection connection;
    private Channel channel;
    private String username;
    private String receptorAtual; //usuario que o usuario atual está mandando mensagem
    private String grupoAtual;
    private String QUEUE_NAME;
    private Consumer consumer;
    private Scanner sc;
    
    public Cliente(String host, String username) {
        this.username = username;
        receptorAtual = "";
        try {
            init_comunicacao(host);
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
        //(queue-name, durable, exclusive, auto-delete, params); 
        channel.queueDeclare(QUEUE_NAME, false,   false,     false,       null);
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
        String corpo = conteudo.getCorpo().toStringUtf8();
        String nome = conteudo.getNome();
        
        String mensagemFormatada;
        if (grupo.equals("")) {
            mensagemFormatada = "(" + data + " as " + hora + ") " + emissor + " diz: " + corpo;
        }
        else {
            mensagemFormatada = "(" + data + " as " + hora + ") " + emissor + "#" + grupo + " diz: " + corpo;
        }
        
        if (emissor.equals(username)) { //para o emissor não receber a própria mensagem
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
    
    public Channel getChannel() {
        return channel;
    }
    
    public String getQUEUE_NAME() {
        return QUEUE_NAME;
    }
    
    public String getGrupoAtual() {
        return grupoAtual;
    }
    
    public void enviarMensagem(String mensagemCorpo, int paraGrupo) throws Exception{ //montar json na origem
        Date dataAtual = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        String dataFormatada = dateFormat.format(dataAtual);
        String horaFormatada = timeFormat.format(dataAtual);
        
        
        
        MensagemProto.Conteudo.Builder builderConteudo = MensagemProto.Conteudo.newBuilder();
        builderConteudo.setTipo("text/plain");
        builderConteudo.setCorpo(ByteString.copyFrom(mensagemCorpo.getBytes("UTF-8")));
        builderConteudo.setNome("");
        
        MensagemProto.Mensagem.Builder builderMensagem = MensagemProto.Mensagem.newBuilder();
        builderMensagem.setEmissor(username);
        builderMensagem.setData(dataFormatada);
        builderMensagem.setHora(horaFormatada);
        if (paraGrupo == 1) {
            builderMensagem.setGrupo(grupoAtual);
        }
        else {
            builderMensagem.setGrupo("");
        }
        builderMensagem.setConteudo(builderConteudo);
        
        MensagemProto.Mensagem mensagem = builderMensagem.build();
        
        byte[] buffer = mensagem.toByteArray();
         
        if (paraGrupo == 1) {
            channel.basicPublish(grupoAtual, "", null, buffer);
        }
        else {
            channel.basicPublish("", "fila" + receptorAtual, null,  buffer);
        }
        
    }
    
    
    public void addUser(String nomeUser, String nomeGrupo)  throws Exception{
        channel.queueBind("fila@" + nomeUser, nomeGrupo, "");
    }
    
    public void criaGrupo(String nomeGrupo)  throws Exception{
        channel.exchangeDeclare(nomeGrupo, "fanout");
        addUser(username, nomeGrupo); //o criador do grupo é adicionado
    }
}
