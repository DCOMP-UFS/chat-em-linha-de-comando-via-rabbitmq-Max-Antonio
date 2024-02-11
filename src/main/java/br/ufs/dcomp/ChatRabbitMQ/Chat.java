package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;

import java.io.IOException;

import java.util.Scanner; 

public class Chat {
    
    private static void user_input(Cliente cliente, Scanner sc) throws Exception{
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
                        cliente.addGroup(palavras[1]);
                    }
                    else if("!addUser".equals(palavras[0])) {
                        cliente.addUser(palavras[1], palavras[2]);
                    }
                    else if ("!delFromGroup".equals(palavras[0])) {
                        cliente.delFromGroup(palavras[1], palavras[2]);
                    }
                    else if ("!removeGroup".equals(palavras[0])) {
                        cliente.removeGroup(palavras[1]);
                    }
                    else if ("!upload".equals(palavras[0])) {
                        if (estadoAtual == '#') {
                            cliente.uploadFile(palavras[1], 1);
                        }
                        else if (estadoAtual == '@') {
                            cliente.uploadFile(palavras[1], 0);
                        }
                    }
                    break;
                case '#':
                    cliente.setGrupoAtual(novaLinha.trim().substring(1));
                    estadoAtual = '#';
                    break;
                case 'n': //não faz nada
                    break;
                default:
                    byte[] mensagemCorpo = novaLinha.getBytes("UTF-8");
                    if (estadoAtual == '@') {
                        cliente.enviarMensagem(mensagemCorpo, 0);
                    }
                    else if (estadoAtual == '#') {
                        cliente.enviarMensagem(mensagemCorpo, 1);
                    }
        }
    }
  }

  public static void main(String[] argv) throws Exception {
    String username;
    Scanner sc = new Scanner(System.in);
    System.out.print("User: ");
    username = sc.nextLine();
    Cliente cliente = new Cliente("ec2-34-230-4-134.compute-1.amazonaws.com", username);
    user_input(cliente, sc);
  }
}
