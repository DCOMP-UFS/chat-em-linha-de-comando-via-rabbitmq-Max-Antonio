package br.ufs.dcomp.ChatRabbitMQ;
import com.rabbitmq.client.*;

public class Uploader extends Thread {
    
    private Channel channel;
    private byte[] buffer;
    private String receptorAtual;
    private String grupoAtual;
    private String ROUTING_KEY_FILE;
    private int paraGrupo;
    
    public Uploader(Channel channel, byte[] buffer, String receptorAtual, String grupoAtual, String ROUTING_KEY_FILE, int paraGrupo) {
        this.channel = channel;
        this.buffer = buffer;
        this.receptorAtual = receptorAtual;
        this.grupoAtual = grupoAtual;
        this.ROUTING_KEY_FILE = ROUTING_KEY_FILE;
        this.paraGrupo = paraGrupo;
    }
    
    public void run() {
        try {
            if (paraGrupo == 1) {
                channel.basicPublish(grupoAtual, ROUTING_KEY_FILE, null, buffer);
            }
            else {
                channel.basicPublish("", "filaArquivo" + receptorAtual, null,  buffer);
            }
        } catch (Exception e){
            
        }
    }
}