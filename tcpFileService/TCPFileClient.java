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

                            // Integrating the buffer-safe read
                            while (listChannel.read(listReply) > 0) {
                                listReply.flip();  // Flip buffer to prepare for reading
                                byte[] listResponse = new byte[listReply.remaining()];
                                listReply.get(listResponse);  // Read content from the buffer
                                responseBuilder.append(new String(listResponse));  // Append to the response string
                                listReply.clear();  // Clear buffer for the next read cycle
                            }


                            // Display the file list (it should be in a readable format, like a list of file names)
                            System.out.println(responseBuilder.toString().trim());

                        } else if (listCode.equals("F")) {
                            System.out.println("Failed to retrieve file list.");
                        } else {
                            System.out.println("Invalid response received from the server.");
                        }

                    } catch (SocketException e) {
                        e.printStackTrace();  // Handle the exception for any IO issues
                    }
                    break;

                case "R": // rename
                    System.out.println("Enter original file name: ");
                    String originalFileName = keyboard.nextLine();
                    System.out.println("Enter new file name: ");
                    String newFileName = keyboard.nextLine();

                    try (SocketChannel renameChannel = SocketChannel.open()) {
                        // Connect to the server
                        renameChannel.connect(new InetSocketAddress(args[0], serverPort));

                        // Prepare and send the rename request
                        String renameRequestString = "R" + originalFileName + "|" + newFileName;
                        ByteBuffer renameRequest = ByteBuffer.wrap(renameRequestString.getBytes());
                        renameChannel.write(renameRequest);
                        renameChannel.shutdownOutput();  // Indicate that no more data will be sent

                        // Allocate buffer for the server response
                        ByteBuffer renameReply = ByteBuffer.allocate(1);

                        // Read the server's response
                        int bytesRead = renameChannel.read(renameReply);
                        if (bytesRead > 0) {
                            renameReply.flip();  // Flip buffer for reading

                            // Extract the response
                            byte[] renameResponse = new byte[renameReply.remaining()];
                            renameReply.get(renameResponse);

                            // Check the response code
                            String renameCode = new String(renameResponse);
                            if (renameCode.equals("S")) {
                                System.out.println("File renamed successfully.");
                            } else if (renameCode.equals("F")) {
                                System.out.println("Failed to rename file.");
                            } else {
                                System.out.println("Invalid server code received.");
                            }
                        } else {
                            System.out.println("No response received from the server.");
                        }
                    } catch (SocketException e) {
                        e.printStackTrace();
                    }
                    break;


                case "U": // upload
                    System.out.println("Enter File Name: ");
                    String fileToUpload = keyboard.nextLine();
                    File file = new File("ClientFiles/" + fileToUpload);  // Prepend directory "ClientFiles/"

                    // Check if the file exists
                    if (!file.exists()) {
                        System.out.println("File does not exist.");
                        break;
                    }

                    try (SocketChannel uploadChannel = SocketChannel.open();
                         FileInputStream fis = new FileInputStream(file)) {

                        // Connect to the server
                        uploadChannel.connect(new InetSocketAddress(args[0], serverPort));

                        // Send the upload command and file name as header
                        String uploadHeader = "U" + fileToUpload;
                        ByteBuffer header = ByteBuffer.wrap(uploadHeader.getBytes());
                        uploadChannel.write(header);

                        // Read and send file bytes
                        byte[] buffer = new byte[1024];  // 1KB buffer for file transfer
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) > 0) {
                            ByteBuffer fileData = ByteBuffer.wrap(buffer, 0, bytesRead);
                            while (fileData.hasRemaining()) {
                                uploadChannel.write(fileData);  // Ensure the full buffer is written
                            }
                        }

                        // Signal end of file upload
                        uploadChannel.shutdownOutput();

                        // Wait for server response
                        ByteBuffer uploadReply = ByteBuffer.allocate(1);
                        int bytesReceived = uploadChannel.read(uploadReply);

                        if (bytesReceived > 0) {
                            uploadReply.flip();
                            byte[] uploadResponse = new byte[uploadReply.remaining()];
                            uploadReply.get(uploadResponse);
                            String uploadCode = new String(uploadResponse);

                            // Check the response code from the server
                            if (uploadCode.equals("S")) {
                                System.out.println("File uploaded successfully.");
                            } else if (uploadCode.equals("F")) {
                                System.out.println("File upload failed.");
                            } else {
                                System.out.println("Invalid server code received.");
                            }
                        } else {
                            System.out.println("No response from the server.");
                        }

                    } catch (SocketException e) {
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
                        int bytesReceived = downloadChannel.read(downloadReply);
                        if (bytesReceived > 0) {
                            downloadReply.flip();
                            byte[] downloadResponse = new byte[downloadReply.remaining()];
                            downloadReply.get(downloadResponse);
                            String downloadCode = new String(downloadResponse);

                            if (downloadCode.equals("S")) {
                                System.out.println("Receiving file...");
                                try (FileOutputStream fos = new FileOutputStream("downloaded_" + fileToDownload)) {
                                    ByteBuffer fileBuffer = ByteBuffer.allocate(1024);  // Buffer for file data (1KB chunks)

                                    // Read and write the file data
                                    while (downloadChannel.read(fileBuffer) > 0) {
                                        fileBuffer.flip();  // Prepare buffer for reading
                                        fos.write(fileBuffer.array(), 0, fileBuffer.remaining());
                                        fileBuffer.clear();  // Clear buffer for the next read
                                    }

                                    System.out.println("File download successful.");
                                }
                            } else if (downloadCode.equals("F")) {
                                System.out.println("File not found on server.");
                            } else {
                                System.out.println("Invalid server code received.");
                            }
                        } else {
                            System.out.println("No response received from the server.");
                        }
                    } catch (SocketException e) {
                        System.err.println("Error during file download: " + e.getMessage());
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
