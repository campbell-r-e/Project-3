package tcpFileService;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class TCPFileClient {
    public static void main(String[] args) throws  Exception {
        if (args.length != 2) {
            System.out.println();
            return;
        }
        int serverPort = Integer.parseInt(args[1]);
        String command;
        do {
            System.out.println("Enter command ");
            Scanner keyboard = new Scanner(System.in);
            command = keyboard.nextLine().toUpperCase();

            switch (command) {
                case "D"://delete
                    System.out.println("Enter file name");
                    String fileName = keyboard.nextLine();
                    ByteBuffer request = ByteBuffer.wrap((command + fileName).getBytes());
                    SocketChannel channe = SocketChannel.open();
                    channe.connect(new InetSocketAddress(args[0], serverPort));
                    channe.write(request);
                    channe.shutdownOutput();
                     // todo: receive server code and tell the user.
                    ByteBuffer reply = ByteBuffer.allocate(1);
                    channe.read(reply);
                    channe.close();
                    reply.flip();
                    byte[]a = new byte[1];
                    reply.get(a);
                    String code = new String(a);
                    if(code.equals("S")){
                        System.out.println("File successfully Deleted");

                    } else if (code.equals("F")) {
                        System.out.println("Failde to delete file");

                    }
                    else{
                        System.out.println("Invalid server code received ");
                    }


                    break;
                case "L"://list
                    break;
                case "R": // rename
                    break;

                case "U": // upload
                    break;
                case "G": // Download
                    break;
                default:
                    if(!command.equals("Q")){
                        System.out.println("Invalid input");
                    }


            }// end of switch statement

        }
        while (!command.equals("Q"));{


        }


    }
}
