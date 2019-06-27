/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

/**
 *
 * @author João
 */
public class AgenteUDP extends Thread {
    //Declaração do tamanho dos dados de cada pacote
    public final static int UDP_PACKET_SIZE = 512;
    
    //Variável correspondente ao localPort
    private int localPort;
    //Variável correspondente ao transfereCC
    private TransfereCC transfereCC;
    //Variável correspndete à socket
    private DatagramSocket socket;
    
    //Construtor pro parâmetros do AgenteUDP
    public AgenteUDP(int localPort) throws SocketException {
        this.localPort=localPort;
        socket = new DatagramSocket(localPort);
    }
    
    //Set do transfereCC
    public void setTransfereCC(TransfereCC transfereCC) {
        this.transfereCC = transfereCC;
    }

    //Para receber os pacotes dos hosts remotos e enviar para o transfereCC
    //o pacote recebido após o tratamento do mesmo. 
    @Override
    public void run(){
        //Para receber os pacotes dos hosts remotos
        try{
            

            byte[] buffer = new byte[2048];

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (true) {
                
                socket.receive(packet);
                //Para teste
                System.out.println("udp.receive:"+packet.getLength()+","+packet.getOffset());
                
                int type;
                byte[] data = packet.getData();
                switch(data[0]){
                    case 1:
                        type=PDU.TYPE_ACK;
                        break;
                    case 2:
                        type=PDU.TYPE_DATA;
                        break;
                    case 3:
                        type=PDU.TYPE_PUTFILE;
                        break;
                    case 4:
                        type=PDU.TYPE_CHECKSUM;
                        break;
                    case 5:
                        type=PDU.TYPE_GETFILE;
                        break;
                        
                    default:
                        type=0;
                }
                byte[] length = new byte[4];
                byte[] ofset = new byte[4];

                copyBytes(length,data,0,1,4);
                copyBytes(ofset,data,0,5,4);

                int len=byte2int(length);
                int off=byte2int(ofset);
                
                byte[] pd = new byte[packet.getLength()-9];
                copyBytes(pd,data,0,9,packet.getLength()-9);
                
                PDU pdu = new PDU(type, len, off, pd);
        
                if (transfereCC!=null)
                    transfereCC.receive(packet.getAddress(), packet.getPort(), pdu);

                packet.setLength(buffer.length);
            }
        } catch (Exception e) {
            System.err.println(e);
        }
    }    
    
    //Método que trata do envio de um pacote dado um address, um port
    //um array com os dados a enviar e um tamanho
    public boolean send(InetAddress address, int port, byte[] data, int length){
        try{
                
            DatagramPacket packet = new DatagramPacket(data, length, address, port);
            
            socket.send(packet);       
            
            return true;
        } catch (Exception e) {
            System.err.println(e);
            return false;
        }
    }
    
    //Método que codifica um pacote a enviar dado um address, um port e um pdu.
    //É realizada a conversão para bytes recorrendo a métodos auxiliares.
    //Primeiro indice do array corresponde ao tipo de pdu a enviar!
    void send(InetAddress address, int port, PDU pdu) {

        byte[] data = new byte[pdu.getData().length + 9];
        
        switch (pdu.getType()){
            case PDU.TYPE_ACK:
                System.out.println("SEND ACK");
                data[0] = 1;
                break;
            case PDU.TYPE_DATA:
                System.out.printf("SEND DATA: %d, %d\n", pdu.getOffset(), pdu.getLength());
                data[0] = 2;
                break;
            case PDU.TYPE_PUTFILE:
                System.out.println("SEND METADATA:" + new String(pdu.getData()));
                data[0] = 3;
                break;
            case PDU.TYPE_CHECKSUM:
                System.out.println("SEND CHECKSUM:" + new String(pdu.getData()));
                data[0] = 4;
                break;
            case PDU.TYPE_GETFILE:
                System.out.println("SEND GET METADATA:" + new String(pdu.getData()));
                data[0] = 5;
                break;
                
        }

        byte[] length = int2byte(pdu.getLength());
        byte[] ofset = int2byte(pdu.getOffset());
        
        copyBytes(data,length,1,0,4);
        copyBytes(data,ofset,5,0,4);
        
        copyBytes(data,pdu.getData(),9,0,pdu.getData().length);
        
        send(address, port, data, data.length);
    }

    //Método auxiliar para conversão de inteiros para bytes
    private byte[] int2byte(int i){
        
        byte[] result = new byte[4];

        result[0] = (byte) (i >> 24);
        result[1] = (byte) (i >> 16);
        result[2] = (byte) (i >> 8);
        result[3] = (byte) (i);

        return result;
    }

    //Método auxiliar para conversão de bytes para inteiros
    private int byte2int(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }
    
    //Método auxiliar para copia de arrays. arrayCopy??
    private void copyBytes(byte[] dest, byte[] source, int destOfset, int sourceOfset, int len) {
        for (int j = 0; j < len; j++) {
            dest[destOfset+j]=source[sourceOfset+j];
        }
    }



} 
