package tcpFileService;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.*;

public class server {
    public static void main(String[] args) throws  Exception{
        ServerSocketChannel Listenchannel= ServerSocketChannel.open();
        Listenchannel.bind(new InetSocketAddress(3000));
        while(true) {
            // to keep server
            SocketChannel serveChannel = Listenchannel.accept();
            ByteBuffer request = ByteBuffer.allocate(1024);
            //int numBytes = serveChannel.read(request);
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
                File server_folder = new File("ServerFiles");   // came from stack overflow
                ArrayList<File> listOfServerFiles = new ArrayList<>(Arrays.asList(server_folder.listFiles()));
                 
                String s = listOfServerFiles.toString();
                String server_reseponceL;


                if(s!=null){
                    server_reseponceL = "S";
                    ByteBuffer Lreply = ByteBuffer.wrap(s.getBytes());
                    serveChannel.write(Lreply);
                    //serveChannel.close();


                    ByteBuffer Lreplyonce = ByteBuffer.wrap(server_reseponceL.getBytes());
                    serveChannel.write(Lreplyonce);
                    serveChannel.close();
    
                }else{
                 server_reseponceL = "F";
                 ByteBuffer Lrespionce = ByteBuffer.wrap(server_reseponceL.getBytes());
                 serveChannel.write(Lrespionce);
                 serveChannel.close();
 
                }
                 
 


                   



                    break;
                case "R":
                byte[]r= new byte[request.remaining()];
                request.get(r);
                String ogFilename = new String(r);
                String newFileName=new String(r);
                File rfile = new File("ServerFiles/"+ogFilename);
                File rfilenew = new File("ServerFiles/"+newFileName);
                
                String rreplymessage;
               
                if(rfile.exists()){
                    boolean rsucess = rfile.renameTo(rfilenew); 
                    if(rsucess){
                        rreplymessage = "S";
    
                    }
                    else{
                        rreplymessage = "F";
    
                    }}
                else {
                    rreplymessage = "F"; 
                }
                ByteBuffer rreply = ByteBuffer.wrap(rreplymessage.getBytes());
                serveChannel.write(rreply);
                serveChannel.close();
                break;
                
                
                
                
                case "U": // upload

                byte[]u= new byte[request.remaining()];
                request.get(u);
              
                String uFilename = new String(u);
                BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream("ServerFiles/" + new String(uFilename)));

                int bytesRead = 0;
                ByteBuffer fileDataBuffer = ByteBuffer.allocate(1024); 
              
                while((bytesRead = serveChannel.read(fileDataBuffer))>0){
                    fileDataBuffer.flip();
                    byte[] datapacket = new byte[bytesRead];
                    fileDataBuffer.get(datapacket);
                    fos.write(datapacket);
            
                    fileDataBuffer.clear();
    
    
                }
                fos.close();
                

            File w = new File("ServerFiles/" + new String(uFilename)); 
  
             String ureplys;
               
                if(w.exists()){
                    ureplys = "S";
                }
                else{
                    ureplys = "F";
                }

                ByteBuffer ureply = ByteBuffer.wrap(ureplys.getBytes());
                serveChannel.write(ureply);
                serveChannel.close();
                
               
                    break;
                case "G":  // download
                String greplys;

                byte[]G= new byte[request.remaining()];
                request.get(G);
                String GFilename = new String(G);
                File gfile = new File("ServerFiles/" + GFilename);

                if (!gfile.exists()) {
                    greplys = "F";
                    System.out.println("File does not exist.");
                    ByteBuffer greply = ByteBuffer.wrap(greplys.getBytes());
                    serveChannel.write(greply);
                    serveChannel.close();

                    break;
                }

               
                
                FileInputStream gfis = new FileInputStream(GFilename);
               

              
                byte[] gbuffer = new byte[1024];
                int dbytesRead;
                while ((dbytesRead = gfis.read(gbuffer)) > 0) {
                    ByteBuffer fileData = ByteBuffer.wrap(gbuffer, 0, dbytesRead);
                    serveChannel.write(fileData);
                    //fileData.clear();
                }
                gfis.close();
                

               

                
               
                
                greplys = "S";
                
                

                ByteBuffer greply = ByteBuffer.wrap(greplys.getBytes());
                serveChannel.write(greply);
                serveChannel.shutdownOutput();
                serveChannel.close();



                
                  
              
           
                break;
                default:
                    System.out.println("Invalid Command");
            }

        }

    }
}
