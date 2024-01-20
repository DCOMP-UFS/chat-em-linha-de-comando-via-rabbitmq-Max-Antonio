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
    
    Cliente cliente = new Cliente("ec2-54-167-43-53.compute-1.amazonaws.com", username);
    cliente.init_consumer();    

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
