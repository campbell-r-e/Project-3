package tcpFileService;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

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
                byte[]r= new byte[request.remaining()];
                request.get(r);
                String ogFilename = new String(r);
                String newFileName=new String(r);
                File rfile = new File("ServerFiles/"+ogFilename);
                File rfilenew = new File("ServerFiles/"+ogFilename);
                boolean rsucess = rfile.renameTo(rfilenew); 
                
               
                if(rfile.exists()){
                   


                }
                String rreplymessage;
                if(rsucess){
                    rreplymessage = "S";

                }
                else{
                    rreplymessage = "F";

                }
                ByteBuffer rreply = ByteBuffer.wrap(rreplymessage.getBytes());
                serveChannel.write(rreply);
                serveChannel.close();

                 


                // Send the rename request
               // SocketChannel renameChannel = SocketChannel.open();
                //renameChannel.connect(new InetSocketAddress(args[0], serverPort));
                //ByteBuffer renameRequest = ByteBuffer.wrap(("R" + originalFileName + "|" + newFileName).getBytes());
                //renameChannel.write(renameRequest);
                // renameChannel.shutdownOutput();




                
              
              

              

                
                
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
