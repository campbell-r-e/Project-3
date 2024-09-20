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
            Scanner keyboard = new Scanner(System.in);
            command = keyboard.nextLine().toUpperCase();

            switch (command) {
                case "D"://delete
                    String fileName = keyboard.nextLine();
                    ByteBuffer request = ByteBuffer.wrap((command + fileName).getBytes());
                    SocketChannel channe = SocketChannel.open();
                    channe.connect(new InetSocketAddress(args[0], serverPort));
                    channe.write(request);
                    channe.shutdownOutput();
                     // todo: receive server code and tell the user.

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
                    System.out.println("Invalid input");
            }// end of switch statement

        }
        while (!command.equals("Q"));{


        }


    }
}
