package br.ufs.dcomp.ChatRabbitMQ;
import com.rabbitmq.client.*;

public class Uploader extends Thread {
    
    private Channel channelFile;
    private byte[] buffer;
    private String receptorAtual;
    private String grupoAtual;
    private String ROUTING_KEY_FILE;
    private int paraGrupo;
    
    public Uploader(Channel channelFile, byte[] buffer, String receptorAtual, String grupoAtual, String ROUTING_KEY_FILE, int paraGrupo) {
        this.channelFile = channelFile;
        this.buffer = buffer;
        this.receptorAtual = receptorAtual;
        this.grupoAtual = grupoAtual;
        this.ROUTING_KEY_FILE = ROUTING_KEY_FILE;
        this.paraGrupo = paraGrupo;
    }
    
    public void run() {
        try {
            if (paraGrupo == 1) {
                channelFile.basicPublish(grupoAtual, ROUTING_KEY_FILE, null, buffer);
            }
            else {
                channelFile.basicPublish("", "filaArquivo" + receptorAtual, null,  buffer);
            }
        } catch (Exception e){
            
        }
    }
}