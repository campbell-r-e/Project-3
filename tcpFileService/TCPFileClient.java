package tcpFileService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
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
                        if (channel.read(reply) > 0) {
                            reply.flip();
                            byte[] a = new byte[reply.remaining()];
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
                    } catch (IOException e) {
                        System.err.println("Error during delete operation: " + e.getMessage());
                    }
                    break;

                case "L": // list
                    try (SocketChannel listChannel = SocketChannel.open()) {
                        listChannel.connect(new InetSocketAddress(args[0], serverPort));
                        ByteBuffer listRequest = ByteBuffer.wrap(command.getBytes());
                        listChannel.write(listRequest);
                        listChannel.shutdownOutput();

                        ByteBuffer statusBuffer = ByteBuffer.allocate(1);
                        if (listChannel.read(statusBuffer) > 0) {
                            statusBuffer.flip();
                            byte[] statusArray = new byte[statusBuffer.remaining()];
                            statusBuffer.get(statusArray);
                            String listCode = new String(statusArray);

                            if (listCode.equals("S")) {
                                System.out.println("Files on the server:");
                                ByteBuffer listReply = ByteBuffer.allocate(1024);
                                StringBuilder responseBuilder = new StringBuilder();
                                while (listChannel.read(listReply) > 0) {
                                    listReply.flip();
                                    byte[] listResponse = new byte[listReply.remaining()];
                                    listReply.get(listResponse);
                                    responseBuilder.append(new String(listResponse));
                                    listReply.clear();
                                }
                                System.out.println(responseBuilder.toString().trim());
                            } else if (listCode.equals("F")) {
                                System.out.println("Failed to retrieve file list.");
                            } else {
                                System.out.println("Invalid response received from the server.");
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error during list operation: " + e.getMessage());
                    }
                    break;

                case "R": // rename
                    System.out.println("Enter original file name: ");
                    String originalFileName = keyboard.nextLine();
                    System.out.println("Enter new file name: ");
                    String newFileName = keyboard.nextLine();

                    try (SocketChannel renameChannel = SocketChannel.open()) {
                        renameChannel.connect(new InetSocketAddress(args[0], serverPort));
                        String renameRequestString = "R" + originalFileName + "|" + newFileName;
                        ByteBuffer renameRequest = ByteBuffer.wrap(renameRequestString.getBytes());
                        renameChannel.write(renameRequest);
                        renameChannel.shutdownOutput();

                        ByteBuffer renameReply = ByteBuffer.allocate(1);
                        if (renameChannel.read(renameReply) > 0) {
                            renameReply.flip();
                            byte[] renameResponse = new byte[renameReply.remaining()];
                            renameReply.get(renameResponse);
                            String renameCode = new String(renameResponse);
                            if (renameCode.equals("S")) {
                                System.out.println("File renamed successfully.");
                            } else if (renameCode.equals("F")) {
                                System.out.println("Failed to rename file.");
                            } else {
                                System.out.println("Invalid server code received.");
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error during rename operation: " + e.getMessage());
                    }
                    break;

                case "U": // upload
                    System.out.println("Enter File Name: ");
                    String fileToUpload = keyboard.nextLine();
                    File file = new File("ClientFiles/" + fileToUpload);  // Prepend directory "ClientFiles/"

                    if (!file.exists()) {
                        System.out.println("File does not exist.");
                        break;
                    }

                    try (SocketChannel uploadChannel = SocketChannel.open();
                         FileInputStream fis = new FileInputStream(file)) {

                        uploadChannel.connect(new InetSocketAddress(args[0], serverPort));

                        String uploadHeader = "U" + fileToUpload;
                        ByteBuffer header = ByteBuffer.wrap(uploadHeader.getBytes());
                        uploadChannel.write(header);

                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) > 0) {
                            ByteBuffer fileData = ByteBuffer.wrap(buffer, 0, bytesRead);
                            while (fileData.hasRemaining()) {
                                uploadChannel.write(fileData);
                            }
                        }

                        uploadChannel.shutdownOutput();

                        ByteBuffer uploadReply = ByteBuffer.allocate(1);
                        if (uploadChannel.read(uploadReply) > 0) {
                            uploadReply.flip();
                            byte[] uploadResponse = new byte[uploadReply.remaining()];
                            uploadReply.get(uploadResponse);
                            String uploadCode = new String(uploadResponse);

                            if (uploadCode.equals("S")) {
                                System.out.println("File uploaded successfully.");
                            } else if (uploadCode.equals("F")) {
                                System.out.println("File upload failed.");
                            } else {
                                System.out.println("Invalid server code received.");
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error during file upload: " + e.getMessage());
                    }
                    break;

                case "G": // download
                    System.out.println("Enter the file name to download: ");
                    String fileToDownload = keyboard.nextLine();

                    try (SocketChannel downloadChannel = SocketChannel.open()) {
                        // Connect to the server
                        downloadChannel.connect(new InetSocketAddress(args[0], serverPort));

                        // Send the download request (command "G" + file name)
                        ByteBuffer downloadRequest = ByteBuffer.wrap(("G" + fileToDownload).getBytes());
                        downloadChannel.write(downloadRequest);
                        downloadChannel.shutdownOutput();  // Signal end of the request

                        // Allocate buffer to receive the server response ("S" for success, "F" for failure)
                        ByteBuffer downloadReply = ByteBuffer.allocate(1);
                        if (downloadChannel.read(downloadReply) > 0) {
                            downloadReply.flip();
                            byte[] downloadResponse = new byte[downloadReply.remaining()];
                            downloadReply.get(downloadResponse);
                            String downloadCode = new String(downloadResponse);

                            if (downloadCode.equals("S")) {
                                System.out.println("Receiving file...");
                                try (FileOutputStream fos = new FileOutputStream("ClientFiles/"+"downloaded_" + fileToDownload)) {
                                    ByteBuffer fileBuffer = ByteBuffer.allocate(1024);

                                    // Read and write the file data in chunks of 1KB
                                    int bytesRead;
                                    while ((bytesRead = downloadChannel.read(fileBuffer)) > 0) {
                                        fileBuffer.flip();  // Prepare buffer for reading
                                        fos.write(fileBuffer.array(), 0, bytesRead);  // Only write the actual bytes read
                                        fileBuffer.clear();  // Clear buffer for the next read
                                    }

                                    System.out.println("File download successful.");
                                } catch (IOException e) {
                                    System.err.println("Error writing the file: " + e.getMessage());
                                }
                            } else if (downloadCode.equals("F")) {
                                System.out.println("File not found on server.");
                            } else {
                                System.out.println("Invalid server code received.");
                            }
                        } else {
                            System.out.println("No response received from the server.");
                        }
                    } catch (IOException e) {
                        System.err.println("Error during file download: " + e.getMessage());
                    }
                    break;


                default:
                    if (!command.equals("Q")) {
                        System.out.println("Invalid input");
                    }
            }

        } while (!command.equals("Q"));
    }
}
