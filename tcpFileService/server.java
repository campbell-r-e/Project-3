package tcpFileService;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
//import java.util.Arrays;
import java.io.*;

public class server {
    public static void main(String[] args) throws Exception {
        ServerSocketChannel Listenchannel = ServerSocketChannel.open();
        Listenchannel.bind(new InetSocketAddress(3000));
        String command = null;

        while (true) {
            // Keep the server running
            try (SocketChannel serveChannel = Listenchannel.accept()) {
                ByteBuffer request = ByteBuffer.allocate(1024);
                int numBytes = serveChannel.read(request);

                // Check if the client sent any data
                if (numBytes == -1) {
                    System.out.println("Client closed connection.");
                    serveChannel.close();
                    continue;
                }

                if (numBytes > 0) {
                    // Prepare the buffer to be read
                    request.flip();

                    // Size of byte array should match the number of bytes for the command
                    byte[] a = new byte[1];
                    request.get(a);
                    command = new String(a);
                    System.out.println("\nReceived command: " + command);
                }

                if (command != null) {
                    switch (command) {
                        case "D":
                            byte[] d = new byte[request.remaining()];
                            request.get(d);
                            String Filename = new String(d).trim();  // Added trimming for safety
                            System.out.println("File to delete: " + Filename);

                            File file = new File("../ServerFiles/" + Filename);
                            Boolean success = false;
                            if (file.exists() && file.isFile()) {
                                success = file.delete();
                            }

                            String replymessage = success ? "S" : "F";
                            ByteBuffer reply = ByteBuffer.wrap(replymessage.getBytes());
                            serveChannel.write(reply);
                            serveChannel.shutdownOutput();
                            break;

                        case "L":
                            File serverFolder = new File("../ServerFiles");
                            if (!serverFolder.exists() || !serverFolder.isDirectory()) {
                                ByteBuffer failureReply = ByteBuffer.wrap("F".getBytes());
                                serveChannel.write(failureReply);
                                serveChannel.shutdownOutput();
                                break;
                            }

                            File[] files = serverFolder.listFiles();
                            if (files != null && files.length > 0) {
                                // Convert the file list to a string
                                ArrayList<String> listOfServerFiles = new ArrayList<>();
                                for (File f : files) {
                                    if (f.isFile()) {
                                        listOfServerFiles.add(f.getName());
                                    }
                                }
                                String fileListString = String.join(",", listOfServerFiles);

                                // Send success code "S" first
                                ByteBuffer successReply = ByteBuffer.wrap("S".getBytes());
                                serveChannel.write(successReply);

                                // Send the file list as a second response
                                ByteBuffer fileListReply = ByteBuffer.wrap(fileListString.getBytes());
                                serveChannel.write(fileListReply);
                            } else {
                                // If no files or folder doesn't exist, send failure code "F"
                                ByteBuffer failureReply = ByteBuffer.wrap("F".getBytes());
                                serveChannel.write(failureReply);
                            }
                            serveChannel.shutdownOutput();
                            break;

                        case "R": // Rename file
                            byte[] r = new byte[request.remaining()];
                            request.get(r);
                            String receivedString = new String(r).trim(); // Convert byte array to string and trim whitespace

                            // Split the received string by a comma to get the original and new filenames
                            String[] filenames = receivedString.split("|");

                            // Ensure the split resulted in exactly two filenames (original and new)
                            if (filenames.length != 2) {
                                System.err.println("Invalid input: Expected two filenames separated by a comma");
                                ByteBuffer errorReply = ByteBuffer.wrap("F".getBytes()); // Send failure code
                                serveChannel.write(errorReply);
                                serveChannel.shutdownOutput();
                                break;
                            }

                            // Extract and trim the filenames
                            String ogFilename = filenames[0].trim();
                            String newFileName = filenames[1].trim();

                            // Define file paths based on the ServerFiles directory
                            File rfile = new File("../ServerFiles/" + ogFilename);
                            File rfilenew = new File("../ServerFiles/" + newFileName);

                            String rreplymessage;

                            // Check if the original file exists and attempt to rename it
                            if (rfile.exists() && rfile.isFile()) {
                                boolean rsuccess = rfile.renameTo(rfilenew);
                                rreplymessage = rsuccess ? "S" : "F"; // Rename success/failure
                            } else {
                                rreplymessage = "F"; // Original file does not exist
                            }

                            // Send the response back to the client
                            ByteBuffer rreply = ByteBuffer.wrap(rreplymessage.getBytes());
                            serveChannel.write(rreply);
                            serveChannel.shutdownOutput();
                            break;

                        case "U": // Upload file
                            byte[] u = new byte[request.remaining()];
                            request.get(u);
                            String uFilename = new String(u).trim();

                            // Ensure directories and create the file output stream
                            String fileSavePath = "ServerFiles/" + uFilename;
                            File directory = new File("../ServerFiles");
                            try (BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(fileSavePath))) {
                                // Buffer to read file data from the client
                                ByteBuffer fileDataBuffer = ByteBuffer.allocate(1024);
                                int bytesRead;

                                while ((bytesRead = serveChannel.read(fileDataBuffer)) > 0) {
                                    fileDataBuffer.flip(); // Prepare buffer for reading
                                    byte[] datapacket = new byte[fileDataBuffer.remaining()];
                                    fileDataBuffer.get(datapacket);
                                    fos.write(datapacket);
                                    fileDataBuffer.clear(); // Clear buffer for the next read
                                }

                                // Confirm file upload success
                                File uploadedFile = new File(fileSavePath);
                                String ureplyMessage = uploadedFile.exists() ? "S" : "F";
                                ByteBuffer ureply = ByteBuffer.wrap(ureplyMessage.getBytes());
                                serveChannel.write(ureply);

                            } catch (IOException e) {
                                System.err.println("File write error: " + e.getMessage());
                                ByteBuffer errorReply = ByteBuffer.wrap("F".getBytes());
                                serveChannel.write(errorReply);
                            }

                            serveChannel.shutdownOutput();
                            break;

                        case "G":  // Download file
                            byte[] G = new byte[request.remaining()];
                            request.get(G);
                            String GFilename = new String(G).trim();
                            File gfile = new File("../ServerFiles/" + GFilename);

                            if (!gfile.exists() || !gfile.isFile()) {
                                ByteBuffer greply = ByteBuffer.wrap("F".getBytes());
                                serveChannel.write(greply);
                                serveChannel.shutdownOutput();
                                break;
                            }

                            try (FileInputStream gfis = new FileInputStream(gfile)) {
                                byte[] gbuffer = new byte[1024];
                                int dbytesRead;
                                while ((dbytesRead = gfis.read(gbuffer)) > 0) {
                                    ByteBuffer fileData = ByteBuffer.wrap(gbuffer, 0, dbytesRead);
                                    serveChannel.write(fileData);
                                }

                                ByteBuffer greply = ByteBuffer.wrap("S".getBytes());
                                serveChannel.write(greply);
                                serveChannel.shutdownOutput();

                            } catch (IOException e) {
                                System.err.println("File read error: " + e.getMessage());
                                ByteBuffer errorReply = ByteBuffer.wrap("F".getBytes());
                                serveChannel.write(errorReply);
                                serveChannel.shutdownOutput();
                            }
                            break;

                        default:
                            System.out.println("Invalid Command");
                            serveChannel.shutdownOutput();
                            break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Error during communication: " + e.getMessage());
            }
        }
    }
}
