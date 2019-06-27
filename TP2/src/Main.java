/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author João
 */
//Classe correspondente à CmdLineApp
public class Main {
    //Variável correspondente ao UDP_PORT
    public static int UDP_PORT = 7777;
    //Variável correspondente ao port local
    private static int localPort;
    //Variável correspondente ao port remoto
    private static int remotePort;
    
    
    
    //Main da aplicação
    public static void main (String[] args){
        localPort = UDP_PORT;
        remotePort = UDP_PORT;
        
        if (args.length==0){
            System.out.println("Modos de uso: ");
            System.out.println("\nwait <localport>");
            System.out.println("\nget ficheiro address <localport,remoteport> ");
            System.out.println("\nput ficheiro address <localport,remoteport> ");
            return;
        }

        AgenteUDP udp=null;
        TransfereCC transfer;
        final String ficheiro;
        final String address;
        
        //definir porta local, porta remota e comando a inserir
        //wait para quem aguarda pelos comandos
        //put e get para quem pretende executar uma ação, seja 
        //de upload ou de download
        switch(args[0]){
            case "wait":
                if (args.length>1)
                    localPort=Integer.parseInt(args[1]);
                address=null;
                ficheiro=null;
                System.out.println("Modo servidor a aguardar comandos");
                break;
                
            case "get":
            case "put":
                if (args.length<3){
                    System.out.println("Comando invalido");
                    return;
                }
                
                ficheiro = args[1];
                address = args[2];
                
                if (args.length>3){
                    localPort = Integer.parseInt(args[3]);
                    remotePort = Integer.parseInt(args[4]);
                }
                break;
            default:
                System.out.println("Comando invalido");
                address=null;
                ficheiro=null;
                return;
        }
        
        try {
            //inicia-se a thread do udp
            udp = new AgenteUDP(localPort);
            udp.start();
        } catch (SocketException ex) {
            System.out.println("Erro ao iniciar o UDP: " + ex);
            System.exit(1);
        }

        transfer = new TransfereCC(udp);
        
        //tratamento dos casos em que recebemos os comandos GET ou PUT
        switch(args[0]){
            case "get":
                try {
                    transfer.getFile(InetAddress.getByName(address), remotePort, ficheiro);
                } catch (Exception ex) {
                    System.out.println("Erro ao receber o ficheiro: " + ex);
                    System.exit(1);
                }

                while(!transfer.complete()){
                    try{
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        System.out.println("Erro no wait");
                    }
                }
                System.exit(0);
                break;
            case "put":
                try {
                    transfer.sendFile(InetAddress.getByName(address), remotePort, ficheiro);
                    System.exit(0);
                } catch (Exception ex) {
                    System.out.println("Erro ao enviar o ficheiro: " + ex);
                    System.exit(1);
                }
                break;
        } 
    }
}
