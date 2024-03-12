package br.ufs.dcomp.ChatRabbitMQ;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class Binding { 
    private String source;
    private String vhost;
    private String destination;
    private String destination_type;
    private String routing_key;
    
    @JsonIgnore
    private String arguments;
    
    private String properties_key;
    
    public String getSource() {
        return source;
    }
    
    public String getVhost() {
        return vhost;
    }
    
    public String getDestination() {
        return destination;
    }
    
    public String getDestination_type() {
        return destination_type;
    }
    
    public String getRouting_key() {
        return routing_key;
    }
    
    public String getArguments() {
        return arguments;
    }
    
    public String getProperties_key() {
        return properties_key;
    }
    
    public void setSource(final String source) {
        this.source = source;
    }
    
    public void setVhost(final String vhost) {
        this.vhost = vhost;
    }
    
    public void setDestination(final String destination) {
        this.destination = destination;
    }
    
    public void setDestination_type(final String destination_type) {
        this.destination_type = destination_type;
    }
    
    public void setRouting_key(final String routing_key) {
        this.routing_key = routing_key;
    }
    
    public void setArguments(final String arguments) {
        this.arguments = arguments;
    }
    
    public void setProperties_key(final String properties_key) {
        this.properties_key = properties_key;
    }
}