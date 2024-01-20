package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;

import java.io.IOException;

import java.util.Scanner; 

import java.text.SimpleDateFormat;

import java.util.Date;

public class Cliente {
    
    private Connection connection;
    private Channel channel;
    private String username;
    private String receptorAtual; //usuario que o usuario atual está mandando mensagem
    private String QUEUE_NAME;
    private Consumer consumer;
    
    private Scanner sc;
    
    public Cliente(String host, String username) {
        this.username = username;
        try {
            init_comunicacao(host);
        } catch(Exception e) {
        }
    }
    
    private void init_comunicacao(String host) throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host); // Alterar
        factory.setUsername("admin"); // Alterar
        factory.setPassword("password"); // Alterar
        factory.setVirtualHost("/");

        connection = factory.newConnection();
        channel = connection.createChannel();
        QUEUE_NAME = "fila@" + username;
        //(queue-name, durable, exclusive, auto-delete, params); 
        channel.queueDeclare(QUEUE_NAME, false,   false,     false,       null);
    }
    

    public void init_consumer() throws Exception{
        consumer = new DefaultConsumer(channel) {
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)  throws IOException {

        String message = new String(body, "UTF-8");
        System.out.println(message);

        }
        };
        channel.basicConsume(QUEUE_NAME, true,    consumer);
    }

    
    public void setReceptor(String receptor) {
        receptorAtual = receptor;
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
    
    public void enviarMensagem(String mensagem) throws Exception{
        Date dataAtual = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        String dataFormatada = dateFormat.format(dataAtual);
        String tempoFormatado = timeFormat.format(dataAtual);
        String mensagemFormatada = "(" + dataFormatada + " às " + tempoFormatado + ") " + username + " diz: " + mensagem;
        
        channel.basicPublish("",       "fila" + receptorAtual, null,  mensagemFormatada.getBytes("UTF-8"));
    }
}
