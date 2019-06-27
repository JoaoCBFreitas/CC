/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.File;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import static java.util.Objects.hash;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author João
 */
class TransfereCC {
    //Declaracao da pasta de envio
    public static final String SEND_FOLDER = "";//"send/";
    //Declaracao da pasta de recebimento
    public static final String RECEIVE_FOLDER = "";//"receive/";
    
    //Declaracao do número máximo de pacotes correspondente a um Data Block
    //Usado para controlo de fluxo
    public final static int MAX_PENDING_PACKETS = 100;  
    //Declaração do tempo máximo de espera por um ACK
    public final static int MAX_WAIT_ACK=2000;
    
    //Variável correspondente ao AgenteUDP
    private AgenteUDP udp;
    //Variável correspondente ao estado
    private Estado estado;
    
    //Construtor que recebe como parâmetro um agenteUDP e define nesse agente
    //o transfereCC.
    public TransfereCC(AgenteUDP udp){
        this.udp=udp;
        udp.setTransfereCC(this);
    }
    
    //Método que faz o tratamento do PDU a enviar para a utilização do comando GET
    void getFile(InetAddress address, int port, String ficheiro) {

        try {
            estado = new Estado("GET");
            
            String metadata = "GET;" + ficheiro;
            
            byte[] metadataBytes = metadata.getBytes("UTF-8");
            
            PDU pdu = new PDU(PDU.TYPE_GETFILE, metadataBytes.length, -1, metadataBytes);
            
            udp.send(address, port, pdu);
        } catch (Exception ex) {
            System.out.println("TransfereCC.getFile ERRO: " + ex);
        }
    }


    //Método que envia o ficheiro dado um address, um port e um nome de ficheiro
    //Neste método é feito todo o tratamento dos PDU's a enviar, tal como o envio
    //do PUT que corresponde ao envio do pacote que contêm a informação do pacote
    //a enviar, o envio dos dados a serem transferidos e o envio do Checksum 
    //para verificação de integridade e sinalização do fim de envio do ficheiro.
    //De notar que durante todo o método, o estado vai sendo utilizado e 
    //manipulado de acordo com as necessidades da aplicação.
    void sendFile(InetAddress address, int port, String ficheiro) throws Exception {

        MessageDigest md5 = MessageDigest.getInstance("MD5");

        estado = new Estado("PUT");
        
        RandomAccessFile f=null;
        try {
            f = new RandomAccessFile(SEND_FOLDER + ficheiro, "r");
            
            int fileLength = (int) f.length(); 
            int numPackets = fileLength / AgenteUDP.UDP_PACKET_SIZE; 
            
            if (fileLength > numPackets * AgenteUDP.UDP_PACKET_SIZE)
                numPackets++;
            
            String metadata = "PUT;" + ficheiro + ";" + fileLength + ";" + numPackets + ";" + AgenteUDP.UDP_PACKET_SIZE;
            
            byte[] metadataBytes = metadata.getBytes("UTF-8");
            
            PDU pdu = new PDU(PDU.TYPE_PUTFILE, metadataBytes.length, -1, metadataBytes);
            System.out.println(metadataBytes);
            
            estado.setEstado(Estado.WAIT_ACK_PUT);
            
            udp.send(address, port, pdu); 
            
            long waitStart = System.currentTimeMillis();
            while (System.currentTimeMillis() - waitStart < MAX_WAIT_ACK 
                    && !estado.getEstado().equals(Estado.ACK_PUT_RECEIVED)){
                Thread.sleep(100);
            }
            
            if (!estado.getEstado().equals(Estado.ACK_PUT_RECEIVED))
                throw new Exception("Host Remoto recusou transferencia");
            
            byte[] buffer = new byte[AgenteUDP.UDP_PACKET_SIZE];
            
            int offset = 0;
            int len;
            int sentPackets=0;
            
            while(offset<fileLength){
                //envio de MAX_PENDING_PACKETS todos seguidos
                while(offset<fileLength && sentPackets<MAX_PENDING_PACKETS){
                    f.seek(offset);
                    len = f.read(buffer, 0, AgenteUDP.UDP_PACKET_SIZE);
                    
                    //atualiza-se o md5 com este pacote
                    md5.update(buffer,0,len);

                    pdu = new PDU(PDU.TYPE_DATA, len, offset, buffer);

                    estado.addSentPacket(new EstadoPacket(System.currentTimeMillis(), offset));

                    //if (Math.random()>0.1). Serve para criar perda de pacotes caso se pretenda.
                        udp.send(address, port, pdu);

                    sentPackets++;
                    offset+=AgenteUDP.UDP_PACKET_SIZE;

                }
                
                boolean wait=true;
                while(estado.getEstadoPackets().size()>0){
                    wait=false;
                    //falta por um retryCount
                    ConcurrentHashMap<Integer, EstadoPacket> packets = estado.getEstadoPackets();
                    
                    for(int packetOffset : packets.keySet()){
                        EstadoPacket ep;
                        synchronized(estado){
                            ep = packets.get(packetOffset);
                        }
                        //o pacote pode ter sido entretanto removido
                        if (ep!=null){
                            if (System.currentTimeMillis()-ep.sentTime > MAX_WAIT_ACK ){
                                //Reenvio do pacote
                                f.seek(packetOffset);
                                len = f.read(buffer, 0, AgenteUDP.UDP_PACKET_SIZE);

                                pdu = new PDU(PDU.TYPE_DATA, len, packetOffset, buffer);
                                udp.send(address, port, pdu);

                                ep.setSentTime(System.currentTimeMillis());
                            }                            
                        }
                        wait=true;
                    }
                    if (wait)
                        Thread.sleep(50);
                }
                
                sentPackets=0;
            }
            
            f.close();
            
            byte[] hash = md5.digest();
            
            pdu = new PDU(PDU.TYPE_CHECKSUM, hash.length, 0, hash);
            udp.send(address, port, pdu);
            
            //String hasHexa = String.format("%032x", new BigInteger(1,hash));
            
        } catch (Exception ex) {
            throw (ex);
        }
        
        //notificar a main thread que acabou o PUT
        udp.interrupt();
        
        estado.setComplete(true);
    }

