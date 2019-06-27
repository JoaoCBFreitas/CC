/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author João
 */
public class EstadoPacket {
    //Variável correspondente ao tempo de envio do pacote
    public long sentTime;
    //Offset do 
    public final int offset;

    //Construtor por parâmetros do estado de pacote
    public EstadoPacket(long sentTime , int offset) {
        this.sentTime=sentTime;
        this.offset = offset;
    }
    
    //Set do tempo a que foi enviado o pacote
    public void setSentTime(long sentTime) {
        this.sentTime = sentTime;
    }

    
    
}
