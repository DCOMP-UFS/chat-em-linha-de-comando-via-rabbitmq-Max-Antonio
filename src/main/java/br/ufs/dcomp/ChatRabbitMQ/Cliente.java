package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;

import java.util.Scanner; 

import java.text.SimpleDateFormat;

import java.util.Date;

import java.io.*;

public class Cliente {
    
    private Connection connection;
    private Channel channel;
    private String username;
    private String receptorAtual; //usuario que o usuario atual está mandando mensagem
    private String grupoAtual;
    private String QUEUE_NAME;
    private Consumer consumer;
    private static final String EXCHANGE_NAME = "logs";
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

        String message = new String(body, "UTF-8");
        System.out.println(message);
        System.out.print(receptorAtual + ">> ");

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
    
    public void enviarMensagem(String mensagem) throws Exception{ //montar json na origem
        Date dataAtual = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        String dataFormatada = dateFormat.format(dataAtual);
        String horaFormatada = timeFormat.format(dataAtual);
        
        /*
        
        MensagemProto.Conteudo.Builder builderConteudo = MensagemProto.Conteudo.newBuilder();
        builderConteudo.setTipo(    );
        builderConteudo.setCorpo(mensagemCorpo);
        builderConteudo.setNome(    );
        
        MensagemProto.Mensagem.Builder builderMensagem = MensagemProto.Mensagem.newBuilder();
        builderMensagem.setEmissor(username);
        builderMensagem.setData(dataFormatada);
        builderMensagem.setHora(horaFormatada);
        builderMensagem.setGrupo(             );
        builderMensagem.setConteudo(builderConteudo);
        
        */
        
        String mensagemFormatada = "\n(" + dataFormatada + " às " + horaFormatada + ") " + username + " diz: " + mensagem;
        
        channel.basicPublish("", "fila" + receptorAtual, null,  mensagemFormatada.getBytes("UTF-8"));
    }
    
    public void enviarMensagemGrupo(String mensagem) throws Exception{
        Date dataAtual = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        String dataFormatada = dateFormat.format(dataAtual);
        String horaFormatada = timeFormat.format(dataAtual);
        String mensagemFormatada = "\n(" + dataFormatada + " às " + horaFormatada + ") " + username + "#" + grupoAtual + " diz: " + mensagem;
        
        channel.basicPublish(grupoAtual, "", null, mensagemFormatada.getBytes("UTF-8"));
    }
    
    public void addUser(String nomeUser, String nomeGrupo)  throws Exception{
        channel.queueBind("fila@" + nomeUser, nomeGrupo, "");
    }
    
    public void criaGrupo(String nomeGrupo)  throws Exception{
        channel.exchangeDeclare(nomeGrupo, "fanout");
        addUser(username, nomeGrupo); //o criador do grupo é adicionado
    }
}
