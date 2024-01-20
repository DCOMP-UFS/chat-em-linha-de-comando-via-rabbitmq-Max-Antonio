package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;

import java.io.IOException;

import java.util.Scanner; 

public class Chat {

  public static void main(String[] argv) throws Exception {

    Scanner sc = new Scanner(System.in);
    String username;
    
    System.out.print("User: ");
    username = sc.nextLine();
    
    Cliente cliente = new Cliente("ec2-34-201-149-80.compute-1.amazonaws.com", username);
    
    
    Consumer consumer = new DefaultConsumer(cliente.getChannel()) {
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)  throws IOException {
            String message = new String(body, "UTF-8");
            System.out.println(message);
        }
    };
    
    cliente.getChannel().basicConsume(cliente.getQUEUE_NAME(), true,    consumer);
        
    System.out.print(">> ");
    String novoReceptor = sc.nextLine();
    cliente.setReceptor(novoReceptor.trim());
     while (true) {
        System.out.print(cliente.getReceptor() + ">> ");
        String novaLinha = sc.nextLine();
        if (novaLinha.trim().charAt(0) == '@') {
            cliente.setReceptor(novaLinha.trim());
        }
        else {
            cliente.enviarMensagem(novaLinha);
        }
    }
  }
}
