package tcpFileService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
                        System.out.println("Failed to delete file");

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
                    System.out.println("Enter File Name: ");
                    String fileToUpload = keyboard.nextLine();
                    File file = new File(fileToUpload);

                    // Check if file exists
                    if (!file.exists()) {
                        System.out.println("File does not exist.");
                        break;
                    }

                    // Prepare the command and file size header
                    SocketChannel uploadChannel = SocketChannel.open();
                    uploadChannel.connect(new InetSocketAddress(args[0], serverPort));
                    FileInputStream fis = new FileInputStream(file);
                    ByteBuffer header = ByteBuffer.wrap(("U" + fileToUpload).getBytes());
                    uploadChannel.write(header);

                    // Read and send file bytes
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) > 0) {
                        ByteBuffer fileData = ByteBuffer.wrap(buffer, 0, bytesRead);
                        uploadChannel.write(fileData);
                    }
                    fis.close();
                    uploadChannel.shutdownOutput();

                    // Wait for server response
                    ByteBuffer uploadReply = ByteBuffer.allocate(1);
                    uploadChannel.read(uploadReply);
                    uploadChannel.close();
                    uploadReply.flip();
                    byte[] uploadResponse = new byte[1];
                    uploadReply.get(uploadResponse);
                    String uploadCode = new String(uploadResponse);

                    if (uploadCode.equals("S")) {
                        System.out.println("File uploaded successfully");
                    } else if (uploadCode.equals("F")) {
                        System.out.println("File upload failed");
                    } else {
                        System.out.println("Invalid server code received");
                    }
                    break;
                case "G": // Download
                    System.out.println("Enter the file name to download: ");
                    String fileToDownload = keyboard.nextLine();
                    SocketChannel downloadChannel = SocketChannel.open();
                    downloadChannel.connect(new InetSocketAddress(args[0], serverPort));

                    // Send the command and file name to the server
                    ByteBuffer downloadRequest = ByteBuffer.wrap(("G" + fileToDownload).getBytes());
                    downloadChannel.write(downloadRequest);
                    downloadChannel.shutdownOutput();

                    // Prepare to receive the file or error response
                    ByteBuffer downloadReply = ByteBuffer.allocate(1);
                    downloadChannel.read(downloadReply);
                    downloadReply.flip();
                    byte[] downloadResponse = new byte[1];
                    downloadReply.get(downloadResponse);
                    String downloadCode = new String(downloadResponse);

                    if (downloadCode.equals("S")) {
                        System.out.println("Receiving file...");
                        FileOutputStream fos = new FileOutputStream("downloaded_" + fileToDownload);

                        ByteBuffer fileBuffer = ByteBuffer.allocate(1024);
                        while (downloadChannel.read(fileBuffer) > 0) {
                            fileBuffer.flip();
                            fos.write(fileBuffer.array(), 0, fileBuffer.remaining());
                            fileBuffer.clear();
                        }
                        fos.close();
                        System.out.println("File download successful");
                    } else if (downloadCode.equals("F")) {
                        System.out.println("File not found on server.");
                    } else {
                        System.out.println("Invalid server code received");
                    }

                    downloadChannel.close();
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
