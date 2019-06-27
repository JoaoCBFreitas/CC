/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author João
 */
class Estado {
    
    //Declaração de diferentes estados relativos a ACK's
    static final String WAIT_ACK_PUT = "Espera ACK put";
    static final String WAIT_ACK_GET = "Espera ACK get";
    static final String ACK_PUT_RECEIVED = "ACK put Recebido";
    static final String ACK_GET_RECEIVED = "ACK get Recebido";
    static final String ACK_GET_ERROR_RECEIVED = "ACK ERRO GET";
    
    //Coleçao que contem o offset dos pacotes enviados como Key's e o Estado dos mesmos como Values
    ConcurrentHashMap<Integer, EstadoPacket> sentPackets= new ConcurrentHashMap<>();
    
    //Variavel correspondente ao estado
    private String estado;
    //Variavel correspondente ao nome do ficheiro
    private String filename;
    //Variavel correspondente ao numero de pacotes a enviar
    private int numPackets;
    //Variavel correspondente ao tamanho do ficheiro a transferir
    private int fileLength;
    //Variavel correspondente ao tamanho de cada pacote
    private int packetSize;
    //Variavel correspondente ao comando inserido
    private final String comando;
    //Variavel que indica se a transferência foi concluída
    private boolean complete=false;

    //Construtor de estado que recebe um comando como parâmetro
    public Estado(String comando){
        this.comando=comando;
    }
    
    //Set do estado
    public void setEstado(String estado){
        synchronized(this){
            this.estado = estado;
        }
    }
    
    //Método que devolve o estado
    public String getEstado() {
        return estado;
    }
    
    //Método que adiciona um pacote enviado ao Map com os pacotes enviados
    void addSentPacket(EstadoPacket estadoPacket) {
        synchronized(this){
            sentPackets.put(estadoPacket.offset, estadoPacket);
        }
    }

    //Método que remove um pacote que foi confirmado por um Ack 
    //dos pacotes enviados dado o seu offset.
    void setPacketAck(int offset) {
        synchronized(this){
            sentPackets.remove(offset);
        }
    }
    
    //Set do nome do ficheiro
    void setFilename(String filename) {
        this.filename=filename;
    }
    
    //Set do nome do tamanho do ficheiro
    void setFileLength(int filelength) {
        this.fileLength=filelength;
    }
    
    //Set do numero de pacotes a enviar
    void setNumPackets(int numPackets) {
        this.numPackets=numPackets;
    }

    //Set do tamanho do pacote a enviar
    void setPacketSize(int packetSize) {
        this.packetSize=packetSize;
    }
    
    //Método que devolve o nome do ficheiro
    public String getFilename() {
        return filename;
    }
    
    //Método que devolve o numero de pacotes a enviar
    public int getNumPackets() {
        return numPackets;
    }

    //Método que devolve o tamanho do ficheiro
    public int getFileLength() {
        return fileLength;
    }

    //Método que devolve o tamanho do pacote a enviar
    public int getPacketSize() {
        return packetSize;
    }

    //Método que devolve uma coleção com os pacotes enviados
    public ConcurrentHashMap<Integer, EstadoPacket> getEstadoPackets() {
        return sentPackets;
    }

    //Método que devolve o comando inserido
    String getComando() {
        return comando;
    }

    //Set da conclusão da transferência do ficheiro
    public void setComplete(boolean complete) {
        this.complete = complete;
    }
    
    //Método que devolve o valor de complete
    boolean isComplete() {
        return complete;
    }

}