    //Método que trata do recebimento de PDU's com recurso a métodos auxiliares
    //que fazem o tratamento do mesmo de acordo com o tipo de PDU recebido.
    void receive(InetAddress address, int port, PDU pdu) {
        switch(pdu.getType()){
            case PDU.TYPE_ACK:
                handleACK(address, port, pdu);
                break;
            case PDU.TYPE_DATA:
                handleData(address, port, pdu);
                break;
            case PDU.TYPE_PUTFILE:
                handlePutMetadata(address, port, pdu);
                break;
            case PDU.TYPE_CHECKSUM:
                handleChecksum(address, port, pdu);
                break;
            case PDU.TYPE_GETFILE:
                handleGetMetadata(address, port, pdu);
                break;
        }
    }
    
    //Método que faz o tratamento dos PDU's correspondentes ao tipo ACK.
    private void handleACK(InetAddress address, int port, PDU pdu) {
        try {
            String message = new String(pdu.getData(), "UTF-8");

            System.out.println("RECEIVED ACK: " + message + ", " + pdu.getOffset());

            switch(message){
                case "RECEIVED_PUT_REQUEST":
                    if (estado==null)
                        return;

                    estado.setEstado(Estado.ACK_PUT_RECEIVED);
                    break;
                case "RECEIVED_DATA_BLOCK":
                    if (estado==null)
                        return;
                    
                    estado.setPacketAck(pdu.getOffset());
                    break;
                    
                case "GET_REQUEST_OK":
                    System.out.println("GET Aceite, à espera do PUT");
                    break;
                
                case "GET_REQUEST_ERROR_FILE_NOT_FOUND":
                    System.out.println("GET Recusado: Ficheiro não encontrado");
                    break;
            }
        } catch (Exception ex) {
            System.out.println("TrasnfereCC.receive: " + ex);
        }
    }
    
    //Método que faz o tratamento dos PDU's correspondentes ao tipo PUT.
    private void handlePutMetadata(InetAddress address, int port, PDU pdu) {
 
        try {
            String message = new String(pdu.getData(), "UTF-8");
            
            System.out.println("RECEIVED Metadata: " + message);
            
            String[] parts = message.split(";");
            
            if (parts[0].equals("PUT")){
                //"PUT;" + ficheiro + ";" + fileLength + "; " + numPackets + ";" + AgenteUDP.UDP_PACKET_SIZE;
                //o estado pode ter sido iniciado por um GET
                //se nao exsite ou nao é um get cria-se um novo estado
                if (estado==null || !estado.getComando().equals("GET"))
                    estado = new Estado("RECEIVE_PUT");
                
                estado.setFilename(parts[1]);
                estado.setFileLength(Integer.parseInt(parts[2]));
                estado.setNumPackets(Integer.parseInt(parts[3]));
                estado.setPacketSize(Integer.parseInt(parts[4]));

                byte[] data = "RECEIVED_PUT_REQUEST".getBytes("UTF-8");

                PDU reply = new PDU(PDU.TYPE_ACK, data.length, 0, data);

                udp.send(address, port, reply);
                
                File f = new File(estado.getFilename());
                if (f.exists())
                    f.delete();
            }    
        } catch (UnsupportedEncodingException ex) {    
        }
    }
    
