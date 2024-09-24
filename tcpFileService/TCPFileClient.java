package tcpFileService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class TCPFileClient {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: TCPFileClient <server-address> <server-port>");
            return;
        }
        int serverPort = Integer.parseInt(args[1]);
        String command;
        do {
            System.out.println("Enter command ");
            Scanner keyboard = new Scanner(System.in);
            command = keyboard.nextLine().toUpperCase();

            switch (command) {
                case "D": // delete
                    System.out.println("Enter file name");
                    String fileName = keyboard.nextLine();
                    try (SocketChannel channel = SocketChannel.open()) {
                        ByteBuffer request = ByteBuffer.wrap((command + fileName).getBytes());
                        channel.connect(new InetSocketAddress(args[0], serverPort));
                        channel.write(request);
                        channel.shutdownOutput();
                        ByteBuffer reply = ByteBuffer.allocate(1);
                        channel.read(reply);
                        reply.flip();
                        byte[] a = new byte[1];
                        reply.get(a);
                        String code = new String(a);
                        if (code.equals("S")) {
                            System.out.println("File successfully Deleted");
                        } else if (code.equals("F")) {
                            System.out.println("Failed to delete file");
                        } else {
                            System.out.println("Invalid server code received");
                        }
                    }
                    break;

                case "L": // list
                    try (SocketChannel listChannel = SocketChannel.open()) {
                        listChannel.connect(new InetSocketAddress(args[0], serverPort));

                        // Send the list request command
                        ByteBuffer listRequest = ByteBuffer.wrap(command.getBytes());
                        listChannel.write(listRequest);
                        listChannel.shutdownOutput();

                        // Buffer to receive the server's success or failure response
                        ByteBuffer statusBuffer = ByteBuffer.allocate(1);  // Assuming server sends "S" or "F"
                        listChannel.read(statusBuffer);
                        statusBuffer.flip();

                        // Read the status response
                        byte[] statusArray = new byte[statusBuffer.remaining()];
                        statusBuffer.get(statusArray);
                        String listCode = new String(statusArray);

                        // Check if the response is successful ("S") or failed ("F")
                        if (listCode.equals("S")) {
                            System.out.println("Files on the server:");

                            // Now read the actual file list sent by the server
                            ByteBuffer listReply = ByteBuffer.allocate(1024);  // Buffer for receiving the file list
                            StringBuilder responseBuilder = new StringBuilder();
                            int bytesReadList;

                            while ((bytesReadList = listChannel.read(listReply)) > 0) {
                                listReply.flip();
                                byte[] listResponse = new byte[listReply.remaining()];  // Use the remaining data in the buffer
                                listReply.get(listResponse);
                                responseBuilder.append(new String(listResponse));
                                listReply.clear();
                            }

                            // Display the file list (it should be in a readable format, like a list of file names)
                            System.out.println(responseBuilder.toString().trim());

                        } else if (listCode.equals("F")) {
                            System.out.println("Failed to retrieve file list.");
                        } else {
                            System.out.println("Invalid response received from the server.");
                        }

                    } catch (IOException e) {
                        e.printStackTrace();  // Handle the exception for any IO issues
                    }
                    break;

                case "R": // rename
                    System.out.println("Enter original file name: ");
                    String originalFileName = keyboard.nextLine();
                    System.out.println("Enter new file name: ");
                    String newFileName = keyboard.nextLine();
                    try (SocketChannel renameChannel = SocketChannel.open()) {
                        // Send the rename request
                        renameChannel.connect(new InetSocketAddress(args[0], serverPort));
                        ByteBuffer renameRequest = ByteBuffer.wrap(("R" + originalFileName + "|" + newFileName).getBytes());
                        renameChannel.write(renameRequest);
                        renameChannel.shutdownOutput();

                        // Receive server response
                        ByteBuffer renameReply = ByteBuffer.allocate(1);
                        renameChannel.read(renameReply);
                        renameReply.flip();
                        byte[] renameResponse = new byte[1];
                        renameReply.get(renameResponse);
                        String renameCode = new String(renameResponse);
                        if (renameCode.equals("S")) {
                            System.out.println("File renamed successfully");
                        } else if (renameCode.equals("F")) {
                            System.out.println("Failed to rename file");
                        } else {
                            System.out.println("Invalid server code received");
                        }
                    }
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

                    try (SocketChannel uploadChannel = SocketChannel.open();
                         FileInputStream fis = new FileInputStream(file)) {
                        uploadChannel.connect(new InetSocketAddress(args[0], serverPort));
                        ByteBuffer header = ByteBuffer.wrap(("U" + fileToUpload).getBytes());
                        uploadChannel.write(header);

                        // Read and send file bytes
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) > 0) {
                            ByteBuffer fileData = ByteBuffer.wrap(buffer, 0, bytesRead);
                            uploadChannel.write(fileData);
                        }
                        uploadChannel.shutdownOutput();

                        // Wait for server response
                        ByteBuffer uploadReply = ByteBuffer.allocate(1);
                        uploadChannel.read(uploadReply);
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
                    }
                    break;

                case "G": // download
                    System.out.println("Enter the file name to download: ");
                    String fileToDownload = keyboard.nextLine();
                    try (SocketChannel downloadChannel = SocketChannel.open()) {
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
                            try (FileOutputStream fos = new FileOutputStream("downloaded_" + fileToDownload)) {
                                ByteBuffer fileBuffer = ByteBuffer.allocate(1024);
                                while (downloadChannel.read(fileBuffer) > 0) {
                                    fileBuffer.flip();
                                    fos.write(fileBuffer.array(), 0, fileBuffer.remaining());
                                    fileBuffer.clear();
                                }
                            }
                            System.out.println("File download successful");
                        } else if (downloadCode.equals("F")) {
                            System.out.println("File not found on server.");
                        } else {
                            System.out.println("Invalid server code received");
                        }
                    }
                    break;

                default:
                    if (!command.equals("Q")) {
                        System.out.println("Invalid input");
                    }
            } // end of switch statement

        } // end of do-while loop
        while (!command.equals("Q"));

    } // end of Main Method
} // end of class
