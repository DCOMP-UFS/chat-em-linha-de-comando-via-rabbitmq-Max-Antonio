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
    
    Cliente cliente = new Cliente("ec2-52-201-240-99.compute-1.amazonaws.com", username);
    cliente.init_consumer();    
    
    char estadoAtual = 'i';
    char tipoComando;
    
    while (true) {
        if (estadoAtual == 'i') { //inicial
            System.out.print(">> ");
        }
        else if (estadoAtual == '@') {
            System.out.print(cliente.getReceptor() + ">> ");
        }
        else if (estadoAtual == '#') {
            System.out.print("#" + cliente.getGrupoAtual() + ">> ");
        }
        String novaLinha = sc.nextLine();
        
        if (novaLinha.length() > 0) {
            tipoComando = novaLinha.trim().charAt(0);
        }
        else {
            tipoComando = 'n';
        }
        
        switch(tipoComando) {
            case '@':
                cliente.setReceptor(novaLinha.trim());
                estadoAtual = '@';
                break;
            case '!':
                String[] palavras = novaLinha.trim().split("\\s+");
                if ("!addGroup".equals(palavras[0])) {
                    cliente.criaGrupo(palavras[1]);
                }
                else if("!addUser".equals(palavras[0])) {
                    cliente.addUser(palavras[1], palavras[2]);
                }
                break;
            case '#':
                cliente.setGrupoAtual(novaLinha.trim().substring(1));
                estadoAtual = '#';
                break;
            case 'n': //não faz nada
                break;
            default:
                if (estadoAtual == '@') {
                    cliente.enviarMensagem(novaLinha);
                }
                else if (estadoAtual == '#') {
                    cliente.enviarMensagemGrupo(novaLinha);
                }
        }
    }
  }
}
