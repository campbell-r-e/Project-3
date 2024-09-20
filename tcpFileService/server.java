package tcpFileService;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class server {
    public static void main(String[] args) throws  Exception{
        ServerSocketChannel Listenchannel= ServerSocketChannel.open();
        Listenchannel.bind(new InetSocketAddress(3000));
        while(true) {
            // to keep server
            SocketChannel serveChannel = Listenchannel.accept();
            ByteBuffer request = ByteBuffer.allocate(1024);
            int numBytes = serveChannel.read(request);
            request.flip();

            // Size of byte array should match number bytes for command

            byte[] a = new byte[1];
            request.get(a);
            String command = new String(a);
            System.out.println("\n recieved command:"+command);

            switch (command){
                case "D":
                    byte[]d= new byte[request.remaining()];
                    request.get(d);
                    String Filename = new String(d);
                    System.out.println("File to delte"+Filename);
                    File file = new File("ServerFiles/"+Filename);
                    Boolean sucess = false;
                    if(file.exists()){
                        sucess = file.delete();


                    }
                    String replymessage;
                    if(sucess){
                        replymessage = "S";

                    }
                    else{
                        replymessage = "F";

                    }
                    ByteBuffer reply = ByteBuffer.wrap(replymessage.getBytes());
                    serveChannel.write(reply);
                    serveChannel.close();



                    break;
                case "L":
                    break;
                case "R":
                    break;
                case "U":
                    break;
                case "G":
                    break;
                default:
                    System.out.println("Invalid Command");
            }

        }

    }
}