    //Método que faz o tratamento dos PDU's correspondentes ao tipo Data, os 
    // dados a transferir.
    private void handleData(InetAddress address, int port, PDU pdu) {
        
        if (estado==null)
            return;
        
        try {
            
            System.out.printf("RECEIVED DATA: %d, %d\n", pdu.getOffset(), pdu.getLength());
            
            RandomAccessFile file = new RandomAccessFile(RECEIVE_FOLDER+estado.getFilename(), "rw");
            
            file.seek(pdu.getOffset());
            file.write(pdu.getData(), 0, pdu.getLength());
            
            file.close();
            
            byte[] data = "RECEIVED_DATA_BLOCK".getBytes("UTF-8");

            PDU reply = new PDU(PDU.TYPE_ACK, data.length, pdu.getOffset(), data);

            udp.send(address, port, reply);
            
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    //Método que faz o tratamento dos PDU's correspondentes ao tipo Checksum
    //para verificação da integridade dos ficheiros enviados.
    private void handleChecksum(InetAddress address, int port, PDU pdu) {
        try {
            //o ficheiro em principio foi recebido integralmente
            //calcula-se o checksum do ficheiro recebido
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            
            
            RandomAccessFile f = new RandomAccessFile(RECEIVE_FOLDER + estado.getFilename(), "r");
            
            int fileLength = (int) f.length();
            byte[] buffer = new byte[AgenteUDP.UDP_PACKET_SIZE];
            
            int offset = 0;
            while(offset<fileLength){
                f.seek(offset);
                int len = f.read(buffer, 0, AgenteUDP.UDP_PACKET_SIZE);
                
                //atualiza-se o md5 com este pacote
                md5.update(buffer,0,len);
                offset+=AgenteUDP.UDP_PACKET_SIZE;
            }
            f.close();
            
            byte[] hash = md5.digest();
            int i;
            for (i = 0; i < hash.length; i++) {
                if (hash[i]!=pdu.getData()[i])
                    break;
            }
            
            if (i == hash.length){
                System.out.println("Ficheiro Recebido com sucesso");
            }else{
                System.out.println("Ficheiro Recebido não possui mesmo checksum");
            }
            
            //se o estado resulta de um GET
            if (estado.getComando().equals("GET")){
                estado.setComplete(true);
            }
            
        } catch (Exception ex) {
            System.out.println("TransfereCC.handleChecksum erro:" + ex);
        }
    }

    //Método que faz o tratamento dos PDU's correspondentes ao tipo GET.
    private void handleGetMetadata(InetAddress address, int port, PDU pdu) {
        try {
            String message = new String(pdu.getData(), "UTF-8");
            
            System.out.println("RECEIVED Metadata: " + message);
            
            String[] parts = message.split(";");
            
            if (parts[0].equals("GET")){
                byte[] data;
                
                boolean sendFile=false;
                
                if (new File(SEND_FOLDER+parts[1]).exists()){
                    data = "GET_REQUEST_OK".getBytes("UTF-8");
                    sendFile=true;
                }else{
                    data = "GET_REQUEST_ERROR_FILE_NOT_FOUND".getBytes("UTF-8");
                }
                
                PDU reply = new PDU(PDU.TYPE_ACK, data.length, 0, data);
                udp.send(address, port, reply);
                
                if (sendFile){
                    //neste momento esta thread é a do AgenteUDP por isso 
                    //temos de invocar o put noutra thread
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                sendFile(address, port, parts[1]);
                            } catch (Exception ex) {
                                System.out.println("Erro ao enviar o ficheiro em resposta ao GET: " + ex);
                            }
                        }
                    }).start();
                }
            }               
        } catch (Exception ex) {
            System.out.println("TransfereCC.handleGetmetdata ERRO:" + ex);
        }
    }

    boolean complete() {
        return estado!=null && estado.isComplete();
    }  
}
