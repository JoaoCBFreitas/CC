
import java.io.Serializable;


public class PDU implements Serializable{
    //Declaração dos tipos de PDU
    public static final int TYPE_ACK = 1;
    public static final int TYPE_PUTFILE = 2;
    public static final int TYPE_DATA = 3;
    public static final int TYPE_CHECKSUM = 4;
    public static final int TYPE_GETFILE = 5;
    
    //Variável correspondente ao tipo de PDU
    private int type;
    //Variável correspondente ao tamanho do PDU
    private int length;
    //Variável correspondente ao offset do PDU
    private int offset;
    //Variável correspondente aos dados enviados no PDU
    private byte[] data;
    
   
    // Construtor com parâmetros do PDU
    public PDU(int type, int length, int offset, byte[] data){
        this.type = type;
        this.length = length;
        this.offset = offset;
        this.data = data;
    }
    
    //Método que devolve o tipo do PDU
    public int getType() { 
        return this.type; 
    }
    
    //Método que devolve o tamanho do PDU
    public int getLength() {
        return this.length;
    }
    
    //Método que devolve o offset do PDU
    public int getOffset() {
        return this.offset;
    }
    
    //Método que devolve os dados do PDU
    public byte[] getData() {
        return this.data;
    }
    
    //Método que deovlve uma string com os valores do PDU
    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder("\n----------------------------\nConteudo do pacote\n");
        str.append("\nTipo : ").append(type);
        str.append("\nlength : ").append(length);
        str.append("\noffset : ").append(offset);
        str.append("\ndata : ").append(data);
        return str.toString();
    }
}